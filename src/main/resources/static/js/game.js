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
let combo = 0;
let bestComboThisGame = 0;
let bestMultiWordThisGame = 0;
let got4LetterThisGame = false;
let got5LetterThisGame = false;
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
    "E".repeat(14) + "A".repeat(11) + "I".repeat(10) + "O".repeat(8) +
    "N".repeat(8) + "T".repeat(8) + "S".repeat(8) + "R".repeat(7) +
    "L".repeat(5) + "D".repeat(5) + "U".repeat(3) + "C".repeat(3) +
    "M".repeat(3) + "G".repeat(2) + "H".repeat(2) + "B".repeat(2) +
    "P".repeat(2) + "F" + "Y" + "W" + "V" + "K" + "J" + "X" + "Q" + "Z"
).split('');

// ときどき、ミノ内の一直線に基本的な3文字単語を仕込む。
// 完成形を固定しすぎず、偶然単語がそろう楽しさも残す。
const WORD_FRIENDLY_CHANCE = 0.7;
const COMMON_THREE_LETTER_WORDS = [
    'ACT', 'ADD', 'AGE', 'AIR', 'ALL', 'AND', 'ANT', 'ANY', 'ARM', 'ART',
    'ASK', 'BAG', 'BAT', 'BED', 'BEE', 'BIG', 'BOX', 'BOY', 'BUS', 'CAR',
    'CAT', 'CUP', 'DAY', 'DOG', 'EAR', 'EAT', 'EGG', 'FAN', 'FOX', 'FUN',
    'HAT', 'ICE', 'KEY', 'MAN', 'MAP', 'PEN', 'PIG', 'RED', 'RUN', 'SEA',
    'SIT', 'SUN', 'TOP', 'TOY', 'WIN', 'APE', 'APP', 'ACE', 'AID', 'AIM',
    'BAD', 'BAR', 'BIT', 'BUY', 'CAN', 'CAP', 'COW', 'CRY', 'CUT', 'DAD',
    'DIE', 'DRY', 'END', 'EYE', 'FAR', 'FAT', 'FEW', 'FLY', 'GET', 'GOD',
    'GUN', 'GUY', 'GYM', 'HIT', 'HOT', 'HOW', 'JOB', 'JOY', 'KID', 'LEG',
    'LIE', 'LIP', 'LOT', 'LOW', 'MAY', 'MOM', 'NEW', 'NOT', 'NOW', 'NUT',
    'OLD', 'ONE', 'OWN', 'PAY', 'PUT', 'RAW', 'SAD', 'SAY', 'SEE', 'SET',
    'SKY', 'SON', 'TEA', 'TEN', 'TWO', 'USE', 'WAR', 'WAY', 'WEB', 'WET',
    'WHY', 'YES', 'YET', 'ZOO'
];
const COMMON_FOUR_LETTER_WORDS = [
    'BOOK', 'GAME', 'WORD', 'PLAY', 'READ', 'BLUE', 'HOME', 'LOVE', 'TIME', 'TREE',
    'ABLE', 'BABY', 'BALL', 'BIRD', 'BOAT', 'CAKE', 'CALL', 'CARD', 'CITY', 'COOK',
    'EASY', 'FACE', 'FARM', 'FIRE', 'FISH', 'FOOD', 'GIRL', 'GOOD', 'HAND', 'HELP',
    'HOPE', 'JUMP', 'LIFE', 'MAKE', 'MILK', 'MOON', 'RAIN', 'RICE', 'ROAD', 'ROOM',
    'SHOP', 'SING', 'SNOW', 'SONG', 'STAR', 'TEAM', 'WALK', 'WARM', 'WASH', 'WISH'
];
const WORD_LINES = {
    I: [[[0, 1], [1, 1], [2, 1]], [[1, 1], [2, 1], [3, 1]]],
    T: [[[0, 1], [1, 1], [2, 1]]],
    J: [[[0, 1], [1, 1], [2, 1]]],
    L: [[[0, 1], [1, 1], [2, 1]]]
};

function randomLetter() {
    return LETTER_POOL[Math.floor(Math.random() * LETTER_POOL.length)];
}

