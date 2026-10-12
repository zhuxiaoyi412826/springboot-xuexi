# 在线听歌网站冒烟测试（PowerShell）
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$port = if ($env:MUSIC_PORT) { $env:MUSIC_PORT } else { "8085" }
$b = "http://localhost:$port"
$tmp = $env:TEMP
$adminTok = ""
$userTok = ""
$pass = 0
$fail = 0

function Check($name, $cond, $extra) {
    if ($cond) { $script:pass++; Write-Output ("PASS  " + $name + $(if ($extra) { " -> " + $extra })) }
    else { $script:fail++; Write-Output ("FAIL  " + $name + $(if ($extra) { " -> " + $extra })) }
}

function Post($path, $body, $token) {
    $headers = @{ 'Content-Type' = 'application/json' }
    if ($token) { $headers['satoken'] = $token }
    try {
        return Invoke-RestMethod -Uri ($b + $path) -Method Post -Headers $headers -Body $body
    } catch {
        return $_.Exception.Response
    }
}

function GetJson($path, $token) {
    $headers = @{}
    if ($token) { $headers['satoken'] = $token }
    try {
        return Invoke-RestMethod -Uri ($b + $path) -Method Get -Headers $headers
    } catch {
        return $null
    }
}

function Code($path, $token) {
    $headers = @{}
    if ($token) { $headers['satoken'] = $token }
    try {
        $r = Invoke-WebRequest -Uri ($b + $path) -Method Get -Headers $headers -UseBasicParsing
        return $r.StatusCode
    } catch {
        return [int]$_.Exception.Response.StatusCode
    }
}

"================ 1. 基础连通 ================"
$ping = GetJson "/api/public/ping" $null
Check "ping + redis" ($ping.data.redis -eq "pong") $ping.data.redis
Check "登录页可访问" ((Code "/login.html" $null) -eq 200)
Check "用户首页可匿名浏览" ((Code "/user/index.html" $null) -eq 200)
Check "管理页未登录被拦" ((Code "/admin/index.html" $null) -eq 401)

"================ 2. 登录（共用登录页） ================"
$admin = Post "/api/auth/login" '{"username":"admin","password":"admin123"}' $null
$adminTok = $admin.data.token
Check "admin 登录" ($admin.code -eq 200) ("role=" + $admin.data.user.role + " home=" + $admin.data.home)
$user = Post "/api/auth/login" '{"username":"zhangsan","password":"123456"}' $null
$userTok = $user.data.token
Check "zhangsan 登录" ($user.code -eq 200) ("role=" + $user.data.user.role)
$bad = Post "/api/auth/login" '{"username":"admin","password":"wrong"}' $null
Check "错误密码被拒" ($bad.code -ne 200) $bad.msg

"================ 3. 歌曲（三级缓存 + 榜单） ================"
$list = GetJson "/api/songs/list?page=1&size=5" $userTok
Check "歌曲列表" ($list.code -eq 200) ("共 " + $list.data.total + " 首")
$detail = GetJson "/api/songs/1" $userTok
Check "歌曲详情" ($detail.code -eq 200) $detail.data.song.title
$play = Post "/api/songs/1/play" '{}' $userTok
Check "播放计数" ($play.code -eq 200) ("playCount=" + $play.data.playCount)
$hot = GetJson "/api/songs/hot?limit=5" $userTok
Check "热歌榜" ($hot.code -eq 200) ("Top" + $hot.data.Count)
$like = Post "/api/songs/2/like" '{}' $userTok
Check "点赞" ($like.code -eq 200) ("liked=" + $like.data.liked)
$fav = Post "/api/songs/2/favorite" '{}' $userTok
Check "收藏" ($fav.code -eq 200) ("favorited=" + $fav.data.favorited)

"================ 4. 评论（限流 + 幂等 + 分布式 ID） ================"
$comment = Post "/api/comments/add" '{"songId":1,"content":"冒烟测试评论","idempotentKey":"smoke-1"}' $userTok
Check "发评论" ($comment.code -eq 200) ("id=" + $comment.data.id)
$dup = Post "/api/comments/add" '{"songId":1,"content":"冒烟测试评论","idempotentKey":"smoke-1"}' $userTok
Check "幂等拦截重复提交" ($dup.code -ne 200) $dup.msg
$comments = GetJson "/api/comments/list?songId=1" $userTok
Check "评论列表" ($comments.code -eq 200) ("共 " + $comments.data.Count + " 条")

