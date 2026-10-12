/**
 * 公共 JS：token 管理、统一请求、顶部导航、播放器。
 * Sa-Token 的 token 放在请求头 satoken 里（token-name 在 application.yaml 配置）。
 */
const TOKEN_KEY = 'music_token';
const USER_KEY = 'music_user';

function getToken() { return localStorage.getItem(TOKEN_KEY); }
function setToken(t) { localStorage.setItem(TOKEN_KEY, t); }
function getUser() { const s = localStorage.getItem(USER_KEY); return s ? JSON.parse(s) : null; }
function setUser(u) { localStorage.setItem(USER_KEY, JSON.stringify(u)); }
function clearAuth() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); }

/** 统一请求：自动带 token，统一解析 {code,data,msg} */
async function api(url, options = {}) {
    const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
    const token = getToken();
    if (token) headers['satoken'] = token;
    const res = await fetch(url, Object.assign({}, options, { headers }));
    if (res.status === 401) {
        clearAuth();
        location.href = '/login.html?redirect=' + encodeURIComponent(location.pathname);
        return null;
    }
    let json;
    try { json = await res.json(); } catch (e) { throw new Error('响应不是 JSON'); }
    if (json.code !== 200) throw new Error(json.msg || ('请求失败(' + json.code + ')'));
    return json.data;
}

function toast(msg, isError) {
    const el = document.createElement('div');
    el.className = 'toast' + (isError ? ' error' : '');
    el.innerText = msg;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 2500);
}

function fmtDuration(sec) {
    if (!sec) return '00:00';
    const m = Math.floor(sec / 60), s = sec % 60;
    return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
}
function fmtCount(n) {
    if (n === null || n === undefined) return '0';
    if (n >= 10000) return (n / 10000).toFixed(1) + '万';
    return String(n);
}
function escapeHtml(s) {
    return (s || '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** 顶部导航（用户端页面共用） */
function renderNav(active) {
    const user = getUser();
    const links = [
        ['/user/index.html', '发现音乐', 'index'],
        ['/user/rank.html', '排行榜', 'rank'],
        ['/user/search.html', '搜索', 'search'],
        ['/user/me.html', '我的', 'me']
    ];
    let html = '<div class="navbar">' +
        '<a class="logo" href="/user/index.html">🎵 Music Redis</a>' +
        '<div class="nav-links">' +
        links.map(l => '<a href="' + l[0] + '"' + (active === l[2] ? ' style="color:var(--primary)"' : '') + '>' + l[1] + '</a>').join('') +
        (user && user.role === 'admin' ? '<a href="/admin/index.html" style="color:var(--warn)">管理后台</a>' : '') +
        '</div><div class="nav-right">';
    html += user ? ('<span class="badge' + (user.role === 'admin' ? '' : ' gray') + '">' + escapeHtml(user.role) + '</span>' +
        '<span>' + escapeHtml(user.nickname || user.username) + '</span>' +
        '<button class="ghost tiny" onclick="doLogout()">退出</button>')
        : '<a href="/login.html">登录</a>';
    html += '</div></div>';
    document.body.insertAdjacentHTML('afterbegin', html);
}

async function doLogout() {
    try { await api('/api/auth/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
    clearAuth();
    location.href = '/login.html';
}

/* ---------------- 播放器 ---------------- */
const Player = {
    el: null,
    current: null,
    init() {
        if (this.el) return;
        document.body.classList.add('has-player');
        const div = document.createElement('div');
        div.className = 'player';
        div.innerHTML =
            '<img id="p-cover" src="" alt="">' +
            '<div class="info"><div id="p-title" class="small">还没有播放</div>' +
            '<div id="p-singer" class="small muted">选一首歌开始吧</div></div>' +
            '<audio id="p-audio" controls preload="none"></audio>' +
            '<button class="ghost tiny" id="p-like">♡ 点赞</button>' +
            '<button class="ghost tiny" id="p-fav">☆ 收藏</button>';
        document.body.appendChild(div);
        this.el = div;
        const audio = document.getElementById('p-audio');
        audio.addEventListener('play', () => { if (this.current) this.reportPlay(this.current.id); });
        document.getElementById('p-like').onclick = () => this.toggleLike();
        document.getElementById('p-fav').onclick = () => this.toggleFav();
    },
    /** 播放歌曲（song 为 SongVO） */
    play(song) {
        this.init();
        this.current = song;
        document.getElementById('p-cover').src = song.coverUrl || '';
        document.getElementById('p-title').innerText = song.title;
        document.getElementById('p-singer').innerText = song.singer;
        const audio = document.getElementById('p-audio');
        audio.src = song.audioUrl;
        audio.play().catch(() => toast('浏览器阻止了自动播放，请点一下播放按钮'));
        this.reportPlay(song.id);
    },
    async reportPlay(id) {
        try { await api('/api/songs/' + id + '/play', { method: 'POST' }); } catch (e) { /* 未登录也能听 */ }
    },
    async toggleLike() {
        if (!this.current) return;
        try {
            const d = await api('/api/songs/' + this.current.id + '/like', { method: 'POST' });
            document.getElementById('p-like').innerText = d.liked ? '♥ 已点赞' : '♡ 点赞';
            toast(d.liked ? '已点赞' : '已取消点赞');
        } catch (e) { toast(e.message, true); }
    },
    async toggleFav() {
        if (!this.current) return;
        try {
            const d = await api('/api/songs/' + this.current.id + '/favorite', { method: 'POST' });
            document.getElementById('p-fav').innerText = d.favorited ? '★ 已收藏' : '☆ 收藏';
            toast(d.favorited ? '已收藏' : '已取消收藏');
        } catch (e) { toast(e.message, true); }
    }
};

function songCardHtml(s) {
    return '<div class="song-card" onclick=\'Player.play(' + JSON.stringify(s).replace(/'/g, "&apos;") + ')\'>' +
        '<img src="' + (s.coverUrl || '') + '" alt="" onerror="this.style.visibility=\'hidden\'">' +
        '<div class="title">' + escapeHtml(s.title) + '</div>' +
        '<div class="singer">' + escapeHtml(s.singer) + '</div>' +
        '<div class="small muted">▶ ' + fmtCount(s.playCount) + ' · ' + fmtDuration(s.duration) + '</div>' +
        '</div>';
}
