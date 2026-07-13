// キャンバスの取得と基本設定
const canvas = document.getElementById('board');
const ctx = canvas.getContext('2d');

const COLS = 9;
const ROWS = 13;
const CELL = 32;

// 盤面データ(全部nullで初期化)
let grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));

// 落下中の文字
let curCol = Math.floor(COLS / 2);
let curRow = 0;
let curLetter = randomLetter();

let score = 0;
let life = 3;

function randomLetter() {
    const letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    return letters[Math.floor(Math.random() * letters.length)];
}

function collidesBelow() {
    return curRow + 1 >= ROWS || grid[curRow + 1][curCol] !== null;
}

function lockPiece() {
    grid[curRow][curCol] = curLetter;
    // 今後Phase3で、ここで単語判定APIを呼び出す予定

    curCol = Math.floor(COLS / 2);
    curRow = 0;
    curLetter = randomLetter();

    // 一番上に置けない = ゲームオーバー扱い(ライフを減らす)
    if (grid[curRow][curCol]) {
        life--;
        document.getElementById('life').textContent = life;
        grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));
    }
}

function moveHorizontal(dir) {
    const nc = curCol + dir;
    if (nc < 0 || nc >= COLS) return;
    if (grid[curRow][nc] !== null) return;
    curCol = nc;
    draw();
}

function softDrop() {
    if (collidesBelow()) {
        lockPiece();
    } else {
        curRow++;
    }
    draw();
}

function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // 盤面のマス目線(縦線)
    ctx.strokeStyle = "rgba(237,232,222,0.08)";
    for (let c = 0; c <= COLS; c++) {
        ctx.beginPath();
        ctx.moveTo(c * CELL, 0);
        ctx.lineTo(c * CELL, ROWS * CELL);
        ctx.stroke();
    }
    // 盤面のマス目線(横線)
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

    // 落下中の文字
    drawLetter(curCol, curRow, curLetter);
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
});

// 一定間隔で自動落下
setInterval(() => {
    softDrop();
}, 700);

draw();