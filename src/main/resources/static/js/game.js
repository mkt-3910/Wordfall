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
let isProcessing = false; // lockPiece()が処理中かどうかのフラグ

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

// ミノを1つ作る。cells各マスに、それぞれ別のランダムな文字を割り当てる
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

// 盤面から「縦・横・斜め」の全ライン(マスの並び)を取り出す
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

// 1本のライン(マスの配列)の中から、文字が3つ以上連続している部分を取り出す
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

// 盤面全体から「単語判定の候補」(文字列+座標)を集める
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

// 各列ごとに、消えたマスの分だけ上のマスを下に詰める(重力)
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

async function lockPiece() {
    for (const [cx, cy, letter] of current.cells) {
        const gx = current.x + cx;
        const gy = current.y + cy;
        if (gy >= 0) grid[gy][gx] = letter;
    }

    const candidates = collectCandidateRuns();
    const cellsToClear = new Set();

    for (const candidate of candidates) {
        const res = await fetch(`/api/check-word?word=${candidate.word}`);
        const isWord = await res.json();

        if (isWord) {
            score += candidate.word.length * 10;
            wordCount++;
            for (const cell of candidate.cells) {
                cellsToClear.add(`${cell.r},${cell.c}`);
            }

            const meaningRes = await fetch(`/api/meaning?word=${candidate.word}`);
            const meaning = await meaningRes.json();

            const wordLog = document.getElementById('wordLog');
            const entry = document.createElement('p');
            entry.textContent = `${meaning.word} (${meaning.partOfSpeech ?? '?'}) - ${meaning.definition}`;
            wordLog.prepend(entry);
        }
    }

    for (const key of cellsToClear) {
        const [r, c] = key.split(',').map(Number);
        grid[r][c] = null;
    }

    applyGravity();

    document.getElementById('score').textContent = score;
    document.getElementById('wordCount').textContent = wordCount;

    current = makePiece(randomKey());
    if (collides(current)) {
        life--;
        document.getElementById('life').textContent = life;

        if (life <= 0) {
            gameOver = true;
            document.getElementById('gameOverMessage').style.display = 'block';
            return;
        }

        grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));
    }

    draw();
}

function moveHorizontal(dir) {
    if (!collides(current, dir, 0)) {
        current.x += dir;
        draw();
    }
}

function tryRotate() {
    const rotated = rotatePiece(current);
    if (!collides(current, 0, 0, rotated.cells)) {
        current.cells = rotated.cells;
        draw();
    }
}

async function softDrop() {
    if (gameOver || isProcessing) return;
    if (!collides(current, 0, 1)) {
        current.y++;
    } else {
        isProcessing = true;
        await lockPiece();
        isProcessing = false;
    }
    draw();
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

    for (let r = 0; r < ROWS; r++) {
        for (let c = 0; c < COLS; c++) {
            if (grid[r][c]) drawLetter(c, r, grid[r][c]);
        }
    }

    for (const [cx, cy, letter] of current.cells) {
        const gy = current.y + cy;
        if (gy >= 0) drawLetter(current.x + cx, gy, letter);
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

window.addEventListener('keydown', async (e) => {
    if (gameOver) return;
    if (e.key === 'ArrowLeft') { e.preventDefault(); moveHorizontal(-1); }
    if (e.key === 'ArrowRight') { e.preventDefault(); moveHorizontal(1); }
    if (e.key === 'ArrowDown') { e.preventDefault(); await softDrop(); }
    if (e.key === 'ArrowUp') { e.preventDefault(); tryRotate(); }
});

setInterval(async () => {
    if (gameOver) return;
    await softDrop();
}, 900);

draw();