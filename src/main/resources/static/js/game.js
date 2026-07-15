// =====================
// game.js (統合版)
// 演出（イージング・シャドウ・スコアポップ・LEVEL UP・単語スライド）を含む
// =====================

// キャンバスの取得と基本設定
const canvas = document.getElementById('board');
const ctx = canvas.getContext('2d');

const COLS = 12;
const ROWS = 13;
const CELL = 32;

// --- エフェクト設定 ---
const EFFECT_CONFIG = {
  cell: { duration: 400, maxScale: 1.18 },
  scorePopup: { duration: 700, rise: 36, font: "bold 18px sans-serif", color: "#ffd966" }
};

// エフェクトキュー
const cellEffects = new Map(); // key: "r,c" -> { r, c, start, duration, letter? }
const textEffects = []; // { x, y, text, start, duration }
const levelEffects = []; // { start, duration, text, x, y }

// 盤面データ
let grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));

let score = 0;
let life = 3;
let wordCount = 0;
let gameOver = false;
let isProcessing = false;

// ミノ定義
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

const LETTER_POOL = (
  "E".repeat(12) + "A".repeat(9) + "I".repeat(9) + "O".repeat(8) +
  "N".repeat(7) + "R".repeat(7) + "T".repeat(7) + "S".repeat(6) +
  "L".repeat(4) + "U".repeat(4) + "D".repeat(4) + "G".repeat(3) +
  "C".repeat(3) + "M".repeat(3) + "H".repeat(3) + "B".repeat(2) +
  "P".repeat(2) + "F".repeat(2) + "V".repeat(2) + "W".repeat(2) +
  "Y".repeat(2) + "K" + "J" + "X" + "Q" + "Z"
).split('');

function randomKey() { return KEYS[Math.floor(Math.random() * KEYS.length)]; }
function randomLetter() { return LETTER_POOL[Math.floor(Math.random() * LETTER_POOL.length)]; }

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

// 衝突判定
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

// ライン抽出（縦横斜め）
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
    const line = []; let r = 0, c = startCol;
    while (r < ROWS && c < COLS) { line.push({ r, c }); r++; c++; }
    lines.push(line);
  }
  for (let startRow = 1; startRow < ROWS; startRow++) {
    const line = []; let r = startRow, c = 0;
    while (r < ROWS && c < COLS) { line.push({ r, c }); r++; c++; }
    lines.push(line);
  }
  for (let startCol = 0; startCol < COLS; startCol++) {
    const line = []; let r = ROWS - 1, c = startCol;
    while (r >= 0 && c < COLS) { line.push({ r, c }); r--; c++; }
    lines.push(line);
  }
  for (let startRow = ROWS - 2; startRow >= 0; startRow--) {
    const line = []; let r = startRow, c = 0;
    while (r >= 0 && c < COLS) { line.push({ r, c }); r--; c++; }
    lines.push(line);
  }
  return lines;
}

function findRuns(line) {
  const runs = []; let cur = [];
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

function wait(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

// 座標ヘルパー
function boardToCanvasCell(row, col) { return { x: col * CELL, y: row * CELL, size: CELL }; }
function boardToCanvasCenter(row, col) { const { x, y, size } = boardToCanvasCell(row, col); return { x: x + size / 2, y: y + size / 2 }; }

// イージング
function easeOutQuad(t) { return t * (2 - t); }

// lockPiece（単語検出→演出→消去）
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
    try {
      const res = await fetch(`/api/check-word?word=${encodeURIComponent(candidate.word)}`);
      const isWord = await res.json();
      if (isWord) {
        score += candidate.word.length * 10;
        wordCount++;
        for (const cell of candidate.cells) cellsToClear.add(`${cell.r},${cell.c}`);
        foundWords.push(candidate.word);
      }
    } catch (e) {
      // API エラーは無視して続行
    }
  }

  if (cellsToClear.size > 0) {
    const now = performance.now();
    // 登録（演出中は grid に残す方式）
    for (const key of cellsToClear) {
      const [r, c] = key.split(',').map(Number);
      cellEffects.set(key, { r, c, start: now, duration: EFFECT_CONFIG.cell.duration });
    }

    // スコアポップ
    if (foundWords.length > 0) {
      const cellsArr = Array.from(cellsToClear).map(k => k.split(',').map(Number));
      const avg = cellsArr.reduce((acc, [r, c]) => { acc.r += r; acc.c += c; return acc; }, { r:0, c:0 });
      avg.r /= cellsArr.length; avg.c /= cellsArr.length;
      const center = boardToCanvasCenter(Math.round(avg.r), Math.round(avg.c));
      textEffects.push({ x: center.x, y: center.y, text: `+${Array.from(cellsToClear).length * 10}`, start: now, duration: EFFECT_CONFIG.scorePopup.duration });
    }

    await wait(EFFECT_CONFIG.cell.duration);

    // 実際に消す
    for (const key of cellsToClear) {
      const [r, c] = key.split(',').map(Number);
      grid[r][c] = null;
      cellEffects.delete(key);
    }
    applyGravity();

    // 意味取得とログ追加
    for (const word of foundWords) {
      try {
        const meaningRes = await fetch(`/api/meaning?word=${encodeURIComponent(word)}`);
        const meaning = await meaningRes.json();
        const shortDefinition = simplifyDefinition(meaning.definition);
        const wordLog = document.getElementById('wordLog');
        const entry = document.createElement('p');
        entry.className = 'wordlog-entry';
        entry.textContent = `${meaning.word} (${meaning.partOfSpeech ?? '?'}) - ${shortDefinition}`;
        wordLog.prepend(entry);
        addWordSlideEntry(meaning.word, shortDefinition, meaning.partOfSpeech ?? '');
      } catch (e) {
        // 無視
      }
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
      const overlay = document.getElementById('gameOverMessage');
      if (overlay) overlay.classList.add('show');
      return;
    }
    grid = Array.from({ length: ROWS }, () => Array(COLS).fill(null));
  }

  draw();
}

