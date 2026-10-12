/* 视频详情：播放、点赞、收藏、VIP 下载、评论（对象级权限） */
(function () {
    let videoId = param('id');

    document.addEventListener('DOMContentLoaded', () => {
        renderTopbar('home');
        $('btnLike').addEventListener('click', like);
        $('btnFav').addEventListener('click', favorite);
        $('btnDownload').addEventListener('click', download);
        $('btnComment').addEventListener('click', addComment);
        load();
    });

    async function load() {
        if (!videoId) {
            msg('缺少视频 id', 'err');
            return;
        }
        const r = await apiGet('/app/videos/' + videoId);
        log(r);
        if (r.status !== 200 || !r.data || r.data.code !== 0) {
            msg((r.data && r.data.msg) || '加载失败', 'err');
            return;
        }
        const v = r.data.data;
        $('title').textContent = v.title;
        $('desc').textContent = v.description || '';
        $('meta').textContent = (v.category || '') + ' · ' + (v.viewCount || 0) + ' 次播放 · '
            + (v.likeCount || 0) + ' 赞 · 作者：' + (v.authorName || '') + ' · 状态：' + v.status;
        const player = $('player');
        player.src = v.videoUrl;
        $('btnFav').textContent = v.favorited ? '取消收藏' : '收藏';
        loadComments();
    }

    async function like() {
        if (!await requireLogin()) return;
        const r = await apiPost('/app/videos/' + videoId + '/like');
        log(r);
        msg((r.data && r.data.msg) || '点赞完成', r.status === 200 ? 'ok' : 'err');
        load();
    }

    async function favorite() {
        if (!await requireLogin()) return;
        const r = await apiPost('/app/videos/' + videoId + '/favorite');
        log(r);
        if (r.data && r.data.code === 0) {
            msg(r.data.data.favorited ? '已收藏' : '已取消收藏', 'ok');
            $('btnFav').textContent = r.data.data.favorited ? '取消收藏' : '收藏';
        } else {
            msg((r.data && r.data.msg) || '操作失败', 'err');
        }
    }

    async function download() {
        const r = await apiPost('/app/videos/' + videoId + '/download');
        log(r);
        // 非 VIP 会收到 403 JSON（URL 级权限 hasAuthority('video:download')）
        msg((r.data && r.data.msg) || ('下载链接：' + (r.data && r.data.data ? r.data.data.url : '')),
            r.status === 200 ? 'ok' : 'err');
    }

    async function loadComments() {
        const r = await apiGet('/app/comments/list?videoId=' + videoId);
        const table = $('comments');
        if (!r.data || r.data.code !== 0) {
            table.innerHTML = '<tr><th>用户</th><th>内容</th><th>时间</th><th>操作</th></tr>';
            return;
        }
        const rows = r.data.data.map(c =>
            '<tr><td>' + escapeHtml(c.username) + '</td><td>' + escapeHtml(c.content) + '</td>'
            + '<td>' + fmtTime(c.createTime) + '</td>'
            + '<td>' + (c.canDelete ? '<button class="red" data-id="' + c.id + '">删除</button>' : '<span class="muted">-</span>')
            + '</td></tr>').join('');
        table.innerHTML = '<tr><th>用户</th><th>内容</th><th>时间</th><th>操作</th></tr>' + rows;
        table.querySelectorAll('button[data-id]').forEach(btn => {
            btn.addEventListener('click', () => removeComment(btn.getAttribute('data-id')));
        });
    }

    async function addComment() {
        if (!await requireLogin()) return;
        const content = $('comment').value.trim();
        if (!content) {
            msg('请输入评论内容', 'err');
            return;
        }
        const r = await apiPost('/app/comments/add', { videoId: Number(videoId), content: content });
        log(r);
        if (r.data && r.data.code === 0) {
            $('comment').value = '';
            msg('评论已发表', 'ok');
            loadComments();
        } else {
            msg((r.data && r.data.msg) || '发表失败', 'err');
        }
    }

    async function removeComment(id) {
        const r = await apiDel('/app/comments/' + id);
        log(r);
        // 无权限时服务端返回 403（@PreAuthorize hasPermission）
        msg((r.data && r.data.msg) || '已删除', r.status === 200 ? 'ok' : 'err');
        loadComments();
    }
})();
