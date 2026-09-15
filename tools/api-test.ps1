# =====================================================================
#  智能客服工单系统 —— 8 个接口联调测试脚本
#  用途：一条命令验证所有接口是否正常（改造后回归测试也用它）
#
#  用法：
#     powershell -ExecutionPolicy Bypass -File tools\api-test.ps1
#
#  说明：用 .NET HttpClient 而不是 Invoke-RestMethod，
#        因为 Invoke-RestMethod 在响应头没有 charset 时会按 Latin-1 解码，
#        中文会显示成乱码（服务端其实是对的）。
# =====================================================================

Add-Type -AssemblyName System.Net.Http

$Base = 'http://localhost:8080'
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(15)
$token = $null
$pass = 0
$fail = 0

function Send-Api {
    param([string]$Method, [string]$Path, [string]$BodyJson)

    $m = switch ($Method) {
        'GET'    { [System.Net.Http.HttpMethod]::Get }
        'POST'   { [System.Net.Http.HttpMethod]::Post }
        'PUT'    { [System.Net.Http.HttpMethod]::Put }
        'DELETE' { [System.Net.Http.HttpMethod]::Delete }
    }
    $req = New-Object System.Net.Http.HttpRequestMessage($m, "$Base$Path")
    if ($token) { $req.Headers.Add('Authorization', "Bearer $token") }
    if ($BodyJson) {
        $req.Content = New-Object System.Net.Http.StringContent($BodyJson, [System.Text.Encoding]::UTF8, 'application/json')
    }
    $resp  = $client.SendAsync($req).Result
    $bytes = $resp.Content.ReadAsByteArrayAsync().Result
    return [System.Text.Encoding]::UTF8.GetString($bytes)
}

function Show-Step {
    param([string]$Index, [string]$Name, [string]$Response, [string]$Expect)
    Write-Host ""
    Write-Host "[$Index] $Name" -ForegroundColor Cyan
    $short = $Response
    if ($short.Length -gt 300) { $short = $short.Substring(0, 300) + ' ...(已截断)' }
    Write-Host "      $short" -ForegroundColor Gray
    if ($Expect -and $Response -notlike "*$Expect*") {
        Write-Host "      [FAIL] 预期包含: $Expect" -ForegroundColor Red
        $script:fail++
    } else {
        Write-Host "      [PASS]" -ForegroundColor Green
        $script:pass++
    }
}

Write-Host "=====================================================" -ForegroundColor Yellow
Write-Host "  智能客服工单系统 - 接口联调测试" -ForegroundColor Yellow
Write-Host "  目标: $Base" -ForegroundColor Yellow
Write-Host "=====================================================" -ForegroundColor Yellow

# ---------- 0. 先看服务在不在 ----------
try {
    $null = Send-Api -Method GET -Path '/api/ticket/stats'
} catch {
    Write-Host ""
    Write-Host "  [X] 连不上 $Base" -ForegroundColor Red
    Write-Host "      请先确认：" -ForegroundColor Red
    Write-Host "        1) IDEA 里 SmartTicketApplication 已经 Run 起来" -ForegroundColor Red
    Write-Host "        2) MySQL80 服务已启动 (net start MySQL80)" -ForegroundColor Red
    Write-Host "        3) Redis 服务已启动   (net start Redis)" -ForegroundColor Red
    Write-Host ""
    exit 1
}

# ---------- 1. 未登录访问（应该被拦） ----------
$r = Send-Api -Method GET -Path '/api/ticket/page'
Show-Step -Index '1/8' -Name 'GET  /api/ticket/page   不带 token（预期被拦截）' -Response $r -Expect '"code":401'

# ---------- 2. 登录 ----------
$r = Send-Api -Method POST -Path '/api/auth/login' -BodyJson '{"username":"admin","password":"123456"}'
Show-Step -Index '2/8' -Name 'POST /api/auth/login' -Response $r -Expect '"code":200'
try {
    $obj = $r | ConvertFrom-Json
    $token = $obj.data.token
    Write-Host "      token 前 40 位: $($token.Substring(0,[Math]::Min(40,$token.Length)))..." -ForegroundColor DarkGray
} catch {
    Write-Host "      [X] 拿不到 token，后面全部跳过" -ForegroundColor Red
    exit 1
}

# ---------- 3. 带 token 查当前用户 ----------
$r = Send-Api -Method GET -Path '/api/auth/info'
Show-Step -Index '3/8' -Name 'GET  /api/auth/info' -Response $r -Expect 'admin'

# ---------- 4. 分页查询 ----------
$r = Send-Api -Method GET -Path '/api/ticket/page?pageNum=1&pageSize=3'
Show-Step -Index '4/8' -Name 'GET  /api/ticket/page?pageNum=1&pageSize=3' -Response $r -Expect '"total"'

# ---------- 5. 新建工单 ----------
$newId = $null
$body = '{"title":"测试工单-来自联调脚本","content":"登录不上系统，提示密码错误","customerName":"自动化测试","customerPhone":"13800000000","priority":3}'
$r = Send-Api -Method POST -Path '/api/ticket' -BodyJson $body
Show-Step -Index '5/8' -Name 'POST /api/ticket        新建工单' -Response $r -Expect '"code":200'
try { $newId = ($r | ConvertFrom-Json).data.id } catch { }

# ---------- 6. 查详情 ----------
if ($newId) {
    $r = Send-Api -Method GET -Path "/api/ticket/$newId"
    Show-Step -Index '6/8' -Name "GET  /api/ticket/$newId      工单详情" -Response $r -Expect '"code":200'
} else {
    Write-Host ""
    Write-Host "[6/8] 跳过（上一步没拿到 id）" -ForegroundColor Red
    $fail++
}

# ---------- 7. 改状态 ----------
if ($newId) {
    $r = Send-Api -Method PUT -Path "/api/ticket/$newId/status?status=1"
    Show-Step -Index '7/8' -Name "PUT  /api/ticket/$newId/status?status=1" -Response $r -Expect '"code":200'
} else {
    Write-Host ""
    Write-Host "[7/8] 跳过" -ForegroundColor Red
    $fail++
}

# ---------- 8. 统计（走 Redis 缓存） ----------
$r = Send-Api -Method GET -Path '/api/ticket/stats'
Show-Step -Index '8/8' -Name 'GET  /api/ticket/stats   统计（第 1 次：回源数据库）' -Response $r -Expect '"total"'
$r2 = Send-Api -Method GET -Path '/api/ticket/stats'
Write-Host ""
Write-Host "[附加] 再调一次 stats —— 去 IDEA 控制台找 '统计数据命中缓存'" -ForegroundColor Cyan
Write-Host "      $r2" -ForegroundColor Gray

# ---------- 收尾：删掉测试工单 ----------
if ($newId) {
    $r = Send-Api -Method DELETE -Path "/api/ticket/$newId"
    Write-Host ""
    Write-Host "[清理] DELETE /api/ticket/$newId   $r" -ForegroundColor DarkGray
}

$client.Dispose()
Write-Host ""
Write-Host "=====================================================" -ForegroundColor Yellow
Write-Host "  通过 $pass 项，失败 $fail 项" -ForegroundColor $(if ($fail -eq 0) { 'Green' } else { 'Red' })
Write-Host "=====================================================" -ForegroundColor Yellow
