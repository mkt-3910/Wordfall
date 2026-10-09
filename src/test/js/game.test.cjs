const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const { randomUUID } = require('node:crypto');

const EMPTY_ROWS = Array(13).fill('.'.repeat(12));
const PIECE = { size: 3, x: 4, y: 0, cells: [{ x: 0, y: 1, letter: 'C' }, { x: 1, y: 1, letter: 'A' }, { x: 2, y: 1, letter: 'T' }] };

function reply(value, status = 200) {
    return { ok: status < 400, status, headers: { get: () => null }, text: async () => (value === null ? '' : JSON.stringify(value)) };
}

function landingResult(overrides = {}) {
    return {
        index: 0, words: [], multiWordBonus: 0, combo: 0, score: 0, wordCount: 0, life: 3, lifeLost: false,
        board: EMPTY_ROWS, nextPiece: PIECE, result: null, ...overrides
    };
}

// ブラウザの代わりに最小限の document / fetch を用意して game.js を読み込む
function game(server) {
    const elements = new Map();
    const element = id => {
        if (!elements.has(id)) {
            const classes = new Set();
            elements.set(id, {
                textContent: '', hidden: false, disabled: false, style: {}, listeners: {}, children: [],
                classList: { add: x => classes.add(x), remove: x => classes.delete(x),
                    toggle: (x, on) => (on ? classes.add(x) : classes.delete(x)), contains: x => classes.has(x) },
                getContext: () => new Proxy({}, { get: () => () => {} }),
                addEventListener(name, callback) { this.listeners[name] = callback; },
                prepend(child) { this.children.unshift(child); }, appendChild() {}, remove() {}
            });
        }
        return elements.get(id);
    };
    const calls = [];
    const ctx = vm.createContext({
        document: { getElementById: element, querySelector: element, createElement: () => element(randomUUID()) },
        window: { addEventListener() {} }, location: { reload() {}, href: '' },
        console: { error() {} }, AbortController,
        performance: { now: () => 0 }, setInterval() {}, requestAnimationFrame() {},
        setTimeout(callback, ms) { if (ms !== 8000) queueMicrotask(callback); return 1; }, clearTimeout() {},
        fetch: async (url, options = {}) => {
            calls.push({ url, body: options.body ? JSON.parse(options.body) : undefined });
            if (url === '/api/score/high') return reply({ score: 0 });
            return server(url, options);
        }
    });
    const source = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/game.js'), 'utf8');
    vm.runInContext(source, ctx);
    return { run: code => vm.runInContext(code, ctx), element, calls };
}

async function started(server) {
    const g = game((url, options) => (url === '/api/games'
        ? reply({ gameId: 'g1', maxLandings: 10000, life: 3, board: EMPTY_ROWS, piece: PIECE })
        : server(url, options)));
    await g.run('startGame()');
    return g;
}

// 現在のミノを一番下まで落としてから、もう1段落とそうとして着地させる
async function dropToBottom(g) {
    g.run('current.y = ROWS - 2;');
    await g.run('softDrop()');
}

test('start uses the board and piece chosen by the server', async () => {
    const g = await started(() => assert.fail('unexpected request'));
    assert.equal(g.element('gameStage').hidden, false);
    assert.equal(g.element('titleScreen').hidden, true);
    assert.equal(g.run('current.cells.map(c => c[2]).join("")'), 'CAT');
    assert.equal(g.run('gameStarted && landingIndex === 0'), true);
});

test('landing sends only the position and applies the server result', async () => {
    const g = await started(() => reply(landingResult({
        words: [{ word: 'CAT', partOfSpeech: '名詞', definition: '猫', points: 30, cells: [[12, 4], [12, 5], [12, 6]] }],
        combo: 1, score: 30, wordCount: 1
    })));
    g.run('tryRotate(); tryRotate(); tryRotate(); tryRotate();');
    await dropToBottom(g);

    const landing = g.calls.find(call => call.url === '/api/games/g1/landings');
    assert.deepEqual(landing.body, { index: 0, x: 4, y: 11, rotation: 0 });
    assert.equal(g.run('score'), 30);
    assert.equal(g.run('wordCount'), 1);
    assert.equal(g.run('landingIndex'), 1);
    assert.equal(g.run('grid.flat().filter(Boolean).length'), 0);
    assert.equal(g.element('score').textContent, 30);
    assert.equal(g.element('wordLog').children[0].textContent, 'CAT (名詞) - 猫');
});

