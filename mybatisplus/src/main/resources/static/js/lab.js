/* ============ MyBatis-Plus 实验室 ============ */

function show(outId, data) {
    document.getElementById(outId).textContent =
        typeof data === 'string' ? data : JSON.stringify(data, null, 2);
}

async function runApi(fn, outId, tip) {
    const el = document.getElementById(outId);
    el.textContent = '执行中…';
    try {
        const data = await fn();
        show(outId, data);
    } catch (e) {
        el.textContent = '错误：' + e.message + (tip ? '\n' + tip : '');
    }
}

/** 通用：GET/POST 一个实验室接口 */
async function run(path, outId, method) {
    await runApi(() => method === 'post' ? post('/api/lab/' + path) : get('/api/lab/' + path), outId);
}

/** ① 并发压测 */
async function runConcurrent() {
    const mode = document.getElementById('cnMode').value;
    const threads = document.getElementById('cnThreads').value || 20;
    const stock = document.getElementById('cnStock').value || 10;
    await runApi(() => post(`/api/lab/concurrent?bikeId=1&threads=${threads}&stock=${stock}&mode=${mode}`),
        'outConcurrent', '如果超时，把线程数调小');
}

/** ④ 数据权限 */
async function runDataPermission() {
    const userId = document.getElementById('dpUser').value;
    await runApi(() => get(`/api/lab/data-permission?userId=${userId}`), 'outDataPermission');
}

/** ⑤ 多租户 */
async function runTenant() {
    const name = encodeURIComponent(document.getElementById('tnName').value || '演示商品');
    await runApi(() => post(`/api/lab/tenant?name=${name}`), 'outTenant');
}

/** ⑥ 动态表名 */
async function runDynamicTable() {
    const table = encodeURIComponent(document.getElementById('dtTable').value || 't_order_2026');
    await runApi(() => post(`/api/lab/dynamic-table?table=${table}`), 'outDynamicTable');
}

/** ⑦ 批量插入 */
async function runBatch() {
    const n = document.getElementById('btCount').value || 1000;
    await runApi(() => get(`/api/lab/batch?n=${n}`), 'outBatch', '条数很大时会比较慢，耐心等');
}

/** ⑧ 分页 maxLimit */
async function runMaxLimit() {
    const size = document.getElementById('mlSize').value || 10000;
    await runApi(() => get(`/api/lab/max-limit?size=${size}`), 'outSafe');
}

/** 页面打开就加载插件清单 */
(async function init() {
    await runApi(() => get('/api/lab/plugins'), 'outPlugins');
})();
