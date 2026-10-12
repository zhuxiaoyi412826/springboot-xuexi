function toggleRegister() {
    document.getElementById('register-box').classList.toggle('hidden');
}

async function doLogin() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    if (!username || !password) { toast('请输入用户名和密码', true); return; }
    try {
        const data = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        }).then(r => r.json());
        if (data.code !== 200) { toast(data.msg, true); return; }
        setToken(data.data.token);
        setUser(data.data.user);
        toast('登录成功，欢迎 ' + (data.data.user.nickname || data.data.user.username));
        const redirect = new URLSearchParams(location.search).get('redirect');
        setTimeout(() => location.href = redirect || data.data.home, 400);
    } catch (e) {
        toast('登录失败：' + e.message, true);
    }
}

async function doRegister() {
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const nickname = document.getElementById('nickname').value.trim();
    try {
        const data = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, nickname })
        }).then(r => r.json());
        if (data.code !== 200) { toast(data.msg, true); return; }
        toast('注册成功，请登录');
        toggleRegister();
    } catch (e) {
        toast('注册失败：' + e.message, true);
    }
}

document.getElementById('password').addEventListener('keydown', e => {
    if (e.key === 'Enter') doLogin();
});
