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
let gameStarted = false; // タイトル画面でスタートを押すまでは動かさない

// これまでに完成した単語を全部記録しておく(ハイスコア保存用)
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

function collectCandidateRuns() {
    const lines = getAllLines();
    const candidates = [];
    for (const line of lines) {
        for (const run of findRuns(line)) {
            const word = run.map(cell => cell.ch).join('');
            candidates.push({ word, cells: run });
        }
    }
    return candidates;
}

function applyGravity() {
    for (let c = 0; c < COLS; c++) {
        let writeRow = ROWS - 1;
        for (let r = ROWS - 1; r >= 0; r--) {
            if (grid[r][c] !== null) {
                grid[writeRow][c] = grid[r][c];
                if (writeRow !== r) grid[r][c] = null;
                writeRow--;
            }
        }
        for (let r = writeRow; r >= 0; r--) grid[r][c] = null;
    }
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

// 単語完成トーストを表示する
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
let clearingCells = null; // Map: "行,列" -> 文字
let clearStartTime = 0;
const CLEAR_DURATION = 350; // ミリ秒

async function lockPiece() {
    for (const [cx, cy, letter] of current.cells) {
        const gx = current.x + cx;
        const gy = current.y + cy;
        if (gy >= 0) grid[gy][gx] = letter;
    }

    applyGravity();

    const candidates = collectCandidateRuns();
    const cellsToClear = new Set();
    const foundWords = [];

    for (const candidate of candidates) {
        const res = await fetch(`/api/check-word?word=${candidate.word}`);
        const isWord = await res.json();

        if (isWord) {
            const points = candidate.word.length * 10;
            score += points;
            wordCount++;
            allFoundWords.push(candidate.word);
            for (const cell of candidate.cells) {
                cellsToClear.add(`${cell.r},${cell.c}`);
            }
            foundWords.push(candidate.word);
            showWordToast(candidate.word, points); // すぐにトースト表示
        }
    }

    if (cellsToClear.size > 0) {
        // 1. 消えるマスを記録して、アニメーション開始(draw()側で縮小+フェードを描画する)
        clearingCells = new Map();
        for (const key of cellsToClear) {
            const [r, c] = key.split(',').map(Number);
            clearingCells.set(key, grid[r][c]);
        }
        clearStartTime = performance.now();
        await wait(CLEAR_DURATION);
        clearingCells = null;

        // 2. 実際にマスを消す
        for (const key of cellsToClear) {
            const [r, c] = key.split(',').map(Number);
            grid[r][c] = null;
        }
        applyGravity();

        // 3. 意味を取得して一覧に追加
        for (const word of foundWords) {
            const meaningRes = await fetch(`/api/meaning?word=${word}`);
            const meaning = await meaningRes.json();

            const shortDefinition = simplifyDefinition(meaning.definition);

            const wordLog = document.getElementById('wordLog');
            const entry = document.createElement('p');
            entry.className = 'wordlog-entry';
            entry.textContent = `${meaning.word} (${meaning.partOfSpeech ?? '?'}) - ${shortDefinition}`;
            wordLog.prepend(entry);
        }
    }

    document.getElementById('score').textContent = score;
    document.getElementById('wordCount').textContent = wordCount;

    current = makePiece(randomKey());
    visualY = current.y;

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

// ハイスコアと比較して、更新されていればサーバーに保存する
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

// タイトル画面に、これまでのハイスコアを表示しておく
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
    if (!gameStarted || gameOver || isProcessing) return;
    if (!collides(current, 0, 1)) {
        current.y++;
    } else {
        isProcessing = true;
        await lockPiece();
        isProcessing = false;
    }
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

    // 積まれた文字(消えるアニメーション中のマスは、ここでは描かず後で別に描く)
    for (let r = 0; r < ROWS; r++) {
        for (let c = 0; c < COLS; c++) {
            if (grid[r][c]) {
                const key = `${r},${c}`;
                if (clearingCells && clearingCells.has(key)) continue;
                drawLetter(c, r, grid[r][c]);
            }
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

    // 落下中のミノ
    for (const [cx, cy, letter] of current.cells) {
        const gy = visualY + cy;
        if (gy >= -1) drawLetter(current.x + cx, gy, letter);
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

// 消えていく途中のマスを、縮小+フェードしながら描く
function drawClearingLetter(col, row, letter, progress) {
    const scale = 1 - progress * 0.6; // だんだん小さくなる
    const alpha = 1 - progress; // だんだん透明になる
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

// キー操作
window.addEventListener('keydown', async (e) => {
    if (!gameStarted || gameOver) return;
    if (e.key === 'ArrowLeft') { e.preventDefault(); moveHorizontal(-1); }
    if (e.key === 'ArrowRight') { e.preventDefault(); moveHorizontal(1); }
    if (e.key === 'ArrowDown') { e.preventDefault(); await softDrop(); }
    if (e.key === 'ArrowUp') { e.preventDefault(); tryRotate(); }
});

setInterval(async () => {
    if (!gameStarted || gameOver) return;
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

// タイトル画面のスタートボタン
document.getElementById('startBtn').addEventListener('click', () => {
    document.getElementById('titleScreen').style.display = 'none';
    document.getElementById('gameStage').style.display = 'block';
    gameStarted = true;
});

// ゲームオーバー画面の「タイトルに戻る」ボタン:ページを再読み込みして全部リセットする
document.getElementById('backToTitleBtn').addEventListener('click', () => {
    location.reload();
});

// 起動時に、タイトル画面のハイスコアを取得しておく
loadTitleHighScore();