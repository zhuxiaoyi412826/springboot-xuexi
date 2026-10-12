/* 403 页面：显示当前登录者是谁，为什么被拒绝 */
document.addEventListener('DOMContentLoaded', () => {
    authState().then(s => {
        const box = $('msg');
        if (!box) return;
        if (s.authenticated) {
            box.textContent = '当前登录：' + (s.nickname || s.username) + '，角色：'
                + (s.roles || []).join(' ') + ' —— 权限不足。';
        } else {
            box.textContent = '你还未登录，请先登录。';
        }
    });
});
