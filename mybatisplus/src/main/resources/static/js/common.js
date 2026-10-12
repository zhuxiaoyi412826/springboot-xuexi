/* ============ 公共 JS：请求封装、提示、分页渲染 ============ */

/** 封装 fetch，自动解析统一返回结构 Result{code,msg,data} */
async function request(url, options = {}) {
    const res = await fetch(url, {
        credentials: 'same-origin',
        headers: {'Content-Type': 'application/json'},
        ...options
    });
    let body;
    try {
        body = await res.json();
    } catch (e) {
        throw new Error('服务器返回格式错误');
    }
    if (body.code !== 0) {
        throw new Error(body.msg || '操作失败');
    }
    return body.data;
}

const get = (url) => request(url);
const post = (url, data) => request(url, {method: 'POST', body: JSON.stringify(data || {})});
const put = (url, data) => request(url, {method: 'PUT', body: JSON.stringify(data || {})});
const del = (url) => request(url, {method: 'DELETE'});

/** 轻提示 */
function toast(msg) {
    let el = document.querySelector('.toast');
    if (!el) {
        el = document.createElement('div');
        el.className = 'toast';
        document.body.appendChild(el);
    }
    el.textContent = msg;
    el.style.display = 'block';
    clearTimeout(el._t);
    el._t = setTimeout(() => (el.style.display = 'none'), 2000);
}

/** 安全调用：失败自动提示 */
async function safe(fn) {
    try {
        return await fn();
    } catch (e) {
        toast(e.message);
    }
}

/** 渲染分页条 */
function renderPager(pagerEl, page, onPageChange) {
    const {current, pages, total} = page;
    pagerEl.innerHTML = `
        <span>共 ${total} 条 / ${pages} 页</span>
        <button class="btn small gray" ${current <= 1 ? 'disabled' : ''} data-p="${current - 1}">上一页</button>
        <span>第 ${current} 页</span>
        <button class="btn small gray" ${current >= pages ? 'disabled' : ''} data-p="${current + 1}">下一页</button>`;
    pagerEl.querySelectorAll('button[data-p]').forEach(btn => {
        btn.onclick = () => onPageChange(Number(btn.dataset.p));
    });
}

/** HTML 转义，防止 XSS */
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str).replace(/[&<>"']/g, c => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[c]));
}

/** 金额格式化 */
function money(v) {
    return '￥' + Number(v || 0).toFixed(2);
}

/** 退出登录 */
async function logout() {
    await post('/api/auth/logout').catch(() => {});
    location.href = '/login.html';
}
