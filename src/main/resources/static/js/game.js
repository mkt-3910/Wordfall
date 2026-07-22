// キャンバスの取得と基本設定
const canvas = document.getElementById('board');
const ctx = canvas.getContext('2d');

const COLS = 12;
const ROWS = 13;
const CELL = 32;

// 盤面データ:各マスに文字(例:"A")か null が入る
let grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));

let score = 0;
let life = 3;
let wordCount = 0;
let gameOver = false;
let isProcessing = false;
let gameStarted = false;
let showCurrentPiece = true;
let paused = false;

let allFoundWords = [];

// ミノの形(積み木崩しと同じ座標データ)
const SHAPES = {
    I: { cells: [[0,1],[1,1],[2,1],[3,1]], size: 4 },
    O: { cells: [[1,0],[2,0],[1,1],[2,1]], size: 4 },
    T: { cells: [[1,0],[0,1],[1,1],[2,1]], size: 3 },
    S: { cells: [[1,0],[2,0],[0,1],[1,1]], size: 3 },
    Z: { cells: [[0,0],[1,0],[1,1],[2,1]], size: 3 },
    J: { cells: [[0,0],[0,1],[1,1],[2,1]], size: 3 },
    L: { cells: [[2,0],[0,1],[1,1],[2,1]], size: 3 },
};
const KEYS = Object.keys(SHAPES);

function randomKey() {
    return KEYS[Math.floor(Math.random() * KEYS.length)];
}

const LETTER_POOL = (
    "E".repeat(12) + "A".repeat(9) + "I".repeat(9) + "O".repeat(8) +
    "N".repeat(7) + "R".repeat(7) + "T".repeat(7) + "S".repeat(6) +
    "L".repeat(4) + "U".repeat(4) + "D".repeat(4) + "G".repeat(3) +
    "C".repeat(3) + "M".repeat(3) + "H".repeat(3) + "B".repeat(2) +
    "P".repeat(2) + "F".repeat(2) + "V".repeat(2) + "W".repeat(2) +
    "Y".repeat(2) + "K" + "J" + "X" + "Q" + "Z"
).split('');

function randomLetter() {
    return LETTER_POOL[Math.floor(Math.random() * LETTER_POOL.length)];
}

function makePiece(key) {
    const def = SHAPES[key];
    return {
        key,
        size: def.size,
        cells: def.cells.map(([x, y]) => [x, y, randomLetter()]),
        x: Math.floor((COLS - def.size) / 2),
        y: 0,
    };
}

let current = makePiece(randomKey());
let visualY = current.y;

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
    return { ...piece, cells: rotated };
}

function getAllLines() {
    const lines = [];

    for (let r = 0; r < ROWS; r++) {
        const line = [];
        for (let c = 0; c < COLS; c++) line.push({ r, c });
        lines.push(line);
    }
    for (let c = 0; c < COLS; c++) {
        const line = [];
        for (let r = 0; r < ROWS; r++) line.push({ r, c });
        lines.push(line);
    }
    for (let startCol = 0; startCol < COLS; startCol++) {
        const line = [];
        let r = 0, c = startCol;
        while (r < ROWS && c < COLS) { line.push({ r, c }); r++; c++; }
        lines.push(line);
    }
    for (let startRow = 1; startRow < ROWS; startRow++) {
        const line = [];
        let r = startRow, c = 0;
        while (r < ROWS && c < COLS) { line.push({ r, c }); r++; c++; }
        lines.push(line);
    }
    for (let startCol = 0; startCol < COLS; startCol++) {
        const line = [];
        let r = ROWS - 1, c = startCol;
        while (r >= 0 && c < COLS) { line.push({ r, c }); r--; c++; }
        lines.push(line);
    }
    for (let startRow = ROWS - 2; startRow >= 0; startRow--) {
        const line = [];
        let r = startRow, c = 0;
        while (r >= 0 && c < COLS) { line.push({ r, c }); r--; c++; }
        lines.push(line);
    }

    return lines;
}

function findRuns(line) {
    const runs = [];
    let cur = [];
    for (const { r, c } of line) {
        if (grid[r][c] !== null) {
            cur.push({ r, c, ch: grid[r][c] });
        } else {
            if (cur.length >= 3) runs.push(cur);
            cur = [];
        }
    }
    if (cur.length >= 3) runs.push(cur);
    return runs;
}

// 1本の連続した文字列(run)の中から、辞書に載っている単語の部分だけを探す
async function findWordMatchesInRun(run) {
    const s = run.map(cell => cell.ch).join('');
    const n = s.length;
    const candidates = [];

    // あり得る全ての部分文字列(3文字以上)を作り、1つずつサーバーに確認する
    for (let start = 0; start < n; start++) {
        for (let end = start + 3; end <= n; end++) {
            const sub = s.slice(start, end);
            const res = await fetch(`/api/check-word?word=${sub}`);
            const isWord = await res.json();
            if (isWord) {
                candidates.push({ start, end, len: end - start, word: sub });
            }
        }
    }

    // 長い一致を優先しつつ、マスが被らないように選ぶ
    candidates.sort((a, b) => b.len - a.len);
    const used = new Array(n).fill(false);
    const accepted = [];
    for (const c of candidates) {
        let overlap = false;
        for (let i = c.start; i < c.end; i++) if (used[i]) { overlap = true; break; }
        if (overlap) continue;
        for (let i = c.start; i < c.end; i++) used[i] = true;
        accepted.push(c);
    }

    return accepted.map(c => ({
        word: c.word,
        cells: run.slice(c.start, c.end),
    }));
}