function makePiece(key) {
    const def = SHAPES[key];
    const cells = def.cells.map(([x, y]) => [x, y, randomLetter()]);
    const possibleLines = WORD_LINES[key];

    if (possibleLines && Math.random() < WORD_FRIENDLY_CHANCE) {
        const useFourLetterWord = key === 'I' && Math.random() < 0.35;
        const words = useFourLetterWord ? COMMON_FOUR_LETTER_WORDS : COMMON_THREE_LETTER_WORDS;
        const word = words[Math.floor(Math.random() * words.length)];
        const line = useFourLetterWord
            ? [[0, 1], [1, 1], [2, 1], [3, 1]]
            : possibleLines[Math.floor(Math.random() * possibleLines.length)];
        line.forEach(([wordX, wordY], index) => {
            const cell = cells.find(([cellX, cellY]) => cellX === wordX && cellY === wordY);
            cell[2] = word[index];
        });
    }

    return {
        key,
        size: def.size,
        cells,
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

async function collectCandidateRuns() {
    const lines = getAllLines();
    const runs = lines.flatMap(findRuns);

    // 各runの部分文字列(subs)を先に全部洗い出す
    const runsWithSubs = runs.map(run => {
        const s = run.map(cell => cell.ch).join('');
        const n = s.length;
        const subs = [];
        const seen = new Set();
        for (let start = 0; start < n; start++) {
            for (let end = start + 3; end <= n; end++) {
                const forward = s.slice(start, end);
                const backward = [...forward].reverse().join('');
                for (const word of [forward, backward]) {
                    const key = `${start}:${end}:${word}`;
                    if (!seen.has(key)) {
                        seen.add(key);
                        subs.push({ start, end, word });
                    }
                }
            }
        }
        return { run, subs };
    });

    const uniqueWords = [...new Set(runsWithSubs.flatMap(({ subs }) => subs.map(sub => sub.word)))];
    if (uniqueWords.length === 0) return [];

    const checkResults = {};
    const batchSize = 200;
    for (let start = 0; start < uniqueWords.length; start += batchSize) {
        const response = await fetch('/api/check-words', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(uniqueWords.slice(start, start + batchSize))
        });
        if (!response.ok) {
            throw new Error(`単語判定に失敗しました (${response.status})`);
        }
        Object.assign(checkResults, await response.json());
    }

    // 結果をrunごとに組み立て
    const allMatches = [];
    for (const { run, subs } of runsWithSubs) {
        const candidates = [];
        subs.forEach(sub => {
            if (checkResults[sub.word] === true) {
                candidates.push({ start: sub.start, end: sub.end, len: sub.end - sub.start, word: sub.word });
            }
        });

        candidates.sort((a, b) => b.len - a.len);
        const used = new Array(run.length).fill(false);
        for (const c of candidates) {
            let overlap = false;
            for (let i = c.start; i < c.end; i++) if (used[i]) { overlap = true; break; }
            if (overlap) continue;
            for (let i = c.start; i < c.end; i++) used[i] = true;
            allMatches.push({ word: c.word, cells: run.slice(c.start, c.end) });
        }
    }

    return allMatches;
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

function showWordToast(word,points) {
    showToastMessage(`${word}完成! +${points}`);
}
// 完成した単語を、単語帳(データベース)に保存する
async function saveWordLog(word, partOfSpeech, meaning) {
    try {
        const response = await fetch('/api/word-log', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                word: word,
                partOfSpeech: partOfSpeech,
                meaning: meaning
            })
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
    } catch (e) {
        console.error('単語帳への保存に失敗しました', e);
    }
}

let clearingCells = null;
let clearStartTime = 0;
const CLEAR_DURATION = 220;

const meaningCache = new Map();
function fetchMeaning(word) {
    if (!meaningCache.has(word)) {
        const request =
            fetch(`/api/meaning?word=${word}`)
                .then(res => res.ok ? res.json() : null)
                .then(result => {
                    if (!result) meaningCache.delete(word);
                    return result;
                })
                .catch(() => {
                    meaningCache.delete(word);
                    return null;
                });
        meaningCache.set(word, request);
    }
    return meaningCache.get(word);
}

async function fetchGuaranteedMeaning(word) {
    for (let attempt = 0; attempt < 2; attempt++) {
        const meaning = await fetchMeaning(word);
        if (meaning) return meaning;
        meaningCache.delete(word);
    }
    throw new Error(`${word}の意味を取得できませんでした`);
}

async function lockPiece() {
    for (const [cx, cy, letter] of current.cells) {
        const gx = current.x + cx;
        const gy = current.y + cy;
        if (gy >= 0) grid[gy][gx] = letter;
    }

    showCurrentPiece = false;

    const firstMoves = applyGravity();
    await animateGravity(firstMoves);

    const candidates = await collectCandidateRuns();
    const cellsToClear = new Set();
    const wordLogEntries = [];

    const meanings = await Promise.all(
        candidates.map(candidate => fetchGuaranteedMeaning(candidate.word))
    );

    // 成立対象は日本語の意味を持つローカル辞書の単語だけなので、全件を完成扱いにする。
    const successCount = candidates.length;

    //コンボ:単語ができた着地が連続するとコンボが伸びる。できなければリセット
    if(successCount > 0) {
        combo++;
    } else {
        combo = 0;
    }

    if(combo > bestComboThisGame) {
        bestComboThisGame = combo;
    }
    document.getElementById('combo').textContent = combo;

    const hasBigWord = candidates.some(candidate => candidate.word.length >= 4);
    if (hasBigWord) {
        triggerScreenEffect('flash');
    }

    const comboBonus = combo > 1 ? (combo - 1) * 5 : 0;

    //②マルチワードボーナス:1回の着地で2単語以上同時にできた時のボーナス
    const multiWordBonus = successCount >= 2 ? successCount * 20 : 0;
    if (successCount > bestMultiWordThisGame) {
        bestMultiWordThisGame = successCount;
    }
    if(multiWordBonus > 0) {
        score += multiWordBonus;
    }
    candidates.forEach((candidate, i) => {
        const meaning = meanings[i];
        if(candidate.word.length >= 4) {
            got4LetterThisGame = true;
        }
        if (candidate.word.length >= 5) {
            got5LetterThisGame = true;
        }

        const points = candidate.word.length * 10 + comboBonus;

        score += points;
        wordCount++;
        allFoundWords.push(candidate.word);
        for (const cell of candidate.cells) {
            cellsToClear.add(`${cell.r},${cell.c}`);
        }
        showWordToast(candidate.word, points);

        const firstCell = candidate.cells[0];
        showScorePopup(firstCell.c,firstCell.r,points);

        const shortDefinition = simplifyDefinition(meaning.definition);
        wordLogEntries.push(`${meaning.word} (${meaning.partOfSpeech ?? '?'}) - ${shortDefinition}`);

        void saveWordLog(meaning.word, meaning.partOfSpeech ?? '', meaning.definition);
    });

    if (multiWordBonus > 0) {
        showToastMessage(`${successCount}単語同時！ ボーナス +${multiWordBonus}`);
        triggerScreenEffect('shake');
    }

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
            await sendAchievementCheck();
            return;
        }

        grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));
    }
}