function moveHorizontal(dir) { if (!collides(current, dir, 0)) current.x += dir; }
function tryRotate() { const rotated = rotatePiece(current); if (!collides(current, 0, 0, rotated.cells)) current.cells = rotated.cells; }

async function softDrop() {
  if (gameOver || isProcessing) return;
  if (!collides(current, 0, 1)) current.y++;
  else { isProcessing = true; await lockPiece(); isProcessing = false; }
}

// 描画
function draw(timestamp) {
  const now = timestamp || performance.now();
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // グリッド線
  ctx.strokeStyle = "rgba(237,232,222,0.08)";
  for (let c = 0; c <= COLS; c++) { ctx.beginPath(); ctx.moveTo(c * CELL, 0); ctx.lineTo(c * CELL, ROWS * CELL); ctx.stroke(); }
  for (let r = 0; r <= ROWS; r++) { ctx.beginPath(); ctx.moveTo(0, r * CELL); ctx.lineTo(COLS * CELL, r * CELL); ctx.stroke(); }

  // 積まれた文字（通常）
  for (let r = 0; r < ROWS; r++) {
    for (let c = 0; c < COLS; c++) {
      if (grid[r][c]) {
        const key = `${r},${c}`;
        const effect = cellEffects.get(key);
        if (!effect) drawLetter(c, r, grid[r][c], { alpha: 1, scale: 1 });
      }
    }
  }

  // 落下中のミノ（visualY）
  for (const [cx, cy, letter] of current.cells) {
    const gy = visualY + cy;
    if (gy >= -1) drawLetter(current.x + cx, gy, letter, { alpha: 1, scale: 1 });
  }

  // エフェクト描画
  updateAndRenderCellEffects(now);
  updateAndRenderTextEffects(now);
  updateAndRenderLevelEffects(now);
}

function drawLetter(col, row, letter, options = { alpha: 1, scale: 1 }) {
  const x = col * CELL;
  const y = row * CELL;
  const alpha = options.alpha ?? 1;
  const scale = options.scale ?? 1;

  ctx.save();
  const centerX = x + CELL / 2;
  const centerY = y + CELL / 2;
  ctx.translate(centerX, centerY);
  ctx.scale(scale, scale);
  ctx.translate(-centerX, -centerY);

  ctx.globalAlpha = alpha;
  ctx.fillStyle = "#d9c9a3";
  ctx.fillRect(x + 2, y + 2, CELL - 4, CELL - 4);

  ctx.fillStyle = "#2b3a55";
  ctx.font = "bold 18px sans-serif";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(letter, x + CELL / 2, y + CELL / 2);

  ctx.restore();
}

// エフェクト更新＆描画（イージング適用）
function updateAndRenderCellEffects(now) {
  for (const [key, effect] of Array.from(cellEffects.entries())) {
    const elapsed = now - effect.start;
    const rawT = Math.min(Math.max(elapsed / effect.duration, 0), 1);
    const t = easeOutQuad(rawT);

    const alpha = 1 - t;
    const scale = 1 + (EFFECT_CONFIG.cell.maxScale - 1) * t;

    const { x, y, size } = boardToCanvasCell(effect.r, effect.c);
    const letter = grid[effect.r] ? grid[effect.r][effect.c] : null;

    drawCellEffect(x, y, size, letter, alpha, scale);

    if (rawT >= 1) cellEffects.delete(key);
  }
}

