$ErrorActionPreference = "Continue"
$BASE_AUTH   = "http://localhost:8081/api/v1/auth"
$BASE_USER   = "http://localhost:8083/api/v1/users"
$BASE_POST   = "http://localhost:8084/api/v1/posts"

function Invoke-Api($label, $method, $url, $body, $token) {
    Write-Host "`n===> $label" -ForegroundColor Yellow
    $headers = @{ "Content-Type" = "application/json" }
    if ($token) { $headers["Authorization"] = "Bearer $token" }

    try {
        if ($body) {
            $resp = Invoke-RestMethod -Method $method -Uri $url -Headers $headers -Body ($body | ConvertTo-Json -Compress) -ErrorAction Stop
        } else {
            $resp = Invoke-RestMethod -Method $method -Uri $url -Headers $headers -ErrorAction Stop
        }
        $resp | ConvertTo-Json -Depth 5
        return $resp
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $body = $_.ErrorDetails.Message
        Write-Host "HTTP $statusCode - $body" -ForegroundColor Red
        return $null
    }
}

# ─── AUTH SERVICE ─────────────────────────────────────────────────────────────
Write-Host "`n============================================" -ForegroundColor Magenta
Write-Host "  AUTH SERVICE (port 8081)" -ForegroundColor Magenta
Write-Host "============================================" -ForegroundColor Magenta

$regResp = Invoke-Api "POST /register (should fail - already exists)" POST "$BASE_AUTH/register" @{
    name     = "Vidhika"
    email    = "vidhika@test.com"
    password = "secret123"
}

$loginResp = Invoke-Api "POST /login" POST "$BASE_AUTH/login" @{
    email    = "vidhika@test.com"
    password = "secret123"
}

$TOKEN = $loginResp.accessToken
Write-Host "`n[TOKEN EXTRACTED]: $($TOKEN.Substring(0,40))..." -ForegroundColor Green

# ─── USER SERVICE ─────────────────────────────────────────────────────────────
Write-Host "`n============================================" -ForegroundColor Magenta
Write-Host "  USER SERVICE (port 8083)" -ForegroundColor Magenta
Write-Host "============================================" -ForegroundColor Magenta

Invoke-Api "POST /create-profile" POST "$BASE_USER/create-profile" @{
    name            = "Vidhika Anjne"
    number          = "9999999999"
    age             = 21
    gender          = "FEMALE"
    college         = "MIT Pune"
    educationDegree = "B.Tech"
    currentYear     = "3rd"
    city            = "Pune"
    bio             = "Building microservices!"
} $TOKEN | Out-Null

Invoke-Api "GET /me" GET "$BASE_USER/me" $null $TOKEN | Out-Null
Invoke-Api "GET /profile" GET "$BASE_USER/profile" $null $TOKEN | Out-Null

# ─── POST SERVICE ─────────────────────────────────────────────────────────────
Write-Host "`n============================================" -ForegroundColor Magenta
Write-Host "  POST SERVICE (port 8084)" -ForegroundColor Magenta
Write-Host "============================================" -ForegroundColor Magenta

$post1 = Invoke-Api "POST /posts (text only)" POST "$BASE_POST" @{
    content = "Hello Pulse! My first post."
} $TOKEN

$post2 = Invoke-Api "POST /posts (with mediaUrl)" POST "$BASE_POST" @{
    content  = "Check out this image!"
    mediaUrl = "https://example.com/image.png"
} $TOKEN

$postId = $post1.id
Write-Host "`n[POST ID]: $postId" -ForegroundColor Green

Invoke-Api "GET /posts/$postId" GET "$BASE_POST/$postId" $null $TOKEN | Out-Null
Invoke-Api "GET /posts/my" GET "$BASE_POST/my" $null $TOKEN | Out-Null

Invoke-Api "PUT /posts/$postId (update content)" PUT "$BASE_POST/$postId" @{
    content = "Updated: Hello Pulse! (edited)"
} $TOKEN | Out-Null

Invoke-Api "GET /posts/$postId (verify update)" GET "$BASE_POST/$postId" $null $TOKEN | Out-Null

# ─── OWNERSHIP CHECK ──────────────────────────────────────────────────────────
Write-Host "`n============================================" -ForegroundColor Magenta
Write-Host "  OWNERSHIP CHECK (should return 403)" -ForegroundColor Magenta
Write-Host "============================================" -ForegroundColor Magenta

# Register a second user and try to delete post1 with their token
$reg2 = Invoke-Api "POST /register (user2)" POST "$BASE_AUTH/register" @{
    name     = "OtherUser"
    email    = "other@test.com"
    password = "secret123"
}
$login2 = Invoke-Api "POST /login (user2)" POST "$BASE_AUTH/login" @{
    email    = "other@test.com"
    password = "secret123"
}
$TOKEN2 = $login2.accessToken
Invoke-Api "DELETE /posts/$postId with wrong user (expect 403)" DELETE "$BASE_POST/$postId" $null $TOKEN2 | Out-Null

# ─── DELETE ───────────────────────────────────────────────────────────────────
Write-Host "`n============================================" -ForegroundColor Magenta
Write-Host "  CLEANUP" -ForegroundColor Magenta
Write-Host "============================================" -ForegroundColor Magenta

Invoke-Api "DELETE /posts/$postId (owner, should succeed)" DELETE "$BASE_POST/$postId" $null $TOKEN | Out-Null
Invoke-Api "GET /posts/$postId (expect 404)" GET "$BASE_POST/$postId" $null $TOKEN | Out-Null

Write-Host "`n============================================" -ForegroundColor Green
Write-Host "  ALL TESTS DONE" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
