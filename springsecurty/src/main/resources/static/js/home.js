/* 视频首页：列表 / 搜索 / 分页 */
(function () {
    let page = 1;
    const size = 12;

    document.addEventListener('DOMContentLoaded', () => {
        renderTopbar('home');
        $('btnSearch').addEventListener('click', () => { page = 1; load(); });
        $('btnHot').addEventListener('click', loadHot);
        $('btnPrev').addEventListener('click', () => { if (page > 1) { page--; load(); } });
        $('btnNext').addEventListener('click', () => { page++; load(); });
        $('keyword').addEventListener('keydown', e => { if (e.key === 'Enter') { page = 1; load(); } });
        load();
    });

    async function load() {
        const kw = $('keyword').value.trim();
        const cat = $('category').value;
        const sort = $('sort').value;
        const url = '/app/videos/list?page=' + page + '&size=' + size
            + '&keyword=' + encodeURIComponent(kw)
            + '&category=' + encodeURIComponent(cat)
            + '&sort=' + encodeURIComponent(sort);
        const r = await apiGet(url);
        log(r);
        if (r.status !== 200 || !r.data || r.data.code !== 0) {
            msg((r.data && r.data.msg) || '加载失败', 'err');
            return;
        }
        renderGrid(r.data.data.list);
        $('pageInfo').textContent = '第 ' + r.data.data.page + ' 页，共 ' + r.data.data.total + ' 条';
    }

    async function loadHot() {
        const r = await apiGet('/app/videos/hot');
        log(r);
        if (r.data && r.data.code === 0) {
            renderGrid(r.data.data);
            $('pageInfo').textContent = '热门 Top10';
        }
    }

    function renderGrid(list) {
        const grid = $('grid');
        if (!list || list.length === 0) {
            grid.innerHTML = '<div class="muted">没有数据</div>';
            return;
        }
        grid.innerHTML = list.map((v, i) => {
            const cls = 'cover c' + (i % 4 + 1);
            return '<div class="card" data-id="' + v.id + '">'
                + '<div class="' + cls + '">' + escapeHtml(String(v.title).substring(0, 1)) + '</div>'
                + '<div class="body">'
                + '<div class="title">' + escapeHtml(v.title) + '</div>'
                + '<div class="meta">' + escapeHtml(v.category || '') + ' · '
                + (v.viewCount || 0) + ' 次播放 · ' + (v.likeCount || 0) + ' 赞</div>'
                + '<div style="margin-top:6px">'
                + '<span class="tag status-' + escapeHtml(v.status) + '">' + escapeHtml(v.status) + '</span>'
                + (v.favorited ? '<span class="tag">已收藏</span>' : '')
                + (v.canEdit ? '<span class="tag">可编辑</span>' : '')
                + '</div>'
                + '</div></div>';
        }).join('');

        grid.querySelectorAll('.card').forEach(card => {
            card.addEventListener('click', () => {
                location.href = 'detail.html?id=' + card.getAttribute('data-id');
            });
        });
    }
})();
