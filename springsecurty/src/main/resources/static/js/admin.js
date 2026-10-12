/* 管理后台：概览 / 用户 / 视频 / 评论 / 会话（强制下线）/ 日志 */
(function () {
    const tabs = [
        ['tabStats', 'boxStats', loadStats],
        ['tabUsers', 'boxUsers', loadUsers],
        ['tabVideos', 'boxVideos', loadVideos],
        ['tabComments', 'boxComments', loadComments],
        ['tabSessions', 'boxSessions', loadSessions],
        ['tabLogs', 'boxLogs', loadLogs]
    ];

    document.addEventListener('DOMContentLoaded', () => {
        // /admin/** 页面受 URL 级保护；接口再受方法级保护
        renderTopbar('admin').then(() => {
            authState().then(s => {
                if (!s.authenticated) {
                    location.href = '/user/login.html';
                    return;
                }
                if (!s.roles || s.roles.indexOf('ROLE_ADMIN') < 0) {
                    location.href = '/user/403.html';
                    return;
                }
                loadStats();
            });
        });

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

    async function loadStats() {
        const r = await apiGet('/app/admin/stats');
        log(r);
        if (!r.data || r.data.code !== 0) {
            msg((r.data && r.data.msg) || '加载失败（权限不足？）', 'err');
            return;
        }
        const d = r.data.data;
        $('boxStats').innerHTML = '<table>'
            + '<tr><th>用户数</th><th>视频数</th><th>公开视频</th><th>评论数</th><th>收藏数</th><th>登录日志</th></tr>'
            + '<tr><td>' + d.users + '</td><td>' + d.videos + '</td><td>' + d.publicVideos + '</td>'
            + '<td>' + d.comments + '</td><td>' + d.favorites + '</td><td>' + d.loginLogs + '</td></tr></table>';
    }

    async function loadUsers() {
        const kw = prompt('搜索用户名/昵称（可留空）', '') || '';
        const r = await apiGet('/app/admin/users?keyword=' + encodeURIComponent(kw));
        log(r);
        const box = $('boxUsers');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="err">加载失败</div>'; return; }
        box.innerHTML = '<table><tr><th>ID</th><th>用户名</th><th>昵称</th><th>角色</th><th>状态</th><th>来源</th><th>最后登录</th><th>操作</th></tr>'
            + r.data.data.list.map(u => '<tr>'
                + '<td>' + u.id + '</td><td>' + escapeHtml(u.username) + '</td><td>' + escapeHtml(u.nickname || '') + '</td>'
                + '<td>' + escapeHtml((u.roles || []).join(' ')) + '</td>'
                + '<td>' + (u.enabled ? '启用' : '<b style="color:#dc2626">禁用</b>')
                + (u.accountNonLocked ? '' : ' / 锁定') + '</td>'
                + '<td>' + escapeHtml(u.source || '') + '</td><td>' + fmtTime(u.lastLoginTime) + '</td>'
                + '<td>'
                + '<button class="gray" data-act="toggle" data-id="' + u.id + '" data-enabled="' + (u.enabled ? 'false' : 'true') + '">'
                + (u.enabled ? '禁用' : '启用') + '</button> '
                + '<button class="orange" data-act="vip" data-id="' + u.id + '">设为VIP</button> '
                + '<button class="orange" data-act="reset" data-id="' + u.id + '">重置密码</button> '
                + '<button class="gray" data-act="unlock" data-id="' + u.id + '">解锁</button> '
                + '<button class="red" data-act="del" data-id="' + u.id + '">删除</button>'
                + '</td></tr>').join('') + '</table>';

        box.querySelectorAll('button[data-act]').forEach(btn => {
            btn.addEventListener('click', () => onUserAction(btn));
        });
    }

    async function onUserAction(btn) {
        const id = btn.getAttribute('data-id');
        const act = btn.getAttribute('data-act');
        let r = null;
        if (act === 'toggle') {
            r = await apiPost('/app/admin/users/' + id + '/enabled', { enabled: btn.getAttribute('data-enabled') === 'true' });
        } else if (act === 'vip') {
            r = await apiPost('/app/admin/users/' + id + '/roles', { roles: ['ROLE_USER', 'ROLE_VIP'] });
        } else if (act === 'reset') {
            const pwd = prompt('输入新密码', '123456');
            if (pwd) r = await apiPost('/app/admin/users/' + id + '/reset-password', { password: pwd });
        } else if (act === 'unlock') {
            r = await apiPost('/app/admin/users/' + id + '/unlock');
        } else if (act === 'del') {
            if (!confirm('确认删除该用户？')) return;
            r = await apiDel('/app/admin/users/' + id);
        }
        if (r) {
            log(r);
            msg((r.data && r.data.msg) || '操作完成', r.status === 200 ? 'ok' : 'err');
            loadUsers();
        }
    }

    async function loadVideos() {
        const r = await apiGet('/app/admin/videos?size=20');
        log(r);
        const box = $('boxVideos');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="err">加载失败</div>'; return; }
        box.innerHTML = '<table><tr><th>ID</th><th>标题</th><th>作者</th><th>分类</th><th>状态</th><th>播放</th><th>操作</th></tr>'
            + r.data.data.list.map(v => '<tr><td>' + v.id + '</td><td>' + escapeHtml(v.title) + '</td>'
                + '<td>' + escapeHtml(v.authorName || '') + '</td><td>' + escapeHtml(v.category || '') + '</td>'
                + '<td><span class="tag status-' + escapeHtml(v.status) + '">' + escapeHtml(v.status) + '</span></td>'
                + '<td>' + (v.viewCount || 0) + '</td>'
                + '<td><button class="green" data-act="public" data-id="' + v.id + '">上架</button> '
                + '<button class="orange" data-act="review" data-id="' + v.id + '">转待审</button> '
                + '<button class="red" data-act="del" data-id="' + v.id + '">删除</button></td></tr>').join('') + '</table>';

        box.querySelectorAll('button[data-act]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-id');
                const act = btn.getAttribute('data-act');
                let r;
                if (act === 'del') {
                    if (!confirm('确认删除该视频？')) return;
                    r = await apiDel('/app/admin/videos/' + id);
                } else {
                    r = await apiPost('/app/admin/videos/' + id + '/status', { status: act === 'public' ? 'PUBLIC' : 'REVIEW' });
                }
                log(r);
                msg((r.data && r.data.msg) || '操作完成', r.status === 200 ? 'ok' : 'err');
                loadVideos();
            });
        });
    }

    async function loadComments() {
        const r = await apiGet('/app/admin/comments?size=20');
        log(r);
        const box = $('boxComments');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="err">加载失败</div>'; return; }
        box.innerHTML = '<table><tr><th>ID</th><th>用户</th><th>内容</th><th>时间</th><th>操作</th></tr>'
            + r.data.data.list.map(c => '<tr><td>' + c.id + '</td><td>' + escapeHtml(c.username) + '</td>'
                + '<td>' + escapeHtml(c.content) + '</td><td>' + fmtTime(c.createTime) + '</td>'
                + '<td><button class="red" data-id="' + c.id + '">删除</button></td></tr>').join('') + '</table>';
        box.querySelectorAll('button[data-id]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const r2 = await apiDel('/app/admin/comments/' + btn.getAttribute('data-id'));
                log(r2);
                msg((r2.data && r2.data.msg) || '已删除', r2.status === 200 ? 'ok' : 'err');
                loadComments();
            });
        });
    }

    async function loadSessions() {
        const r = await apiGet('/app/admin/sessions');
        log(r);
        const box = $('boxSessions');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="err">加载失败</div>'; return; }
        const list = r.data.data;
        if (!list.length) {
            box.innerHTML = '<div class="muted">当前没有在线会话（登录后可见）</div>';
            return;
        }
        box.innerHTML = '<table><tr><th>用户名</th><th>会话ID</th><th>最后访问</th><th>状态</th><th>操作</th></tr>'
            + list.map(s => '<tr><td>' + escapeHtml(s.username) + '</td><td>' + escapeHtml(String(s.sessionId).substring(0, 20)) + '…</td>'
                + '<td>' + escapeHtml(s.lastRequest) + '</td><td>' + (s.expired ? '已过期' : '在线') + '</td>'
                + '<td><button class="red" data-user="' + escapeHtml(s.username) + '">强制下线</button></td></tr>').join('') + '</table>';
        box.querySelectorAll('button[data-user]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const r2 = await apiPost('/app/admin/sessions/kick?username=' + encodeURIComponent(btn.getAttribute('data-user')));
                log(r2);
                msg((r2.data && r2.data.msg) || '已下线', r2.status === 200 ? 'ok' : 'err');
                loadSessions();
            });
        });
    }

    async function loadLogs() {
        const r = await apiGet('/app/admin/logs');
        log(r);
        const box = $('boxLogs');
        if (!r.data || r.data.code !== 0) { box.innerHTML = '<div class="err">加载失败</div>'; return; }
        box.innerHTML = '<table><tr><th>用户名</th><th>类型</th><th>IP</th><th>结果</th><th>说明</th><th>时间</th></tr>'
            + r.data.data.map(l => '<tr><td>' + escapeHtml(l.username || '') + '</td><td>' + escapeHtml(l.type) + '</td>'
                + '<td>' + escapeHtml(l.ip || '') + '</td><td>' + (l.success ? '成功' : '失败') + '</td>'
                + '<td>' + escapeHtml(l.message || '') + '</td><td>' + fmtTime(l.createTime) + '</td></tr>').join('') + '</table>';
    }
})();