// 盤面全体から、実際に完成している単語(部分文字列マッチ込み)を集める
async function collectCandidateRuns() {
    const lines = getAllLines();
    const allMatches = [];
    for (const line of lines) {
        for (const run of findRuns(line)) {
            const matches = await findWordMatchesInRun(run);
            allMatches.push(...matches);
        }
    }
    return allMatches;
}

// 各列ごとに、隙間を詰めて下に落とす。
// 「どのマスが、どこからどこへ動いたか」をmovesとして記録して返す(アニメーション用)
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

// 落下アニメーションの状態(drawが参照する)
let gravityMoves = null;
let gravityStartTime = 0;
const GRAVITY_DURATION = 200; // ミリ秒

// 重力による移動を、時間をかけて滑らかに見せる
async function animateGravity(moves) {
    if (moves.length === 0) return;
    gravityMoves = moves;
    gravityStartTime = performance.now();
    await wait(GRAVITY_DURATION);
    gravityMoves = null;
}

// 単語完成トースト
let toastTimer = null;
function showWordToast(word, points) {
    const toast = document.getElementById('wordToast');
    toast.textContent = `${word} 完成！ +${points}`;
    toast.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        toast.classList.remove('show');
    }, 900);
}

// 消えるマスの情報(縮小+フェードアニメーション用)
let clearingCells = null;
let clearStartTime = 0;
const CLEAR_DURATION = 350;

async function lockPiece() {
    for (const [cx, cy, letter] of current.cells) {
        const gx = current.x + cx;
        const gy = current.y + cy;
        if (gy >= 0) grid[gy][gx] = letter;
    }

    showCurrentPiece = false;

    // ミノを置いた直後、浮いている文字を滑らかに落とす
    const firstMoves = applyGravity();
    await animateGravity(firstMoves);

    const candidates = await collectCandidateRuns();
    const cellsToClear = new Set();
    const wordLogEntries = [];

    // ここから「候補を1つずつ確認する」ループ
    // このループの中では、確認と記録(貯めておくだけ)しかしない
    for (const candidate of candidates) {
        const meaningRes = await fetch(`/api/meaning?word=${candidate.word}`);

        let meaning = null;
        if (meaningRes.ok) {
            meaning = await meaningRes.json();
        }
        if (!meaning) {
            continue; // 意味が無かった候補は、ここで諦めて次の候補へ
        }

        const points = candidate.word.length * 10;
        score += points;
        wordCount++;
        allFoundWords.push(candidate.word);
        for (const cell of candidate.cells) {
            cellsToClear.add(`${cell.r},${cell.c}`);
        }
        showWordToast(candidate.word, points);

        const shortDefinition = simplifyDefinition(meaning.definition);
        wordLogEntries.push(`${meaning.word} (${meaning.partOfSpeech ?? '?'}) - ${shortDefinition}`);
    }
    // ここでループが終わる。全部の候補を確認し終えた状態

    // ここから先は、ループの外。1回だけ実行される
    if (cellsToClear.size > 0) {
        clearingCells = new Map();
        for (const key of cellsToClear) {
            const [r, c] = key.split(',').map(Number);
            clearingCells.set(key, grid[r][c]);
        }
        clearStartTime = performance.now();
        await wait(CLEAR_DURATION);
        clearingCells = null;

        for (const key of cellsToClear) {
            const [r, c] = key.split(',').map(Number);
            grid[r][c] = null;
        }

        const secondMoves = applyGravity();
        await animateGravity(secondMoves);

        const wordLog = document.getElementById('wordLog');
        for (const text of wordLogEntries) {
            const entry = document.createElement('p');
            entry.className = 'wordlog-entry';
            entry.textContent = text;
            wordLog.prepend(entry);
        }
    }

    document.getElementById('score').textContent = score;
    document.getElementById('wordCount').textContent = wordCount;

    current = makePiece(randomKey());
    visualY = current.y;
    showCurrentPiece = true;

    if (collides(current)) {
        life--;
        document.getElementById('life').textContent = life;

        if (life <= 0) {
            gameOver = true;
            document.getElementById('finalScoreText').textContent = `スコア: ${score}　完成単語: ${wordCount}`;
            document.getElementById('gameOverMessage').classList.add('show');
            await checkAndSaveHighScore();
            return;
        }

        grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));
    }
}
async function checkAndSaveHighScore() {
    try {
        const res = await fetch('/api/score/high');
        const high = await res.json();

        if (score > high.score) {
            await fetch('/api/score', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    score: score,
                    wordCount: wordCount,
                    words: allFoundWords.join(',')
                })
            });
            document.getElementById('highScoreText').textContent = '🎉 ハイスコア更新！';
        } else {
            document.getElementById('highScoreText').textContent = `ハイスコア: ${high.score}`;
        }
    } catch (e) {
        console.error(e);
    }
}

