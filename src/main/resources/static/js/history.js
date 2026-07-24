let currentPage = 0;
const PAGE_SIZE = 5;

// 指定したページのデータを取得して、画面に表示する
async function loadPage(page) {
    const res = await fetch(`/api/score/list?page=${page}&size=${PAGE_SIZE}`);
    const data = await res.json();

    currentPage = data.number;

    renderList(data.content);
    renderPager(data.number, data.totalPages);
}

// 1ページ分のデータを、カードとして描画する
function renderList(scores) {
    const listEl = document.getElementById('historyList');
    listEl.innerHTML = '';

    if (scores.length === 0) {
        listEl.innerHTML = `<p class="history-empty">まだプレイ履歴がありません</p>`;
        return;
    }

    for (const score of scores) {
        const card = document.createElement('div');
        card.className = 'history-card';

        const date = new Date(score.createdAt);
        const dateText = `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;

        const wordsText = score.words ? score.words.split(',').join(' ・ ') : '(なし)';

        card.innerHTML = `
            <div class="history-card-header">
                <span class="history-date">${dateText}</span>
                <span class="history-score">スコア ${score.score}</span>
            </div>
            <div class="history-wordcount">完成単語数: ${score.wordCount}</div>
            <div class="history-words">${wordsText}</div>
        `;
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
