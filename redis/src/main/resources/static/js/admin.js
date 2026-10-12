/** 管理端脚本 */
const PANES = ['overview', 'songs', 'users', 'comments', 'playlists', 'cache', 'lab'];

function show(name) {
    PANES.forEach(p => {
        document.getElementById('pane-' + p).classList.toggle('hidden', p !== name);
        document.getElementById('tab-' + p).classList.toggle('active', p === name);
    });
    if (name === 'songs') loadSongs();
    if (name === 'users') loadUsers();
    if (name === 'comments') loadComments();
    if (name === 'playlists') loadPlaylists();
    if (name === 'cache') loadOverview();
}

function v(id) { return encodeURIComponent(document.getElementById(id).value); }

async function lab(url) {
    try {
        const data = await api(url, { method: url.indexOf('stream/add') > 0 ? 'POST' : 'GET' });
        document.getElementById('lab-out').innerText = JSON.stringify(data, null, 2);
    } catch (e) {
        document.getElementById('lab-out').innerText = '错误：' + e.message;
    }
}

async function addStream() {
    try {
        const data = await fetch('/api/lab/stream/add?k=play', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ songId: '1', userId: '1', ts: String(Date.now()) })
        }).then(r => r.json());
        document.getElementById('lab-out').innerText = JSON.stringify(data, null, 2);
    } catch (e) { toast(e.message, true); }
}

// ==================== 概览 / 缓存 ====================
async function loadOverview() {
    try {
        const o = await api('/api/admin/cache/overview');
        const cards = [
            ['缓存总命中率', o.cacheStats._summary ? o.cacheStats._summary.hitRate : '-'],
            ['缓存请求总数', o.cacheStats._summary ? o.cacheStats._summary.totalRequests : 0],
            ['L1(Caffeine) 命中率', o.l1.hitRate],
            ['Redis 命中率', o.redis.hitRate || '-'],
            ['Redis key 数量', o.redis.dbSize || 0],
            ['Redis 内存', o.redis.used_memory_human || '-'],
            ['Redis 版本', o.redis.redis_version || '-']
        ];
        document.getElementById('stat-cards').innerHTML = cards.map(c =>
            '<div class="stat-card"><div class="num">' + c[1] + '</div><div class="label">' + c[0] + '</div></div>').join('');
        document.getElementById('overview-json').innerText = JSON.stringify(o, null, 2);
    } catch (e) { toast(e.message, true); }

    try {
        const notices = await api('/api/admin/cache/notices');
        document.getElementById('notices').innerText = notices.length ? notices.join('\n') : '（暂无通知，去 Redis 实验室点 PUBLISH）';
    } catch (e) { /* ignore */ }
}

async function clearCache() {
    const name = document.getElementById('cache-name').value;
    const n = await api('/api/admin/cache/clear?name=' + name, { method: 'POST' });
    toast('已清理 ' + n + ' 个 key');
    loadOverview();
}
async function warm() { await api('/api/admin/cache/warm', { method: 'POST' }); toast('预热完成'); loadOverview(); }
async function resetStats() { await api('/api/admin/cache/reset-stats', { method: 'POST' }); toast('统计已重置'); loadOverview(); }
async function setLevel(level, enabled) {
    await api('/api/admin/cache/level?level=' + level + '&enabled=' + enabled, { method: 'POST' });
    toast(level + ' = ' + enabled);
    loadOverview();
}
async function scanKeys() {
    const keys = await api('/api/admin/cache/keys?pattern=' + v('key-pattern') + '&count=100');
    document.getElementById('keys-json').innerText = JSON.stringify(keys, null, 2);
}

// ==================== 歌曲 ====================
async function loadSongs() {
    try {
        const data = await api('/api/admin/songs/list?page=1&size=50');
        document.getElementById('song-rows').innerHTML = (data.list || []).map(s =>
            '<tr><td>' + s.id + '</td><td>' + escapeHtml(s.title) + '</td><td>' + escapeHtml(s.singer) + '</td>' +
            '<td>' + escapeHtml(s.category || '') + '</td><td>' + fmtCount(s.playCount) + '</td>' +
            '<td>' + (s.status === 1 ? '<span class="badge">上架</span>' : '<span class="badge gray">下架</span>') + '</td>' +
            '<td><button class="ghost tiny" onclick="toggleStatus(' + s.id + ',' + s.status + ')">' +
            (s.status === 1 ? '下架' : '上架') + '</button> ' +
            '<button class="ghost tiny" onclick="delayOnline(' + s.id + ')">30 秒后自动上架</button></td></tr>').join('');
    } catch (e) { toast(e.message, true); }
}

async function addSong() {
    const body = {
        title: document.getElementById('s-title').value,
        singer: document.getElementById('s-singer').value,
        audioUrl: document.getElementById('s-audio').value,
        category: document.getElementById('s-category').value,
        duration: Number(document.getElementById('s-duration').value || 240),
        status: 1
    };
    if (!body.title || !body.audioUrl) { toast('歌名和音频地址必填', true); return; }
    await api('/api/admin/songs/add', { method: 'POST', body: JSON.stringify(body) });
    toast('新增成功（已同步进布隆过滤器）');
    loadSongs();
}

