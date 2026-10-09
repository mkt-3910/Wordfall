// キャンバスの取得と基本設定
const canvas = document.getElementById('board');
const ctx = canvas.getContext('2d');

const COLS = 12;
const ROWS = 13;
const CELL = 32;
const DROP_INTERVAL = 900;

// 盤面・単語判定・得点・次のミノはサーバーが決める。画面側は操作と演出だけを担当する。
const END_TITLES = {
    GAME_OVER: 'ゲームオーバー',
    LANDING_LIMIT: '上限の着地数に到達しました',
    QUIT: 'プレイを終了しました'
};

// 盤面データ:各マスに文字(例:"A")か null が入る
let grid = emptyGrid();
let current = null;
let visualY = 0;

let score = 0;
let life = 3;
let wordCount = 0;
let combo = 0;
let gameOver = false;
let isProcessing = false;
let gameStarted = false;
let showCurrentPiece = true;
let paused = false;
let communicationFailed = false;

let gameId = null;
let landingIndex = 0;

class HttpError extends Error {
    constructor(status) {
        super('HTTP ' + status);
        this.status = status;
    }
}

async function requestJson(url, options = {}) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 8000);
    try {
        const response = await fetch(url, { ...options, signal: controller.signal });
        if (!response.ok) throw new HttpError(response.status);
        const text = await response.text();
        return text ? JSON.parse(text) : null;
    } finally {
        clearTimeout(timer);
    }
}

function postJson(url, value = {}) {
    return requestJson(url, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(value)
    });
}

function emptyGrid() {
    return Array.from({ length: ROWS }, () => Array(COLS).fill(null));
}

// サーバーの盤面("."が空きマス)を画面用の配列にする
function parseBoard(rows) {
    if (!Array.isArray(rows) || rows.length !== ROWS || rows.some(row => row.length !== COLS)) {
        throw new Error('盤面の応答が不正です');
    }
    return rows.map(row => [...row].map(ch => (ch === '.' ? null : ch)));
}

function toPiece(view) {
    return {
        size: view.size,
        x: view.x,
        y: view.y,
        rotation: 0,
        cells: view.cells.map(cell => [cell.x, cell.y, cell.letter])
    };
}

function collides(piece, offX = 0, offY = 0, cells = piece.cells) {
    for (const [cx, cy] of cells) {
        const gx = piece.x + cx + offX;
        const gy = piece.y + cy + offY;
        if (gx < 0 || gx >= COLS || gy >= ROWS) return true;
        if (gy >= 0 && grid[gy][gx]) return true;
    }
    return false;
}

function rotatePiece(piece) {
    const s = piece.size;
    const rotated = piece.cells.map(([x, y, letter]) => [s - 1 - y, x, letter]);
    return { ...piece, cells: rotated, rotation: (piece.rotation + 1) % 4 };
}

function applyGravity() {
    const moves = [];
    for (let c = 0; c < COLS; c++) {
        let writeRow = ROWS - 1;
        for (let r = ROWS - 1; r >= 0; r--) {
            if (grid[r][c] !== null) {
                if (writeRow !== r) {
                    moves.push({ col: c, fromRow: r, toRow: writeRow, ch: grid[r][c] });
                    grid[writeRow][c] = grid[r][c];
                    grid[r][c] = null;
                }
                writeRow--;
            }
        }
        for (let r = writeRow; r >= 0; r--) grid[r][c] = null;
    }
    return moves;
}

function simplifyDefinition(text) {
    if (!text) return text;
    const firstSentence = text.split('。')[0];
    if (firstSentence.length <= 30) return firstSentence + (text.includes('。') ? '。' : '');
    return firstSentence.slice(0, 30) + '...';
}

function wait(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

let gravityMoves = null;
let gravityStartTime = 0;
const GRAVITY_DURATION = 120;

async function animateGravity(moves) {
    if (moves.length === 0) return;
    gravityMoves = moves;
    gravityStartTime = performance.now();
    await wait(GRAVITY_DURATION);
    gravityMoves = null;
}

let toastTimer = null;
function showToastMessage(text) {
    const toast = document.getElementById('wordToast');
    toast.textContent = text;
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        toast.classList.remove('show');
    }, 900);
}

function triggerScreenEffect(className) {
    const boardWrap = document.querySelector('.board-wrap');
    boardWrap.classList.remove(className);
    void boardWrap.offsetWidth;
    boardWrap.classList.add(className);
    setTimeout(() => boardWrap.classList.remove(className), 400);
}

