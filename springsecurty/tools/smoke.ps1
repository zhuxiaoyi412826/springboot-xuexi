# ASCII-only smoke test for video-site (Spring Security lab)
$b = "http://localhost:8084"
$tmp = $env:TEMP
$adminJar = "$tmp\c_admin.txt"
$zhangJar = "$tmp\c_zhang.txt"
$lisiJar  = "$tmp\c_lisi.txt"
Remove-Item $adminJar,$zhangJar,$lisiJar -ErrorAction SilentlyContinue

function Token($jar) {
    $r = curl.exe -s -c $jar -b $jar -H "X-Requested-With: XMLHttpRequest" "$b/app/auth/csrf"
    try { return ($r | ConvertFrom-Json).data.token } catch { return "" }
}

function Login($jar, $u, $p, $remember) {
    $t = Token $jar
    $body = "username=$u&password=$p"
    if ($remember) { $body = $body + "&remember-me=on" }
    return curl.exe -s -c $jar -b $jar -X POST -H "Content-Type: application/x-www-form-urlencoded" `
        -H "X-Requested-With: XMLHttpRequest" -H "X-XSRF-TOKEN: $t" -d $body "$b/login"
}

function Code($jar, $method, $url, $json) {
    # 只有 /app、/api 这类接口才带 XHR 头；页面 URL 带 XHR 会被当成 JSON 请求返回 401
    $xhr = ""
    if ($url -match "^/(app|api)/") { $xhr = "-H|X-Requested-With: XMLHttpRequest" }
    $t = Token $jar
    if ($json) {
        # PS5.1 直接把 JSON 传给 curl.exe 会吞掉双引号，统一走临时文件 -d @file
        $req = "$tmp\req.json"
        [System.IO.File]::WriteAllText($req, $json, (New-Object System.Text.UTF8Encoding($false)))
        if ($xhr) {
            return curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -c $jar -b $jar -X $method `
                -H "Content-Type: application/json" -H "X-Requested-With: XMLHttpRequest" -H "X-XSRF-TOKEN: $t" `
                -d "@$req" "$b$url"
        }
        return curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -c $jar -b $jar -X $method `
            -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $t" -d "@$req" "$b$url"
    }
    if ($xhr) {
        return curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -c $jar -b $jar -X $method `
            -H "X-Requested-With: XMLHttpRequest" -H "X-XSRF-TOKEN: $t" "$b$url"
    }
    return curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -c $jar -b $jar -X $method `
        -H "X-XSRF-TOKEN: $t" "$b$url"
}

function Body { return [System.IO.File]::ReadAllText("$tmp\body.txt", [System.Text.Encoding]::UTF8) }

function Json($raw) {
    try { return $raw | ConvertFrom-Json } catch { return $null }
}

function Check($name, $actual, $expected) {
    if ("$actual" -eq "$expected") { "PASS  $name (= $actual)" } else { "FAIL  $name : expected $expected but got $actual" }
}

"================ 1. anonymous browsing ================"
$code = Code "$tmp\c_anon.txt" "GET" "/app/videos/list?page=1&size=20" $null
Check "anonymous list 200" $code "200"
$json = Body | ConvertFrom-Json
Check "anonymous sees only PUBLIC" (($json.data.list | Where-Object { $_.status -ne "PUBLIC" }).Count) 0
"anonymous video count = " + $json.data.total
$code = Code "$tmp\c_anon.txt" "GET" "/app/admin/stats" $null
Check "anonymous admin api -> 401" $code "401"

"================ 2. login (form + CSRF) ================"
$r = Login $adminJar "admin" "admin123" $false
Check "admin login 200" (($r | ConvertFrom-Json).code) "200"
$code = Code $adminJar "GET" "/app/auth/state" $null
$s = Body | ConvertFrom-Json
Check "admin authenticated" $s.data.authenticated "True"
"admin roles = " + ($s.data.roles -join ",")
$code = Code $adminJar "GET" "/app/admin/stats" $null
Check "admin stats 200" $code "200"

"================ 3. authorization (URL + method) ================"
$r = Login $zhangJar "zhangsan" "123456" $false
Check "zhangsan login" (($r | ConvertFrom-Json).code) "200"
$code = Code $zhangJar "GET" "/app/admin/users" $null
Check "zhangsan admin api -> 403" $code "403"
$code = Code $zhangJar "GET" "/user/me.html" $null
Check "zhangsan can open /user/me.html" $code "200"
$code = Code "$tmp\c_anon.txt" "GET" "/admin/index.html" $null
Check "anonymous /admin -> 302 to login" $code "302"

