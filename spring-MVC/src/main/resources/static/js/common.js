/* 公共请求工具：所有演示页面共用 */
const $ = id => document.getElementById(id);

/** 统一返回 {status, data}：data 优先按 JSON 解析，失败则原文 */
async function request(method, url, body, asJson = true) {
    const opts = { method: method, credentials: 'same-origin' };
    if (body !== undefined && body !== null) {
        if (body instanceof FormData) {
            opts.body = body;                 // 让浏览器自动带 boundary
        } else if (typeof body === 'string') {
            opts.headers = { 'Content-Type': asJson ? 'application/json' : 'application/x-www-form-urlencoded' };
            opts.body = body;
        } else {
            opts.headers = { 'Content-Type': 'application/json' };
            opts.body = JSON.stringify(body);
        }
    }
    const res = await fetch(url, opts);
    const text = await res.text();
    let data;
    try {
        data = JSON.parse(text);
    } catch (e) {
        data = text;
    }
    return { status: res.status, data: data };
}

/** 执行一次请求并把结果显示到 #out */
async function run(method, url, body, asJson = true) {
    const out = $('out');
    out.textContent = '请求中：' + method + ' ' + url + '\n';
    try {
        const r = await request(method, url, body, asJson);
        out.textContent = 'HTTP ' + r.status + '\n'
            + (typeof r.data === 'string' ? r.data : JSON.stringify(r.data, null, 2));
    } catch (e) {
        out.textContent = '请求失败：' + e.message;
    }
}

const get = (url) => run('GET', url);
const post = (url, body) => run('POST', url, body);
const put = (url, body) => run('PUT', url, body);
const del = (url) => run('DELETE', url);

/** 读取页面输入：$v('keyword') */
const $v = id => {
    const el = $(id);
    return el ? el.value : '';
};
