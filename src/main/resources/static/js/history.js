let currentPage = 0;
const PAGE_SIZE = 5;

// 指定したページのデータを取得して、画面に表示する
async function loadPage(page) {
    try {
        const res = await fetch(`/api/score/list?page=${page}&size=${PAGE_SIZE}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();

        currentPage = data.number;

        renderList(data.content);
        renderPager(data.number, data.totalPages);
    } catch (error) {
        console.error('履歴の取得に失敗しました', error);
        const listEl = document.getElementById('historyList');
        listEl.replaceChildren();
        const message = document.createElement('p');
        message.className = 'history-empty';
        message.textContent = '履歴を読み込めませんでした';
        listEl.appendChild(message);
    }
}

// 1ページ分のデータを、カードとして描画する
function renderList(scores) {
    const listEl = document.getElementById('historyList');
    listEl.replaceChildren();

    if (scores.length === 0) {
        const message = document.createElement('p');
        message.className = 'history-empty';
        message.textContent = 'まだプレイ履歴がありません';
        listEl.appendChild(message);
        return;
    }

    for (const score of scores) {
        const card = document.createElement('div');
        card.className = 'history-card';

        const date = new Date(score.createdAt);
        const dateText = `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;

        const wordsText = score.words ? score.words.split(',').join(' ・ ') : '(なし)';

        const header = document.createElement('div');
        header.className = 'history-card-header';

        const dateEl = document.createElement('span');
        dateEl.className = 'history-date';
        dateEl.textContent = dateText;

        const scoreEl = document.createElement('span');
        scoreEl.className = 'history-score';
        scoreEl.textContent = `スコア ${score.score}`;

        const countEl = document.createElement('div');
        countEl.className = 'history-wordcount';
        countEl.textContent = `完成単語数: ${score.wordCount}`;

        const wordsEl = document.createElement('div');
        wordsEl.className = 'history-words';
        wordsEl.textContent = wordsText;

        header.append(dateEl, scoreEl);
        card.append(header, countEl, wordsEl);
        listEl.appendChild(card);
    }
}

// ページ送りのボタン・表示を更新する
function renderPager(pageNumber, totalPages) {
    document.getElementById('pageInfo').textContent =
        totalPages === 0 ? '0 / 0' : `${pageNumber + 1} / ${totalPages}`;
     document.getElementById('prevBtn').disabled = (pageNumber <= 0);
    document.getElementById('nextBtn').disabled = (pageNumber >= totalPages - 1);
}

document.getElementById('prevBtn').addEventListener('click', () => {
    if (currentPage > 0) loadPage(currentPage - 1);
});

document.getElementById('nextBtn').addEventListener('click', () => {
    loadPage(currentPage + 1);
});

document.getElementById('backBtn').addEventListener('click', () => {
    location.href = '/';
});

loadPage(0);
