/* 个人中心：资料、改密、收藏/评论/上传、会话管理、登录日志 */
(function () {
    document.addEventListener('DOMContentLoaded', () => {
        renderTopbar('me').then(() => {
            authState().then(s => {
                if (!s.authenticated) {
                    msg('请先登录', 'err');
                    setTimeout(() => location.href = 'login.html', 800);
                    return;
                }
                loadProfile();
            });
        });

        $('btnSave').addEventListener('click', saveProfile);
        $('btnPwd').addEventListener('click', changePassword);

        const tabs = [
            ['tabFav', 'boxFav', loadFavorites],
            ['tabComment', 'boxComment', loadComments],
            ['tabVideo', 'boxVideo', loadVideos],
            ['tabSession', 'boxSession', loadSessions],
            ['tabLog', 'boxLog', loadLogs]
        ];
        tabs.forEach(([btnId, boxId, fn]) => {
            $(btnId).addEventListener('click', () => {
                tabs.forEach(([b, box]) => {
                    $(b).classList.remove('active');
                    $(box).classList.add('hidden');
                });
                $(btnId).classList.add('active');
                $(boxId).classList.remove('hidden');
                fn();
            });
        });
    });

    async function loadProfile() {
        const r = await apiGet('/app/me/profile');
        log(r);
        if (!r.data || r.data.code !== 0) return;
        const u = r.data.data;
        $('nickname').value = u.nickname || '';
        $('email').value = u.email || '';
        $('phone').value = u.phone || '';
        $('avatar').value = u.avatar || '';
        loadFavorites();
    }

    async function saveProfile() {
        const r = await apiPut('/app/me/profile', {
            nickname: $('nickname').value,
            email: $('email').value,
            phone: $('phone').value,
            avatar: $('avatar').value
        });
        log(r);
        msg((r.data && r.data.msg) || '已保存', r.status === 200 ? 'ok' : 'err');
    }

    async function changePassword() {
        const r = await apiPost('/app/me/password', {
            oldPassword: $('oldPassword').value,
            newPassword: $('newPassword').value
        });
        log(r);
        msg((r.data && r.data.msg) || '已修改', r.status === 200 ? 'ok' : 'err');
        if (r.status === 200) {
            $('oldPassword').value = '';
            $('newPassword').value = '';
        }
    }

    async function loadFavorites() {
        const r = await apiGet('/app/me/favorites');
        log(r);
        const box = $('boxFav');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="muted">无数据</div>'; return; }
        box.innerHTML = renderVideoTable(r.data.data);
    }

    async function loadComments() {
        const r = await apiGet('/app/me/comments');
        log(r);
        const box = $('boxComment');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="muted">无数据</div>'; return; }
        box.innerHTML = '<table><tr><th>视频</th><th>内容</th><th>时间</th><th>操作</th></tr>'
            + r.data.data.map(c => '<tr><td>' + c.videoId + '</td><td>' + escapeHtml(c.content) + '</td>'
                + '<td>' + fmtTime(c.createTime) + '</td>'
                + '<td><button class="red" data-id="' + c.id + '">删除</button></td></tr>').join('')
            + '</table>';
        box.querySelectorAll('button[data-id]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const res = await apiDel('/app/comments/' + btn.getAttribute('data-id'));
                log(res);
                msg((res.data && res.data.msg) || '已删除', res.status === 200 ? 'ok' : 'err');
                loadComments();
            });
        });
    }

    async function loadVideos() {
        const r = await apiGet('/app/me/videos');
        log(r);
        const box = $('boxVideo');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="muted">无数据</div>'; return; }
        box.innerHTML = renderVideoTable(r.data.data);
    }

    async function loadSessions() {
        const r = await apiGet('/app/me/sessions');
        log(r);
        const box = $('boxSession');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="muted">无数据</div>'; return; }
        box.innerHTML = '<table><tr><th>会话ID</th><th>最后访问</th><th>是否过期</th><th>操作</th></tr>'
            + r.data.data.map(s => '<tr><td>' + escapeHtml(String(s.sessionId).substring(0, 16)) + '…</td>'
                + '<td>' + escapeHtml(s.lastRequest) + '</td><td>' + (s.expired ? '已过期' : '在线') + '</td>'
                + '<td><button class="red" data-sid="' + escapeHtml(s.sessionId) + '">下线该设备</button></td></tr>').join('')
            + '</table>';
        box.querySelectorAll('button[data-sid]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const res = await apiDel('/app/me/sessions/' + btn.getAttribute('data-sid'));
                log(res);
                msg((res.data && res.data.msg) || '已下线', res.status === 200 ? 'ok' : 'err');
                loadSessions();
            });
        });
    }

    async function loadLogs() {
        const r = await apiGet('/app/me/login-logs');
        log(r);
        const box = $('boxLog');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="muted">无数据</div>'; return; }
        box.innerHTML = '<table><tr><th>类型</th><th>IP</th><th>是否成功</th><th>说明</th><th>时间</th></tr>'
            + r.data.data.map(l => '<tr><td>' + escapeHtml(l.type) + '</td><td>' + escapeHtml(l.ip) + '</td>'
                + '<td>' + (l.success ? '成功' : '失败') + '</td><td>' + escapeHtml(l.message || '') + '</td>'
                + '<td>' + fmtTime(l.createTime) + '</td></tr>').join('') + '</table>';
    }

    function renderVideoTable(list) {
        if (!list || list.length === 0) return '<div class="muted">暂无数据</div>';
        return '<table><tr><th>ID</th><th>标题</th><th>分类</th><th>状态</th><th>播放</th></tr>'
            + list.map(v => '<tr><td>' + v.id + '</td><td><a href="detail.html?id=' + v.id + '">'
                + escapeHtml(v.title) + '</a></td><td>' + escapeHtml(v.category || '') + '</td>'
                + '<td><span class="tag status-' + escapeHtml(v.status) + '">' + escapeHtml(v.status) + '</span></td>'
                + '<td>' + (v.viewCount || 0) + '</td></tr>').join('') + '</table>';
    }
})();