"================ 4. object-level permission (delete comment) ================"
$code = Code $zhangJar "POST" "/app/comments/add" '{"videoId":1,"content":"hello from zhangsan"}'
Check "zhangsan add comment 200" $code "200"
$newId = (Body | ConvertFrom-Json).data.id
"new comment id = $newId"
$code = Code $zhangJar "DELETE" "/app/comments/$newId" $null
Check "owner delete own comment 200" $code "200"
$code = Code $zhangJar "DELETE" "/app/comments/1" $null
Check "delete other's comment -> 403" $code "403"
$code = Code $adminJar "DELETE" "/app/comments/2" $null
Check "admin delete any comment 200" $code "200"

"================ 5. data permission (private video) ================"
$code = Code "$tmp\c_anon.txt" "GET" "/app/videos/9" $null
Check "anonymous private video -> 403" $code "403"
$code = Code $zhangJar "GET" "/app/videos/9" $null
Check "owner can see private video" $code "200"
$code = Code $adminJar "GET" "/app/videos/9" $null
Check "admin can see private video" $code "200"
$code = Code $zhangJar "GET" "/app/videos/list?page=1&size=20" $null
$cnt = (Body | ConvertFrom-Json).data.total
"zhangsan visible count = $cnt (anonymous was smaller)"

"================ 6. fine-grained authority (VIP download) ================"
$code = Code $zhangJar "POST" "/app/videos/1/download" $null
Check "normal user download -> 403" $code "403"
$r = Login $lisiJar "lisi" "123456" $false
$code = Code $lisiJar "POST" "/app/videos/1/download" $null
Check "VIP download 200" $code "200"

"================ 7. CSRF ================"
$req = "$tmp\req.json"
[System.IO.File]::WriteAllText($req, '{"videoId":1,"content":"no csrf"}', (New-Object System.Text.UTF8Encoding($false)))
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -b $zhangJar -X POST `
    -H "Content-Type: application/json" -d "@$req" "$b/app/comments/add"
Check "POST without CSRF token -> 403" $code "403"

"================ 8. HTTP Basic / Digest ================"
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -u admin:admin123 "$b/app/basic/me"
Check "basic auth 200" $code "200"
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -u admin:wrongpwd "$b/app/basic/me"
Check "basic wrong password -> 401" $code "401"
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" --digest -u admin:admin123 "$b/digest/me"
Check "digest auth 200" $code "200"
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -u admin:admin123 "$b/digest/me"
Check "digest area without digest -> 401" $code "401"

"================ 9. OAuth2 resource server (JWT) ================"
$tokenReq = "$tmp\token.json"
[System.IO.File]::WriteAllText($tokenReq, '{"username":"admin","password":"admin123"}', (New-Object System.Text.UTF8Encoding($false)))
$tokenResp = curl.exe -s -X POST -H "Content-Type: application/json" -d "@$tokenReq" "$b/api/auth/token"
$jwt = (Json $tokenResp).data.access_token
"jwt length = " + $jwt.Length
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -H "Authorization: Bearer $jwt" "$b/api/videos"
Check "JWT /api/videos 200" $code "200"
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" "$b/api/videos"
Check "no token /api/videos -> 401" $code "401"
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -H "Authorization: Bearer $jwt" "$b/api/me"
Check "JWT /api/me 200" $code "200"

"================ 10. custom filter (API Key) ================"
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -H "X-API-Key: video-site-api-key-8888" "$b/api/ping"
Check "api key /api/ping 200" $code "200"
$code = curl.exe -s -o "$tmp\body.txt" -w "%{http_code}" -H "X-API-Key: wrong" "$b/api/ping"
Check "wrong api key -> 401" $code "401"

"================ 11. session management ================"
$code = Code $adminJar "GET" "/app/admin/sessions" $null
$sess = (Body | ConvertFrom-Json).data
"online sessions = " + $sess.Count
$code = Code $adminJar "POST" "/app/admin/sessions/kick?username=zhangsan" $null
Check "admin kick zhangsan" $code "200"
$code = Code $zhangJar "GET" "/app/auth/state" $null
$kicked = Body | ConvertFrom-Json
Check "zhangsan kicked (not authenticated)" $kicked.data.authenticated "False"

"================ 12. disabled account ================"
$lr = Login "$tmp\c_locked.txt" "locked" "123456" $false
try { "locked login msg = " + ($lr | ConvertFrom-Json).msg } catch { "locked login raw = " + $lr }

"================ 13. security headers ================"
curl.exe -s -D - -o NUL "$b/index.html" | Select-String -Pattern "X-Frame-Options|Content-Security-Policy|X-Content-Type-Options|Referrer-Policy|Permissions-Policy|Strict-Transport" | ForEach-Object { "  " + $_.Line.Trim() }

"================ 14. remember-me ================"
$rmJar = "$tmp\c_rm.txt"
Remove-Item $rmJar -ErrorAction SilentlyContinue
$r = Login $rmJar "wangwu" "vip888" $true
Check "wangwu login with remember-me" (($r | ConvertFrom-Json).code) "200"
"remember-me cookie present: " + ((Get-Content $rmJar -Raw) -match "remember-me")
