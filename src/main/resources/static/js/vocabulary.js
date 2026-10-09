let currentPage = 0;
let latestRequest = 0;
const PAGE_SIZE = 10;

async function loadWordLog(page) {
    const requestId = ++latestRequest;
    try {
        const res = await fetch(`/api/word-log/list?page=${page}&size=${PAGE_SIZE}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        if (requestId !== latestRequest) return;

        renderList(data.content);
        renderPagination(data);
    } catch (e) {
        if (requestId !== latestRequest) return;
        console.error('単語帳の取得に失敗しました', e);
        document.getElementById('wordList').replaceChildren();
        const emptyMessage = document.getElementById('emptyMessage');
        emptyMessage.textContent = '単語帳を読み込めませんでした';
        emptyMessage.hidden = false;
    }
}

function renderList(entries) {
    const wordList = document.getElementById('wordList');
    const emptyMessage = document.getElementById('emptyMessage');

    wordList.replaceChildren();

    if (!entries || entries.length === 0) {
        emptyMessage.textContent = 'まだ完成した単語がありません';
        emptyMessage.hidden = false;
        return;
    }
    emptyMessage.hidden = true;

    for (const entry of entries) {
        const card = document.createElement('div');
        card.className = 'word-card';

        const title = document.createElement('div');
        const word = document.createElement('span');
        word.className = 'word-title';
        word.textContent = entry.word ?? '';
        title.appendChild(word);
        if (entry.partOfSpeech) {
            const partOfSpeech = document.createElement('span');
            partOfSpeech.className = 'part-of-speech';
            partOfSpeech.textContent = entry.partOfSpeech;
            title.appendChild(partOfSpeech);
        }

        const meaning = document.createElement('div');
        meaning.className = 'meaning';
        meaning.textContent = entry.meaning ?? '';

        card.appendChild(title);
        card.appendChild(meaning);
        wordList.appendChild(card);
    }
}

function renderPagination(data) {
    currentPage = data.number;
    const totalPages = data.totalPages;

    document.getElementById('pageInfo').textContent =
        totalPages === 0 ? '' : `${currentPage + 1} / ${totalPages}`;

    document.getElementById('prevBtn').disabled = data.first;
    document.getElementById('nextBtn').disabled = data.last;
}

document.getElementById('prevBtn').addEventListener('click', () => {
    if (currentPage > 0) loadWordLog(currentPage - 1);
});

document.getElementById('nextBtn').addEventListener('click', () => {
    loadWordLog(currentPage + 1);
});

document.getElementById('backBtn').addEventListener('click', () => {
    location.href = '/';
});

loadWordLog(0);
