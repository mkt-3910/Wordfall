// キャンバスの取得と基本設定
const canvas = document.getElementById('board');
const ctx = canvas.getContext('2d');

const COLS = 9;
const ROWS = 13;
const CELL = 32;

// 盤面データ:各マスに { letter: "A" } か null が入る
let grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));

let score = 0;
let life = 3;

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

function randomLetter() {
    const letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    return letters[Math.floor(Math.random() * letters.length)];
}

// ミノを1つ作る。cells各マスに、それぞれ別のランダムな文字を割り当てる
function makePiece(key) {
    const def = SHAPES[key];
    return {
        key,
        size: def.size,
        // cells: [x, y, letter] の組にする
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
    // 座標だけ回転させ、文字はそのまま引き継ぐ
    const rotated = piece.cells.map(([x, y, letter]) => [s - 1 - y, x, letter]);
    return { ...piece, cells: rotated };
}

function lockPiece() {
    for (const [cx, cy, letter] of current.cells) {
        const gx = current.x + cx;
        const gy = current.y + cy;
        if (gy >= 0) grid[gy][gx] = letter;
    }
    // 今後Phase3で、ここで単語判定APIを呼び出す予定
    console.log(collectCandidateWords());
    current = makePiece(randomKey());
    if (collides(current)) {
        life--;
        document.getElementById('life').textContent = life;
        grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));
    }
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

function softDrop() {
    if (!collides(current, 0, 1)) {
        current.y++;
    } else {
        lockPiece();
    }
    draw();
}

function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // マス目線
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

    // 積まれた文字
    for (let r = 0; r < ROWS; r++) {
        for (let c = 0; c < COLS; c++) {
            if (grid[r][c]) drawLetter(c, r, grid[r][c]);
        }
    }

    // 落下中のミノ(4マス分)
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

// キー操作
window.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowLeft') { e.preventDefault(); moveHorizontal(-1); }
    if (e.key === 'ArrowRight') { e.preventDefault(); moveHorizontal(1); }
    if (e.key === 'ArrowDown') { e.preventDefault(); softDrop(); }
    if (e.key === 'ArrowUp') { e.preventDefault(); tryRotate(); }
});

// 一定間隔で自動落下
setInterval(() => {
    softDrop();
}, 700);

// 盤面から「縦・横・斜め」の全ライン(マスの並び)を取り出す
function getAllLines() {
    const lines = [];
}

    //横方向・各列を1本のラインとする
    for(let c = 0; c < COLS; c++) {
        const line = [];
        for(let r = 0; r < ROWS; r++) line.push({r,c});
        lines.push(line);
    }

    // 斜め(右下がり ↘):左端・上端から始まる全ての斜めラインを集める
    for (let startCol = 0; startCol < COLS; startCol++) {
        const line = [];
        let r = ROWS - 1, c = startCol;
        while( r >= 0 && c < COLS) { line.push({ r,c}); r--; c++; }
        lines.push(line);
    }
    for (let startRow = ROWS -2; startRow >= 0; startRow--) {
        const line = [];
        let r = startRow, c= 0;
        while(r >= 0 && c < COLS) { line.push({ r,c }); r--; c++; }
        line.push(line);
    }

    return lines;

    //1本のライン(マスの配列)の中から、文字が3つ以上連続している部分を取り出す
    function findRuns(line) {
        const runs = [];
        let cur = [];
        for(const {r,c} of line) {
            if(grid[r][c] !== null) {
                cur.push({ r,c, ch: grid[r][c] });
            } else {
                if(cur.length >= 3) runs.push(cur);
                cur = [];
            }
        }
        if(cur.length >= 3) runs.push(cur);
        return runs;
    }

    // 盤面全体から「単語判定の候補」になりうる文字列を集めて、コンソールに表示する(確認用)
    function collectCandidateWords() {
        const lines = getAllLines();
        const candidates = [];
        for (const line of lines) {
            for(const run of findRuns(line)) {
                const word = run.map(cell => cell.ch).join('');
                candidates.push(word);
            }
        }
        return candidates;
    }

draw();