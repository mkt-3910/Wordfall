async function loadAchievements() {
    const list = document.getElementById('achievementList');
    const summary = document.getElementById('achievementSummary');

    try {
        const response = await fetch('/api/achievements');
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const achievements = await response.json();
        const unlockedCount = achievements.filter(item => item.unlocked).length;
        summary.textContent = `${unlockedCount} / ${achievements.length} 解除`;
        list.replaceChildren(...achievements.map(createAchievementCard));
    } catch (error) {
        console.error('実績の取得に失敗しました', error);
        summary.textContent = '';
        list.textContent = '実績を読み込めませんでした';
    }
}

function createAchievementCard(achievement) {
    const card = document.createElement('article');
    card.className = `achievement-card ${achievement.unlocked ? 'unlocked' : 'locked'}`;

    const icon = document.createElement('span');
    icon.className = 'achievement-icon';
    icon.textContent = achievement.unlocked ? '🏆' : '🔒';

    const content = document.createElement('div');
    const title = document.createElement('h2');
    title.textContent = achievement.title;
    const description = document.createElement('p');
    description.textContent = achievement.description;
    content.append(title, description);

    card.append(icon, content);
    return card;
}

document.getElementById('backBtn').addEventListener('click', () => {
    location.href = '/';
});

void loadAchievements();