"================ 5. 搜索 / 签到 / 统计 ================"
$search = GetJson "/api/search?q=周杰伦" $userTok
Check "搜索" ($search.code -eq 200)
$hotSearch = GetJson "/api/search/hot?limit=5" $userTok
Check "热搜榜" ($hotSearch.code -eq 200) ("词数=" + $hotSearch.data.Count)
$sign = Post "/api/me/sign" '{}' $userTok
Check "签到（Bitmap）" (($sign.code -eq 200) -or ($sign.msg -like '*已经签到*')) ("total=" + $sign.data.total)
$stats = GetJson "/api/me/stats" $userTok
Check "UV/日活统计" ($stats.code -eq 200) ("uv=" + $stats.data.uv + " active=" + $stats.data.active)

"================ 6. 权限（Sa-Token） ================"
Check "普通用户访问管理接口被拒" ((Code "/api/admin/users/list" $userTok) -eq 403)
Check "管理员访问管理接口放行" ((Code "/api/admin/users/list" $adminTok) -eq 200)
Check "未登录访问需要登录的接口" ((Code "/api/songs/favorites" $null) -eq 401)

"================ 7. 缓存与 Redis 面板 ================"
$overview = GetJson "/api/admin/cache/overview" $adminTok
Check "缓存总览" ($overview.code -eq 200) ("Redis keys=" + $overview.data.redis.dbSize)
Check "命中率有统计" ($overview.data.cacheStats._summary.totalRequests -gt 0) ("总请求=" + $overview.data.cacheStats._summary.totalRequests + " 命中率=" + $overview.data.cacheStats._summary.hitRate)
$keys = GetJson "/api/admin/cache/keys?pattern=music:cache:*&count=20" $adminTok
Check "SCAN 查看缓存 key" ($keys.code -eq 200) ("key 数=" + $keys.data.Count)

"================ 8. Redis 实验室（数据结构全覆盖） ================"
$cases = @(
    "/api/lab/string/set?k=demo&v=hi&ttl=60",
    "/api/lab/string/incr?k=demo",
    "/api/lab/string/setnx?k=demo",
    "/api/lab/hash/set?k=demo&f=nick&v=tom",
    "/api/lab/hash/incr?k=demo&f=nick",
    "/api/lab/list/push?k=demo&v=a",
    "/api/lab/list/pop?k=demo",
    "/api/lab/set/add?k=demo&m=x",
    "/api/lab/set/add?k=demo2&m=x",
    "/api/lab/set/inter?k1=demo&k2=demo2",
    "/api/lab/zset/incr?k=demo&m=song1",
    "/api/lab/zset/top?k=demo&n=5",
    "/api/lab/bitmap/set?k=demo&offset=3&value=true",
    "/api/lab/bitmap/get?k=demo&offset=3",
    "/api/lab/hll/add?k=demo&v=u1",
    "/api/lab/geo/add?k=demo&member=a&lng=116.4&lat=39.9",
    "/api/lab/geo/near?k=demo&lng=116.4&lat=39.9&radius=100",
    "/api/lab/stream/read?k=play&n=5",
    "/api/lab/pubsub/pub?msg=smoke-test",
    "/api/lab/lock?name=smoke&lease=5",
    "/api/lab/ratelimit?biz=smoke&capacity=5&refill=1",
    "/api/lab/pipeline?n=100",
    "/api/lab/tx",
    "/api/lab/delay/add?payload=song:1:online&seconds=30",
    "/api/lab/delay/list",
    "/api/lab/id/next?biz=smoke",
    "/api/lab/id/range?biz=smoke&step=5",
    "/api/lab/bloom/add?name=smoke&value=abc",
    "/api/lab/bloom/exists?name=smoke&value=abc",
    "/api/lab/bloom/exists?name=smoke&value=not-exist-xyz",
    "/api/lab/keys?pattern=music:lab:*&count=50",
    "/api/lab/ttl?key=music:lab:demo"
)
foreach ($c in $cases) {
    $r = GetJson $c $null
    $ok = ($r -ne $null) -and ($r.code -eq 200)
    Check ("lab " + $c.Split('?')[0]) $ok $(if ($r) { $r.data.tip })
}

"================ 9. 管理端业务 ================"
$songs = GetJson "/api/admin/songs/list?page=1&size=5" $adminTok
Check "管理端歌曲列表" ($songs.code -eq 200) ("共 " + $songs.data.total)
$users = GetJson "/api/admin/users/list?page=1&size=5" $adminTok
Check "管理端用户列表" ($users.code -eq 200) ("共 " + $users.data.total)
$online = GetJson "/api/admin/users/online" $adminTok
Check "在线用户（Redis Set）" ($online.code -eq 200) ("在线 " + $online.data.count + " 人")
$queue = GetJson "/api/admin/playlists/audit-queue" $adminTok
Check "待审核队列（List）" ($queue.code -eq 200)
$notices = GetJson "/api/admin/cache/notices" $adminTok
Check "Pub/Sub 通知已收到" (($notices.code -eq 200) -and ($notices.data.Count -gt 0)) ("通知数=" + $notices.data.Count)

"================ 结果 ================"
"PASS=$pass  FAIL=$fail"