function showScorePopup(col, row, points) {
    const boardWrap = document.querySelector('.board-wrap');
    const popup = document.createElement('div');
    popup.className = 'score-popup';
    popup.textContent = `+${points}`;
    popup.style.left = `${col * CELL + CELL / 2}px`;
    popup.style.top = `${row * CELL}px`;
    boardWrap.appendChild(popup);
    setTimeout(() => popup.remove(), 800);
}

let clearingCells = null;
let clearStartTime = 0;
const CLEAR_DURATION = 220;

function updatePanels() {
    document.getElementById('score').textContent = score;
    document.getElementById('life').textContent = life;
    document.getElementById('wordCount').textContent = wordCount;
    document.getElementById('combo').textContent = combo;
}

async function lockPiece() {
    const request = { index: landingIndex, x: current.x, y: current.y, rotation: current.rotation };
    const before = grid.map(row => [...row]);
    for (const [cx, cy, letter] of current.cells) {
        const gy = current.y + cy;
        if (gy >= 0) grid[gy][current.x + cx] = letter;
    }
    showCurrentPiece = false;

    // 落下の演出とサーバーへの送信を同時に進める。失敗したら着地前の盤面に戻す。
    let result;
    try {
        [result] = await Promise.all([
            postJson(`/api/games/${gameId}/landings`, request),
            animateGravity(applyGravity())
        ]);
        if (!result || result.index !== request.index || !Array.isArray(result.words)) {
            throw new Error('着地の応答が不正です');
        }
    } catch (error) {
        grid = before;
        showCurrentPiece = true;
        gravityMoves = null;
        throw error;
    }
    landingIndex = request.index + 1;
    combo = result.combo;

    if (result.words.some(word => word.word.length >= 4)) triggerScreenEffect('flash');
    for (const word of result.words) {
        showToastMessage(`${word.word}完成! +${word.points}`);
        const [row, col] = word.cells[0];
        showScorePopup(col, row, word.points);
    }
    if (result.multiWordBonus > 0) {
        showToastMessage(`${result.words.length}単語同時！ ボーナス +${result.multiWordBonus}`);
        triggerScreenEffect('shake');
    }

    if (result.words.length > 0) {
        clearingCells = new Map();
        for (const word of result.words) {
            for (const [r, c] of word.cells) clearingCells.set(`${r},${c}`, grid[r][c]);
        }
        clearStartTime = performance.now();
        await wait(CLEAR_DURATION);
        for (const key of clearingCells.keys()) {
            const [r, c] = key.split(',').map(Number);
            grid[r][c] = null;
        }
        clearingCells = null;
        await animateGravity(applyGravity());

        const wordLog = document.getElementById('wordLog');
        for (const word of result.words) {
            const entry = document.createElement('p');
            entry.className = 'wordlog-entry';
            entry.textContent = `${word.word} (${word.partOfSpeech}) - ${simplifyDefinition(word.definition)}`;
            wordLog.prepend(entry);
        }
    }

    // 最後はサーバーの盤面に合わせる(ライフが減ったときの盤面リセットもここで反映される)
    grid = parseBoard(result.board);
    score = result.score;
    wordCount = result.wordCount;
    life = result.life;
    updatePanels();

    if (result.result) {
        showResult(result.result);
        return;
    }
    current = toPiece(result.nextPiece);
    visualY = current.y;
    showCurrentPiece = true;
}

function showResult(result) {
    gameOver = true;
    showCurrentPiece = false;
    document.getElementById('resultTitle').textContent = END_TITLES[result.reason] ?? 'ゲーム終了';
    document.getElementById('finalScoreText').textContent = 'スコア: ' + result.score + '　完成単語: ' + result.wordCount;
    document.getElementById('highScoreText').textContent = result.newHighScore
        ? 'ハイスコア更新！' : 'ハイスコア: ' + result.highScore;
    document.getElementById('gameOverMessage').classList.add('show');
}

// 通信エラーやサーバー側の一時的な失敗なら再試行できる。それ以外(状態の不一致など)は続行できない。
function isRetryable(error) {
    return !(error instanceof HttpError) || error.status >= 500 || error.status === 429;
}

function showConnectionError(error) {
    const retryable = isRetryable(error);
    document.getElementById('connectionText').textContent = retryable
        ? '通信に失敗しました。盤面を復元して停止しています。'
        : 'このプレイは続行できません。タイトルに戻ってください。';
    document.getElementById('retryConnectionBtn').hidden = !retryable;
    document.getElementById('errorBackToTitleBtn').hidden = retryable;
    document.getElementById('connectionMessage').classList.add('show');
}