function drawCellEffect(x, y, size, letter, alpha, scale) {
  ctx.save();
  const cx = x + size / 2;
  const cy = y + size / 2;
  ctx.translate(cx, cy);
  ctx.scale(scale, scale);
  ctx.translate(-cx, -cy);

  ctx.globalAlpha = alpha;
  ctx.shadowColor = "rgba(255,228,94,0.6)";
  ctx.shadowBlur = 12 * alpha;

  ctx.fillStyle = "#FFE45E";
  ctx.fillRect(x + 2, y + 2, size - 4, size - 4);

  ctx.shadowBlur = 0;

  if (letter) {
    ctx.fillStyle = "#000000";
    ctx.font = "bold 18px sans-serif";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillText(letter, x + size / 2, y + size / 2);
  }

  ctx.restore();
}

function updateAndRenderTextEffects(now) {
  for (let i = textEffects.length - 1; i >= 0; i--) {
    const ef = textEffects[i];
    const elapsed = now - ef.start;
    const rawT = Math.min(Math.max(elapsed / ef.duration, 0), 1);
    const t = easeOutQuad(rawT);

    if (rawT >= 1) { textEffects.splice(i, 1); continue; }

    const alpha = 1 - t;
    const offsetY = -EFFECT_CONFIG.scorePopup.rise * t;
    const scale = 1 + 0.12 * (1 - Math.abs(0.5 - t) * 2);

    drawTextEffect(ef.x, ef.y + offsetY, ef.text, alpha, scale);
  }
}

function drawTextEffect(x, y, text, alpha, scale = 1) {
  ctx.save();
  ctx.globalAlpha = alpha;
  ctx.translate(x, y);
  ctx.scale(scale, scale);
  ctx.font = EFFECT_CONFIG.scorePopup.font;
  ctx.fillStyle = EFFECT_CONFIG.scorePopup.color;
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.shadowColor = "rgba(0,0,0,0.35)";
  ctx.shadowBlur = 8;
  ctx.fillText(text, 0, 0);
  ctx.restore();
}

// レベルエフェクト
let level = 1;
function triggerLevelUp(newLevel) {
  level = newLevel ?? (level + 1);
  const now = performance.now();
  const duration = 1200;
  const x = canvas.width / 2;
  const y = canvas.height / 2;
  levelEffects.push({ start: now, duration, text: `LEVEL UP! ${level}`, x, y });
}

function updateAndRenderLevelEffects(now) {
  for (let i = levelEffects.length - 1; i >= 0; i--) {
    const ef = levelEffects[i];
    const rawT = Math.min(Math.max((now - ef.start) / ef.duration, 0), 1);
    if (rawT >= 1) { levelEffects.splice(i, 1); continue; }
    const t = easeOutQuad(rawT);
    const scale = 1 + 0.6 * (1 - Math.abs(0.5 - t) * 2);
    const alpha = 1 - t;
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.translate(ef.x, ef.y);
    ctx.scale(scale, scale);
    ctx.font = "bold 48px sans-serif";
    ctx.fillStyle = "#ffdd57";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.shadowColor = "rgba(0,0,0,0.4)";
    ctx.shadowBlur = 12;
    ctx.fillText(ef.text, 0, 0);
    ctx.restore();
  }
}

// キー操作
window.addEventListener('keydown', async (e) => {
  if (gameOver) return;
  if (e.key === 'ArrowLeft') { e.preventDefault(); moveHorizontal(-1); }
  if (e.key === 'ArrowRight') { e.preventDefault(); moveHorizontal(1); }
  if (e.key === 'ArrowDown') { e.preventDefault(); await softDrop(); }
  if (e.key === 'ArrowUp') { e.preventDefault(); tryRotate(); }
});

// 自動落下
setInterval(async () => { if (gameOver) return; await softDrop(); }, 900);

// アニメーションループ
function animationLoop(timestamp) {
  const diff = current.y - visualY;
  visualY += diff * 0.25;
  if (Math.abs(diff) < 0.01) visualY = current.y;
  draw(timestamp || performance.now());
  requestAnimationFrame(animationLoop);
}
requestAnimationFrame(animationLoop);

