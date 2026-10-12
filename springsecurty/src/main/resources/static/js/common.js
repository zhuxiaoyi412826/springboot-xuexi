/* 公共工具：请求封装（自动带 CSRF Token）、登录态、提示 */
const $ = id => document.getElementById(id);

function getCookie(name) {
    const m = document.cookie.match(new RegExp('(^|;\\s*)' + name + '=([^;]*)'));
    return m ? decodeURIComponent(m[2]) : null;
}

/** 取 CSRF Token：先读 XSRF-TOKEN cookie，没有就请求 /app/auth/csrf 让服务端下发 */
async function csrfToken() {
    let token = getCookie('XSRF-TOKEN');
    if (token) return token;
    const res = await fetch('/app/auth/csrf', {
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
        credentials: 'same-origin'
    });
    const json = await res.json();
    token = getCookie('XSRF-TOKEN') || (json.data && json.data.token);
    return token;
}

/**
 * 统一请求。返回 {status, data, msg}
 * 非 GET 请求自动带 X-XSRF-TOKEN 头（CSRF 防护）
 */
async function api(method, url, body) {
    const headers = { 'X-Requested-With': 'XMLHttpRequest' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (method !== 'GET' && method !== 'HEAD') {
        const t = await csrfToken();
        if (t) headers['X-XSRF-TOKEN'] = t;
    }
    const res = await fetch(url, {
        method: method,
        headers: headers,
        credentials: 'same-origin',
        body: body === undefined ? undefined : JSON.stringify(body)
    });
    let data = null;
    const text = await res.text();
    try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }
    return { status: res.status, data: data, msg: data && data.msg ? data.msg : '' };
}

const apiGet = url => api('GET', url);
const apiPost = (url, body) => api('POST', url, body === undefined ? {} : body);
const apiPut = (url, body) => api('PUT', url, body);
const apiDel = url => api('DELETE', url);

function escapeHtml(s) {
    if (s === null || s === undefined) return '';
    return String(s).replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[c]);
}

function fmtTime(t) {
    if (!t) return '';
    return String(t).replace('T', ' ').substring(0, 19);
}

/** 显示提示（tip / ok / err） */
function msg(text, type) {
    const box = $('msg');
    if (!box) return;
    box.className = type || 'tip';
    box.innerHTML = escapeHtml(text);
}

/** 在 #log 里打印原始响应 */
function log(obj) {
    const el = $('log');
    if (!el) return;
    el.textContent = typeof obj === 'string' ? obj : JSON.stringify(obj, null, 2);
}

/** 当前登录态（带缓存，登录/退出后调用 clearState()） */
let _state = null;
async function authState(force) {
    if (_state && !force) return _state;
    const r = await apiGet('/app/auth/state');
    _state = (r.data && r.data.data) || {};
    return _state;
}
function clearState() { _state = null; }

/** 渲染顶部导航：区分「用户端」与「管理端」 */
async function renderTopbar(activePage) {
    const bar = $('topbar');
    if (!bar) return;
    const s = await authState(true);
    const links = [
        { key: 'home', href: '/user/home.html', text: '视频首页' },
        { key: 'me', href: '/user/me.html', text: '个人中心' }
    ];
    if (s.roles && s.roles.indexOf('ROLE_ADMIN') >= 0) {
        links.push({ key: 'admin', href: '/admin/index.html', text: '管理后台' });
    }
    let html = '<span class="logo">视频小站 <small>Spring Security 演示</small></span>';
    links.forEach(l => {
        html += '<a href="' + l.href + '"' + (l.key === activePage ? ' style="color:#fff;font-weight:bold"' : '') + '>' + l.text + '</a>';
    });
    html += '<span class="spacer"></span>';
    if (s.authenticated) {
        html += '<span>你好，' + escapeHtml(s.nickname || s.username) + '</span>';
        if (s.roles) {
            html += '<span class="tag">' + s.roles.join(' ') + '</span>';
        }
        html += '<button class="gray" id="btnLogout">退出登录</button>';
    } else {
        html += '<span class="muted">未登录（匿名可浏览）</span>';
        html += '<a href="/user/login.html">登录</a><a href="/user/register.html">注册</a>';
    }
    bar.innerHTML = html;

    const btn = $('btnLogout');
    if (btn) {
        btn.addEventListener('click', doLogout);
    }
}

async function doLogout() {
    await apiPost('/logout');
    clearState();
    location.href = '/user/login.html?logout=1';
}

/** 表单登录（提交到 Spring Security 的 /login） */
async function doLogin(username, password, rememberMe) {
    const token = await csrfToken();
    let body = 'username=' + encodeURIComponent(username) + '&password=' + encodeURIComponent(password);
    if (rememberMe) body += '&remember-me=on';
    const res = await fetch('/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest',
            'X-XSRF-TOKEN': token || ''
        },
        credentials: 'same-origin',
        body: body
    });
    let data = null;
    try { data = await res.json(); } catch (e) { data = null; }
    return { status: res.status, data: data };
}

function param(name) {
    return new URLSearchParams(location.search).get(name);
}

/** 需要登录才能做的操作：未登录给出提示并跳登录页 */
async function requireLogin() {
    const s = await authState();
    if (!s.authenticated) {
        msg('请先登录后再操作', 'err');
        setTimeout(() => location.href = '/user/login.html', 800);
        return false;
    }
    return true;
}