async function loadTitleHighScore() {
    try {
        const high = await requestJson('/api/score/high');
        document.getElementById('titleHighScore').textContent = high.score;
    } catch (e) {
        document.getElementById('titleHighScore').textContent = '取得できませんでした';
    }
}

function moveHorizontal(dir) {
    if (!canControlPiece()) return;
    if (!collides(current, dir, 0)) {
        current.x += dir;
    }
}

function tryRotate() {
    if (!canControlPiece()) return;
    const rotated = rotatePiece(current);
    if (!collides(current, 0, 0, rotated.cells)) {
        current.cells = rotated.cells;
        current.rotation = rotated.rotation;
    }
}

function canControlPiece() {
    return gameStarted && !gameOver && !isProcessing && !paused && !communicationFailed;
}

async function softDrop() {
    if (!canControlPiece()) return;
    if (!collides(current, 0, 1)) {
        current.y++;
        return;
    }
    isProcessing = true;
    try {
        await lockPiece();
    } catch (error) {
        console.error('ゲーム処理に失敗しました', error);
        communicationFailed = true;
        showConnectionError(error);
    } finally {
        isProcessing = false;
    }
}

function easeOut(t) {
    return 1 - Math.pow(1 - t, 3);
}

function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = "rgba(237,232,222,0.08)";
    for (let c = 0; c <= COLS; c++) {
        ctx.beginPath();
        ctx.moveTo(c * CELL, 0);
        ctx.lineTo(c * CELL, ROWS * CELL);
        ctx.stroke();
    }
    for (let r = 0; r <= ROWS; r++) {
        ctx.beginPath();
        ctx.moveTo(0, r * CELL);
        ctx.lineTo(COLS * CELL, r * CELL);
        ctx.stroke();
    }

    const fallingKeys = gravityMoves ? new Set(gravityMoves.map(m => `${m.toRow},${m.col}`)) : null;

    for (let r = 0; r < ROWS; r++) {
        for (let c = 0; c < COLS; c++) {
            if (grid[r][c]) {
                const key = `${r},${c}`;
                if (clearingCells && clearingCells.has(key)) continue;
                if (fallingKeys && fallingKeys.has(key)) continue;
                drawLetter(c, r, grid[r][c]);
            }
        }
    }

    if (gravityMoves) {
        const rawProgress = Math.min(1, (performance.now() - gravityStartTime) / GRAVITY_DURATION);
        const progress = easeOut(rawProgress);
        for (const m of gravityMoves) {
            const rowNow = m.fromRow + (m.toRow - m.fromRow) * progress;
            drawLetter(m.col, rowNow, m.ch);
        }
    }

    if (clearingCells) {
        const progress = Math.min(1, (performance.now() - clearStartTime) / CLEAR_DURATION);
        for (const [key, letter] of clearingCells) {
            const [r, c] = key.split(',').map(Number);
            drawClearingLetter(c, r, letter, progress);
        }
    }

    if (showCurrentPiece && current) {
        for (const [cx, cy, letter] of current.cells) {
            const gy = visualY + cy;
            if (gy >= -1) drawLetter(current.x + cx, gy, letter);
        }
    }
}

