/* 登录页：表单登录（含记住我） */
(function () {
    function init() {
        const btn = $('btnLogin');
        if (btn) {
            btn.addEventListener('click', submit);
        }
        const pwd = $('password');
        if (pwd) {
            pwd.addEventListener('keydown', e => {
                if (e.key === 'Enter') submit();
            });
        }

        // 展示 URL 上的提示（登录失败/会话过期/被踢下线/已退出）
        const p = new URLSearchParams(location.search);
        if (p.get('error')) msg(decodeURIComponent(p.get('error')), 'err');
        if (p.get('expired')) msg('该账号已在其它地方登录，当前会话被挤下线', 'err');
        if (p.get('invalid')) msg('会话已失效，请重新登录', 'err');
        if (p.get('logout')) msg('已安全退出', 'ok');

        // 已登录则直接回首页
        authState().then(s => {
            if (s.authenticated) msg('你已经登录了：' + (s.nickname || s.username), 'ok');
        });

        // 动态渲染第三方登录按钮（OAuth2 / OIDC）
        apiGet('/app/auth/oauth2-clients').then(r => {
            const box = $('oauth2Box');
            const list = (r.data && r.data.data) || [];
            if (!list.length) {
                box.innerHTML = '<span class="muted">（未配置 OAuth2 客户端）</span>';
                return;
            }
            box.innerHTML = list.map(c =>
                '<a href="' + c.loginUrl + '"><button class="gray" type="button">使用 '
                + escapeHtml(c.clientName || c.registrationId) + ' 登录</button></a>').join('');
        });
    }

    async function submit() {
        const username = $('username').value.trim();
        const password = $('password').value;
        const remember = $('remember').checked;
        if (!username || !password) {
            msg('请输入用户名和密码', 'err');
            return;
        }
        const r = await doLogin(username, password, remember);
        log(r);
        if (r.status === 200) {
            clearState();
            msg('登录成功，跳转中……', 'ok');
            setTimeout(() => location.href = 'home.html', 500);
        } else {
            msg((r.data && r.data.msg) || '登录失败', 'err');
        }
    }

    document.addEventListener('DOMContentLoaded', init);
})();
