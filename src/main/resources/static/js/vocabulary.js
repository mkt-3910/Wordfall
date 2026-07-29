let currentPage = 0;
const PAGE_SIZE = 10;

async function loadWordLog(page) {
    try {
        const res = await fetch(`/api/word-log/list?page=${page}&size=${PAGE_SIZE}`);
        const data = await res.json();

        renderList(data.content);
        renderPagination(data);
    } catch (e) {
        console.error('単語帳の取得に失敗しました', e);
    }
}

function renderList(entries) {
    const wordList = document.getElementById('wordList');
    const emptyMessage = document.getElementById('emptyMessage');

    wordList.innerHTML = '';

    if (!entries || entries.length === 0) {
        emptyMessage.style.display = 'block';
        return;
    }
    emptyMessage.style.display = 'none';

    for (const entry of entries) {
        const card = document.createElement('div');
        card.className = 'word-card';

        const title = document.createElement('div');
        title.innerHTML = `<span class="word-title">${entry.word}</span>`
            + (entry.partOfSpeech ? `<span class="part-of-speech">${entry.partOfSpeech}</span>` : '');

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