function drawRoundedRect(x, y, width, height, radius) {
    ctx.beginPath();
    ctx.moveTo(x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.arcTo(x + width, y, x + width, y + radius, radius);
    ctx.lineTo(x + width, y + height - radius);
    ctx.arcTo(x + width, y + height, x + width - radius, y + height, radius);
    ctx.lineTo(x + radius, y + height);
    ctx.arcTo(x, y + height, x, y + height - radius, radius);
    ctx.lineTo(x, y + radius);
    ctx.arcTo(x, y, x + radius, y, radius);
    ctx.closePath();
}

function drawLetter(col, row, letter) {
    const x = col * CELL;
    const y = row * CELL;
    ctx.fillStyle = "#58cc02";
    drawRoundedRect(x + 2, y + 2, CELL - 4, CELL - 4, 8);
    ctx.fill();
    ctx.fillStyle = "#5b3a1e";
    ctx.font = "bold 18px sans-serif";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText(letter, x + CELL / 2, y + CELL / 2);
}

function drawClearingLetter(col, row, letter, progress) {
    const scale = 1 - progress * 0.6;
    const alpha = 1 - progress;
    const cx = col * CELL + CELL / 2;
    const cy = row * CELL + CELL / 2;

    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.translate(cx, cy);
    ctx.scale(scale, scale);

    ctx.fillStyle = "#58cc02";
    drawRoundedRect(-(CELL - 4) / 2, -(CELL - 4) / 2, CELL - 4, CELL - 4, 8);
    ctx.fill();
    ctx.fillStyle = "#5b3a1e";
    ctx.font = "bold 18px sans-serif";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText(letter, 0, 0);

    ctx.restore();
}

window.addEventListener('keydown', async (e) => {
    if (!gameStarted || gameOver) return;

    if (e.key === 'p' || e.key === 'P' || e.key === 'Escape') {
        e.preventDefault();
        togglePause();
        return;
    }

    if (paused) return;

    if (e.key === 'ArrowLeft') { e.preventDefault(); moveHorizontal(-1); }
    if (e.key === 'ArrowRight') { e.preventDefault(); moveHorizontal(1); }
    if (e.key === 'ArrowDown') { e.preventDefault(); await softDrop(); }
    if (e.key === 'ArrowUp') { e.preventDefault(); tryRotate(); }
});

setInterval(async () => {
    if (!gameStarted || gameOver || paused) return;
    await softDrop();
}, DROP_INTERVAL);

// 描画はゲーム開始後だけ動かす
function animationLoop() {
    const diff = current.y - visualY;
    visualY += diff * 0.25;
    if (Math.abs(diff) < 0.01) visualY = current.y;

    draw();
    requestAnimationFrame(animationLoop);
}

async function startGame() {
    const button = document.getElementById('startBtn');
    button.disabled = true;
    let game;
    try {
        game = await postJson('/api/games');
        if (!game?.gameId || !game.piece) throw new Error('ゲームを開始できません');
        grid = parseBoard(game.board);
    } catch (error) {
        console.error('ゲームを開始できませんでした', error);
        document.getElementById('titleStatus').textContent = error instanceof HttpError && error.status === 429
            ? '開始の回数が多すぎます。少し待ってから再度スタートしてください'
            : '開始できませんでした。通信を確認して再度スタートしてください';
        button.disabled = false;
        return;
    }
    gameId = game.gameId;
    landingIndex = 0;
    life = game.life;
    current = toPiece(game.piece);
    visualY = current.y;
    updatePanels();
    document.getElementById('gameLimitText').textContent = '1プレイは最大' + game.maxLandings.toLocaleString() + '回の着地まで';
    document.getElementById('titleScreen').hidden = true;
    document.getElementById('gameStage').hidden = false;
    gameStarted = true;
    requestAnimationFrame(animationLoop);
}

// 途中でやめたプレイを終了として記録する。失敗しても次のスタート時にサーバー側で終了扱いになる。
async function quitGame() {
    if (!gameStarted || gameOver) return;
    gameOver = true;
    try {
        await postJson(`/api/games/${gameId}/quit`);
    } catch (error) {
        console.error('プレイの終了を記録できませんでした', error);
    }
}

function togglePause() {
    if (!gameStarted || gameOver || isProcessing || communicationFailed) return;
    paused = !paused;
    document.getElementById('pauseMessage').classList.toggle('show', paused);
}

document.getElementById('startBtn').addEventListener('click', startGame);

document.getElementById('vocabularyBtn').addEventListener('click', () => {
    location.href = '/vocabulary';
});

document.getElementById('historyBtn').addEventListener('click', () => {
    location.href = '/history';
});

document.getElementById('achievementsBtn').addEventListener('click', () => {
    location.href = '/achievements';
});

document.getElementById('backToTitleBtn').addEventListener('click', () => location.reload());
document.getElementById('pauseBtn').addEventListener('click', () => togglePause());
document.getElementById('resumeBtn').addEventListener('click', () => togglePause());
document.getElementById('pauseBackToTitleBtn').addEventListener('click', async () => {
    await quitGame();
    location.reload();
});
document.getElementById('errorBackToTitleBtn').addEventListener('click', async () => {
    await quitGame();
    location.reload();
});
document.getElementById('retryConnectionBtn').addEventListener('click', async () => {
    if (isProcessing) return;
    communicationFailed = false;
    document.getElementById('connectionMessage').classList.remove('show');
    await softDrop();
});

// プレイ中にページを離れようとしたら確認し、離れた場合はプレイを終了として記録する。
window.addEventListener('beforeunload', event => {
    if (gameStarted && !gameOver) {
        event.preventDefault();
        event.returnValue = '';
    }
});
window.addEventListener('pagehide', () => {
    if (gameStarted && !gameOver) {
        fetch(`/api/games/${gameId}/quit`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}', keepalive: true
        }).catch(() => {});
    }
});

loadTitleHighScore();
