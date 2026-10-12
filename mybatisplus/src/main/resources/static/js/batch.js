/* ============ 批量上架 + 订单状态统计 ============ */

const CATEGORIES = ['山地车', '公路车', '折叠车', '城市车', '儿童车'];

function fillSample() {
    const lines = [];
    for (let i = 1; i <= 20; i++) {
        lines.push(`批量测试车-${i},MP 实验室,${CATEGORIES[i % CATEGORIES.length]},${1000 + i},${10 + i}`);
    }
    document.getElementById('batchText').value = lines.join('\n');
}

/** 把文本解析成商品数组 */
function parseBikes(text) {
    const bikes = [];
    text.split('\n').forEach(line => {
        const t = line.trim();
        if (!t) {
            return;
        }
        const parts = t.split(',').map(s => s.trim());
        if (parts.length < 5) {
            throw new Error('格式不对：' + line + '（需要 名称,品牌,分类,单价,库存）');
        }
        bikes.push({
            name: parts[0],
            brand: parts[1],
            category: parts[2],
            price: Number(parts[3]),
            stock: Number(parts[4]),
            status: 1,
            description: '批量上架：insertBatchSomeColumn'
        });
    });
    if (bikes.length === 0) {
        throw new Error('请至少填写一条商品');
    }
    return bikes;
}

async function submitBatch() {
    const msg = document.getElementById('batchMsg');
    const out = document.getElementById('batchResult');
    msg.textContent = '';
    try {
        const bikes = parseBikes(document.getElementById('batchText').value);
        out.textContent = `提交 ${bikes.length} 条，写入中…`;
        const data = await post('/api/bikes/batch', bikes);
        out.textContent = JSON.stringify(data, null, 2);
        msg.textContent = `成功写入 ${data.rows} 条，耗时 ${data.costMs} ms`;
        toast('批量上架成功');
    } catch (e) {
        out.textContent = '错误：' + e.message;
    }
}

async function loadStat() {
    const out = document.getElementById('statResult');
    try {
        const data = await get('/api/orders/stat');
        const STATUS_TEXT = {WAIT: '待发货', SENT: '已发货', DONE: '已完成', CANCEL: '已取消'};
        out.textContent = data.length
            ? data.map(r => `${STATUS_TEXT[r.status] || r.status}：${r.cnt} 单，合计 ${r.amount} 元`).join('\n')
            : '暂无订单';
    } catch (e) {
        out.textContent = '错误：' + e.message;
    }
}