test('rotation is reported to the server', async () => {
    const g = await started(() => reply(landingResult()));
    g.run('tryRotate();');
    assert.equal(g.run('current.rotation'), 1);
    g.run('current.y = ROWS - 3;');
    await g.run('softDrop()');
    assert.equal(g.calls.find(call => call.url.endsWith('/landings')).body.rotation, 1);
});

test('network failure restores the board and the retry resends the same landing', async () => {
    let fail = true;
    const g = await started(() => {
        if (fail) throw new Error('offline');
        return reply(landingResult({ score: 10 }));
    });
    await dropToBottom(g);
    assert.equal(g.run('grid.flat().filter(Boolean).length'), 0);
    assert.equal(g.run('communicationFailed && showCurrentPiece && !isProcessing'), true);
    assert.equal(g.element('retryConnectionBtn').hidden, false);
    assert.equal(g.run('landingIndex'), 0);

    fail = false;
    await g.element('retryConnectionBtn').listeners.click();
    const landings = g.calls.filter(call => call.url.endsWith('/landings'));
    assert.equal(landings.length, 2);
    assert.deepEqual(landings[0].body, landings[1].body);
    assert.equal(g.run('score'), 10);
    assert.equal(g.run('landingIndex'), 1);
});

test('a rejected landing cannot be retried and offers going back to the title', async () => {
    const g = await started(() => reply({ status: 409 }, 409));
    await dropToBottom(g);
    assert.equal(g.element('retryConnectionBtn').hidden, true);
    assert.equal(g.element('errorBackToTitleBtn').hidden, false);
    assert.match(g.element('connectionText').textContent, /続行できません/);
});

test('server side game over shows the result', async () => {
    const g = await started(() => reply(landingResult({
        life: 0, lifeLost: true, nextPiece: null, score: 120,
        result: { reason: 'GAME_OVER', score: 120, wordCount: 3, highScore: 120, newHighScore: true }
    })));
    await dropToBottom(g);
    assert.equal(g.run('gameOver'), true);
    assert.equal(g.element('resultTitle').textContent, 'ゲームオーバー');
    assert.equal(g.element('highScoreText').textContent, 'ハイスコア更新！');
    assert.equal(g.element('gameOverMessage').classList.contains('show'), true);
});

test('quitting from pause records the game as finished', async () => {
    const g = await started(() => reply(null, 204));
    await g.element('pauseBackToTitleBtn').listeners.click();
    assert.ok(g.calls.some(call => call.url === '/api/games/g1/quit'));
    assert.equal(g.run('gameOver'), true);
});

test('processing, pause and failed communication block movement and rotation', async () => {
    const g = await started(() => reply(landingResult()));
    for (const flag of ['isProcessing', 'paused', 'communicationFailed']) {
        g.run(`${flag}=true; moveHorizontal(1); tryRotate();`);
        assert.equal(g.run('current.x'), 4);
        assert.equal(g.run('current.rotation'), 0);
        g.run(`${flag}=false;`);
    }
});

test('too many starts shows a wait message and keeps the start button usable', async () => {
    const g = game(() => reply({ status: 429 }, 429));
    await g.run('startGame()');
    assert.match(g.element('titleStatus').textContent, /少し待って/);
    assert.equal(g.element('startBtn').disabled, false);
    assert.equal(g.run('gameStarted'), false);
});

test('lucky pieces from the server are announced and drawn in gold', async () => {
    const lucky = { ...PIECE, lucky: true };
    const g = game(url => (url === '/api/games'
        ? reply({ gameId: 'g1', maxLandings: 10000, life: 3, board: EMPTY_ROWS, piece: lucky })
        : reply(landingResult({ nextPiece: PIECE }))));
    await g.run('startGame()');
    assert.equal(g.run('current.lucky'), true);
    assert.equal(g.element('wordToast').textContent, 'ラッキーミノ！');

    g.element('wordToast').textContent = '';
    await dropToBottom(g);
    assert.equal(g.run('current.lucky'), false);
    assert.equal(g.element('wordToast').textContent, '');
});