async function toggleStatus(id, status) {
    await api('/api/admin/songs/' + id + '/status?status=' + (status === 1 ? 0 : 1), { method: 'POST' });
    loadSongs();
}

async function delayOnline(id) {
    await api('/api/lab/delay/add?payload=song:' + id + ':online&seconds=30');
    toast('已投递：30 秒后自动上架（观察后台日志）');
}

// ==================== 用户 ====================
async function loadUsers() {
    try {
        const data = await api('/api/admin/users/list?page=1&size=50&keyword=' + v('u-keyword'));
        document.getElementById('user-rows').innerHTML = (data.list || []).map(u =>
            '<tr><td>' + u.id + '</td><td>' + escapeHtml(u.username) + '</td><td>' + escapeHtml(u.nickname || '') + '</td>' +
            '<td><span class="badge' + (u.role === 'admin' ? '' : ' gray') + '">' + u.role + '</span></td>' +
            '<td>' + (u.status === 1 ? '正常' : '<span class="badge red">禁用</span>') + '</td>' +
            '<td><button class="ghost tiny" onclick="setUserStatus(' + u.id + ',' + u.status + ')">' + (u.status === 1 ? '禁用' : '启用') + '</button> ' +
            '<button class="ghost tiny" onclick="kick(' + u.id + ')">踢下线</button> ' +
            '<button class="ghost tiny" onclick="ban(' + u.id + ')">封禁10分钟</button></td></tr>').join('');
        const online = await api('/api/admin/users/online');
        document.getElementById('online-info').innerText = '在线用户 ' + online.count + ' 人：' + (online.userIds || []).join('、');
    } catch (e) { toast(e.message, true); }
}

async function setUserStatus(id, status) {
    await api('/api/admin/users/' + id + '/status?status=' + (status === 1 ? 0 : 1), { method: 'POST' });
    loadUsers();
}
async function kick(id) { await api('/api/admin/users/' + id + '/kickout', { method: 'POST' }); toast('已踢下线'); loadUsers(); }
async function ban(id) { await api('/api/admin/users/' + id + '/ban?seconds=600', { method: 'POST' }); toast('已封禁'); loadUsers(); }

// ==================== 评论 ====================
async function loadComments() {
    try {
        const data = await api('/api/admin/comments/list?page=1&size=50&keyword=' + v('c-keyword'));
        document.getElementById('comment-rows').innerHTML = (data.list || []).map(c =>
            '<tr><td>' + c.id + '</td><td>' + c.songId + '</td><td>' + escapeHtml(c.nickname || '') + '</td>' +
            '<td>' + escapeHtml(c.content) + '</td>' +
            '<td>' + (c.status === 1 ? '正常' : '<span class="badge red">已屏蔽</span>') + '</td>' +
            '<td><button class="ghost tiny" onclick="shield(' + c.id + ',' + c.status + ')">' + (c.status === 1 ? '屏蔽' : '放开') + '</button> ' +
            '<button class="ghost tiny" onclick="delComment(' + c.id + ')">删除</button></td></tr>').join('');
    } catch (e) { toast(e.message, true); }
}
async function shield(id, status) {
    await api('/api/admin/comments/' + id + '/shield?status=' + (status === 1 ? 0 : 1), { method: 'POST' });
    loadComments();
}
async function delComment(id) {
    await api('/api/admin/comments/' + id + '/delete', { method: 'POST' });
    loadComments();
}

// ==================== 歌单审核 ====================
async function loadPlaylists() {
    try {
        const queue = await api('/api/admin/playlists/audit-queue');
        document.getElementById('audit-queue').innerText = '待审核队列（Redis List）：' + (queue.length ? queue.join('、') : '空');
        const data = await api('/api/admin/playlists/list?page=1&size=50');
        document.getElementById('playlist-rows').innerHTML = (data.list || []).map(p =>
            '<tr><td>' + p.id + '</td><td>' + escapeHtml(p.name) + '</td><td>' + p.userId + '</td>' +
            '<td>' + (p.status === 1 ? '公开' : p.status === 2 ? '待审核' : '私有') + '</td>' +
            '<td><button class="ghost tiny" onclick="audit(' + p.id + ',1)">通过</button> ' +
            '<button class="ghost tiny" onclick="audit(' + p.id + ',0)">拒绝</button></td></tr>').join('');
    } catch (e) { toast(e.message, true); }
}
async function audit(id, status) {
    await api('/api/admin/playlists/' + id + '/audit?status=' + status, { method: 'POST' });
    loadPlaylists();
}

// 启动：没有 token 直接回登录页
if (!getToken()) {
    location.href = '/login.html?redirect=/admin/index.html';
} else {
    show('overview');
}
