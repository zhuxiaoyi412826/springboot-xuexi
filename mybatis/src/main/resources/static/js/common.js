/* ================= 全局 toast ================= */
if (!document.getElementById('toastBox')) {
    const box = document.createElement('div');
    box.id = 'toastBox';
    document.body.appendChild(box);
}
function toast(msg) {
    const el = document.createElement('div');
    el.className = 'toast-item';
    el.textContent = msg;
    document.getElementById('toastBox').appendChild(el);
    setTimeout(() => el.remove(), 2200);
}

/* ================= 请求封装 ================= */
async function request(url, options) {
    try {
        const res = await fetch(url, options);
        if (res.status === 401) {
            location.href = 'login.html';
            return null;
        }
        const data = await res.json();
        if (data.code !== 0) {
            toast(data.msg || '操作失败');
            return null;
        }
        return data.data;
    } catch (e) {
        toast('网络异常，请稍后再试');
        return null;
    }
}
const get = url => request(url, { method: 'GET', credentials: 'same-origin' });
const post = (url, body) => request(url, {
    method: 'POST', credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
});
const putJson = (url, body) => request(url, {
    method: 'PUT', credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
});
const putForm = url => request(url, { method: 'PUT', credentials: 'same-origin' });
const del = url => request(url, { method: 'DELETE', credentials: 'same-origin' });

/** 统一异常兜底 */
async function safe(fn) {
    try {
        await fn();
    } catch (e) {
        toast(e.message || '操作失败');
    }
}

/* ================= 工具 ================= */
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str).replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function formatNumber(n) {
    if (n === null || n === undefined) return '0';
    n = Number(n);
    if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿';
    if (n >= 10000) return (n / 10000).toFixed(1) + '万';
    return String(n);
}
function formatTime(t) {
    if (!t) return '-';
    return String(t).replace('T', ' ').substring(0, 16);
}
function formatSize(bytes) {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
    let i = 0, v = Number(bytes);
    while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
    return v.toFixed(1) + ' ' + units[i];
}
function getParam(name) {
    return new URLSearchParams(location.search).get(name);
}
function statusTag(status) {
    switch (status) {
        case 'FINISHED': return 'green';
        case 'SERIALIZING': return 'blue';
        case 'COMPLETED': return 'green';
        case 'DOWNLOADING': return 'blue';
        case 'QUEUED': return 'orange';
        case 'PAUSED': return 'orange';
        case 'CANCELLED': return 'gray';
        case 'FAILED': return 'red';
        default: return 'gray';
    }
}
function statusText(status) {
    const map = {
        FINISHED: '已完结', SERIALIZING: '连载中',
        QUEUED: '排队中', DOWNLOADING: '下载中', PAUSED: '已暂停',
        COMPLETED: '已完成', CANCELLED: '已停止', FAILED: '失败'
    };
    return map[status] || status || '-';
}

/* ================= 分页渲染 ================= */
function renderPager(el, pageResult, go) {
    if (!el) return;
    const p = pageResult.pageNum, pages = pageResult.pages;
    let html = `<button ${p <= 1 ? 'disabled' : ''} onclick="(${go})(${p - 1})">上一页</button>`;
    const start = Math.max(1, p - 2), end = Math.min(pages, start + 4);
    for (let i = start; i <= end; i++) {
        html += `<button class="${i === p ? 'active' : ''}" onclick="(${go})(${i})">${i}</button>`;
    }
    html += `<button ${p >= pages ? 'disabled' : ''} onclick="(${go})(${p + 1})">下一页</button>`;
    html += `<span>共 ${pageResult.total} 条 / ${pages} 页</span>`;
    el.innerHTML = html;
}

/* ================= 登录态 ================= */
let currentUser = null;
async function loadUser() {
    currentUser = await get('/api/auth/info');
    return currentUser;
}
async function requireLogin() {
    const u = await loadUser();
    if (!u) {
        location.href = 'login.html';
        return null;
    }
    return u;
}
async function requireAdmin() {
    const u = await loadUser();
    if (!u) { location.href = 'login.html'; return null; }
    if (u.role !== 'ADMIN') {
        toast('无权限，仅管理员可访问');
        setTimeout(() => location.href = 'index.html', 1200);
        return null;
    }
    return u;
}
function logout() {
    post('/api/auth/logout').then(() => location.href = 'login.html');
}

/** 顶部导航栏（统一渲染，避免每个页面重复写） */
function renderTopbar(activeName) {
    const links = { index: 'index.html', downloads: 'downloads.html', admin: 'admin.html' };
    const navs = currentUser && currentUser.role === 'ADMIN'
        ? [['index', '书城'], ['downloads', '我的下载'], ['admin', '后台管理']]
        : [['index', '书城'], ['downloads', '我的下载']];
    let html = `<div class="logo">阅 读 器 <small>MyBatis 学习项目</small></div><div class="nav">`;
    html += navs.map(([key, label]) =>
        `<a href="${links[key]}" class="${activeName === key ? 'active' : ''}">${label}</a>`).join('');
    if (currentUser) {
        html += `<span style="color:#999;font-size:13px">${escapeHtml(currentUser.nickname || currentUser.username)}</span>`;
        html += `<button onclick="logout()">退出</button>`;
    } else {
        html += `<a href="login.html" class="${activeName === 'login' ? 'active' : ''}">登录</a>`;
    }
    html += `</div>`;
    document.querySelector('.topbar .inner').innerHTML = html;
}