async function checkAndSaveHighScore() {
    try {
        const res = await fetch('/api/score/high');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const high = await res.json();
        const isNewHighScore = score > high.score;

        const saveResponse = await fetch('/api/score', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                score: score,
                wordCount: wordCount,
                words: allFoundWords.join(',')
            })
        });
        if (!saveResponse.ok) throw new Error(`HTTP ${saveResponse.status}`);

        if (isNewHighScore) {
            document.getElementById('highScoreText').textContent = '🎉 ハイスコア更新！';
        } else {
            document.getElementById('highScoreText').textContent = `ハイスコア: ${high.score}`;
        }
    } catch (e) {
        console.error(e);
    }
}

async function sendAchievementCheck() {
    try {
        const response = await fetch('/api/achievements/check', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                wordsCompletedThisGame: wordCount,
                bestComboThisGame: bestComboThisGame,
                bestMultiWordThisGame: bestMultiWordThisGame,
                got4LetterThisGame: got4LetterThisGame,
                got5LetterThisGame: got5LetterThisGame,
                finalScore: score
            })
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
    } catch (e) {
        console.error('実績判定への送信に失敗しました', e);
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
        try {
            await lockPiece();
        } catch (error) {
            console.error('ゲーム処理に失敗しました', error);
            showToastMessage('通信に失敗しました。もう一度お試しください');
        } finally {
            isProcessing = false;
        }
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

    if (showCurrentPiece) {
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


document.getElementById('vocabularyBtn').addEventListener('click', () => {
    location.href = '/vocabulary';
});

document.getElementById('historyBtn').addEventListener('click', () => {
    location.href = '/history';
});

document.getElementById('achievementsBtn').addEventListener('click', () => {
    location.href = '/achievements';
});

document.getElementById('backToTitleBtn').addEventListener('click', () => {
    location.reload();
});

loadTitleHighScore();

function togglePause() {
    if (gameOver) return;
    paused = !paused;
    document.getElementById('pauseMessage').classList.toggle('show', paused);
}

document.getElementById('pauseBtn').addEventListener('click', () => togglePause());
document.getElementById('resumeBtn').addEventListener('click', () => togglePause());
document.getElementById('pauseBackToTitleBtn').addEventListener('click', () => location.reload());