async function loadTitleHighScore() {
    try {
        const res = await fetch('/api/score/high');
        const high = await res.json();
        document.getElementById('titleHighScore').textContent = high.score;
    } catch (e) {
        document.getElementById('titleHighScore').textContent = '0';
    }
}

function moveHorizontal(dir) {
    if (!collides(current, dir, 0)) {
        current.x += dir;
    }
}

function tryRotate() {
    const rotated = rotatePiece(current);
    if (!collides(current, 0, 0, rotated.cells)) {
        current.cells = rotated.cells;
    }
}

async function softDrop() {
    if (!gameStarted || gameOver || isProcessing || paused) return;
    if (!collides(current, 0, 1)) {
        current.y++;
    } else {
        isProcessing = true;
        await lockPiece();
        isProcessing = false;
    }
}

// 進行度(0〜1)を、減速しながら止まる曲線に変換する(ease-out)
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

    // 今、落下アニメーション中のマス(行列で引けるようにSetにしておく)
    const fallingKeys = gravityMoves ? new Set(gravityMoves.map(m => `${m.toRow},${m.col}`)) : null;

    // 積まれた文字(落下アニメーション中のマスは、ここではスキップする)
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

    // 落下アニメーション中のマスを、途中の位置で描く
    if (gravityMoves) {
        const rawProgress = Math.min(1, (performance.now() - gravityStartTime) / GRAVITY_DURATION);
        const progress = easeOut(rawProgress);
        for (const m of gravityMoves) {
            const rowNow = m.fromRow + (m.toRow - m.fromRow) * progress;
            drawLetter(m.col, rowNow, m.ch);
        }
    }

    // 消えるアニメーション中のマス(縮小+フェード)
    if (clearingCells) {
        const progress = Math.min(1, (performance.now() - clearStartTime) / CLEAR_DURATION);
        for (const [key, letter] of clearingCells) {
            const [r, c] = key.split(',').map(Number);
            drawClearingLetter(c, r, letter, progress);
        }
    }

    // 落下中のミノ(固定済みで、まだ新しいミノに差し替わっていない間は描かない)
    if (showCurrentPiece) {
        for (const [cx, cy, letter] of current.cells) {
            const gy = visualY + cy;
            if (gy >= -1) drawLetter(current.x + cx, gy, letter);
        }
    }
}

function drawLetter(col, row, letter) {
    const x = col * CELL;
    const y = row * CELL;
    ctx.fillStyle = "#d9c9a3";
    ctx.fillRect(x + 2, y + 2, CELL - 4, CELL - 4);
    ctx.fillStyle = "#2b3a55";
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

    ctx.fillStyle = "#f4ecd8";
    ctx.fillRect(-(CELL - 4) / 2, -(CELL - 4) / 2, CELL - 4, CELL - 4);
    ctx.fillStyle = "#2b3a55";
    ctx.font = "bold 18px sans-serif";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText(letter, 0, 0);

    ctx.restore();
}

window.addEventListener('keydown', async (e) => {
    if (!gameStarted || gameOver) return;
   
    //Pキー or Escapeキーで、一時停止のオン・オフを切り替える(ポーズ中でも受け付ける)
    if(e.key === 'p' || e.key === 'P' || e.key === 'Escape') {
        e.preventDefault();
        togglePause();
        return;
    }

    if(paused) return; // ポーズ中は、これ以降の操作を受け付けない

    if(e.key === 'ArrowLeft') {e.preventDefault(); moveHorizontal(-1);}
    if(e.key === 'ArrowRight') {e.preventDefault(); moveHorizontal(1);}
    if(e.key === 'ArrowDown') {e.preventDefault(); await softDrop();}
    if(e.key === 'ArrowUp') {e.preventDefault(); tryRotate();}
});

setInterval(async () => {
    if (!gameStarted || gameOver || paused) return;
    await softDrop();
}, 900);

function animationLoop() {
    const diff = current.y - visualY;
    visualY += diff * 0.25;
    if (Math.abs(diff) < 0.01) visualY = current.y;

    draw();
    requestAnimationFrame(animationLoop);
}
animationLoop();

document.getElementById('startBtn').addEventListener('click', () => {
    document.getElementById('titleScreen').style.display = 'none';
    document.getElementById('gameStage').style.display = 'block';
    gameStarted = true;
});

document.getElementById('backToTitleBtn').addEventListener('click', () => {
    location.reload();
});

loadTitleHighScore();

// 一時停止のオン/オフを切り替える
function togglePause() {
    if (gameOver) return;
    paused = !paused;
    document.getElementById('pauseMessage').classList.toggle('show', paused);
}

document.getElementById('pauseBtn').addEventListener('click', () => togglePause());
document.getElementById('resumeBtn').addEventListener('click', () => togglePause());
document.getElementById('pauseBackToTitleBtn').addEventListener('click', () => location.reload());