// ---------------------------
// 完成単語スライドパネル（DOMベース）
// ---------------------------
function ensureWordSlidePanel() {
  if (document.getElementById('wordSlidePanel')) return;
  const panel = document.createElement('div');
  panel.id = 'wordSlidePanel';
  panel.style.position = 'fixed';
  panel.style.right = '-320px';
  panel.style.top = '80px';
  panel.style.width = '300px';
  panel.style.maxHeight = '60vh';
  panel.style.background = 'rgba(20,20,30,0.95)';
  panel.style.color = '#fff';
  panel.style.borderRadius = '8px 0 0 8px';
  panel.style.boxShadow = '-6px 6px 24px rgba(0,0,0,0.45)';
  panel.style.padding = '12px';
  panel.style.overflow = 'hidden';
  panel.style.transition = 'right 360ms cubic-bezier(.2,.9,.2,1), opacity 360ms';
  panel.style.opacity = '0';
  panel.style.zIndex = '1200';
  panel.style.display = 'flex';
  panel.style.flexDirection = 'column';
  panel.style.gap = '8px';
  panel.style.fontFamily = '"Noto Sans JP", sans-serif';
  panel.innerHTML = `
    <div class="panel-header" style="display:flex;justify-content:space-between;align-items:center;">
      <h4 style="margin:0;font-size:14px;letter-spacing:0.6px;color:#ffd966;">Completed Words</h4>
      <div><button class="close-btn" title="Close" style="background:transparent;border:none;color:#fff;cursor:pointer;font-size:14px;opacity:0.8;">✕</button></div>
    </div>
    <div class="panel-list" role="list" style="margin-top:8px;overflow-y:auto;padding-right:6px;"></div>
  `;
  document.body.appendChild(panel);
  panel.querySelector('.close-btn').addEventListener('click', () => hideWordSlidePanel());
  panel.addEventListener('mouseenter', () => panel.dataset.hover = '1');
  panel.addEventListener('mouseleave', () => delete panel.dataset.hover);
}

let autoHideTimer = null;
function showWordSlidePanel() {
  ensureWordSlidePanel();
  const panel = document.getElementById('wordSlidePanel');
  panel.style.right = '16px';
  panel.style.opacity = '1';
  if (autoHideTimer) { clearTimeout(autoHideTimer); autoHideTimer = null; }
}

function hideWordSlidePanel() {
  const panel = document.getElementById('wordSlidePanel');
  if (!panel) return;
  if (panel.dataset.hover) { scheduleAutoHidePanel(1200); return; }
  panel.style.right = '-320px';
  panel.style.opacity = '0';
}

function scheduleAutoHidePanel(delay) {
  if (autoHideTimer) clearTimeout(autoHideTimer);
  autoHideTimer = setTimeout(() => {
    const panel = document.getElementById('wordSlidePanel');
    if (!panel) return;
    if (panel.dataset.hover) { scheduleAutoHidePanel(1200); return; }
    panel.style.right = '-320px';
    panel.style.opacity = '0';
    autoHideTimer = null;
  }, delay);
}

function escapeHtml(s) {
  if (!s) return '';
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
}

function addWordSlideEntry(word, definition, partOfSpeech = '') {
  ensureWordSlidePanel();
  const panel = document.getElementById('wordSlidePanel');
  const list = panel.querySelector('.panel-list');
  const entry = document.createElement('div');
  entry.className = 'word-slide-entry';
  entry.style.background = 'linear-gradient(180deg, rgba(255,255,255,0.03), rgba(255,255,255,0.01))';
  entry.style.borderRadius = '6px';
  entry.style.padding = '8px';
  entry.style.fontSize = '13px';
  entry.style.lineHeight = '1.2';
  entry.style.display = 'flex';
  entry.style.flexDirection = 'column';
  entry.style.gap = '4px';
  entry.style.transformOrigin = 'right center';
  entry.style.willChange = 'transform, opacity';
  entry.innerHTML = `<div class="word" style="font-weight:700;color:#fff;">${escapeHtml(word)}</div>
                     <div class="meta" style="font-size:12px;color:#d6d6d6;">${escapeHtml(partOfSpeech)} ${escapeHtml(definition)}</div>`;
  list.prepend(entry);
  entry.style.opacity = '0';
  entry.style.transform = 'translateX(8px) scale(0.98)';
  requestAnimationFrame(() => {
    entry.style.transition = 'transform 360ms cubic-bezier(.2,.9,.2,1), opacity 360ms';
    entry.style.opacity = '1';
    entry.style.transform = 'translateX(0) scale(1)';
  });
  const MAX_ENTRIES = 8;
  while (list.children.length > MAX_ENTRIES) {
    const last = list.lastElementChild;
    if (!last) break;
    last.style.transition = 'opacity 240ms';
    last.style.opacity = '0';
    setTimeout(() => last.remove(), 260);
  }
  showWordSlidePanel();
  scheduleAutoHidePanel(2500);
}
