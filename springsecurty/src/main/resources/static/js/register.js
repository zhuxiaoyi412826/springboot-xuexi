/* 注册页 */
(function () {
    document.addEventListener('DOMContentLoaded', () => {
        $('btnReg').addEventListener('click', submit);
    });

    async function submit() {
        const username = $('username').value.trim();
        const password = $('password').value;
        const nickname = $('nickname').value.trim();
        if (username.length < 4 || password.length < 6) {
            msg('用户名至少 4 位，密码至少 6 位', 'err');
            return;
        }
        const r = await apiPost('/app/auth/register', { username: username, password: password, nickname: nickname });
        log(r);
        if (r.status === 200) {
            msg('注册成功，去登录吧', 'ok');
            setTimeout(() => location.href = 'login.html', 800);
        } else {
            msg((r.data && r.data.msg) || '注册失败', 'err');
        }
    }
})();
