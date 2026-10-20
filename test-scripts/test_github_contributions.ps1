$ErrorActionPreference = "Continue"

$baseUrlAuth = "http://localhost:8081/api/v1/auth"
$baseUrlUser = "http://localhost:8083/api/v1/users"

function Send-Req {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        $Body = $null
    )
    $params = @{
        Method      = $Method
        Uri         = $Uri
        Headers     = $Headers
        ContentType = "application/json"
    }
    if ($Body) {
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }
    try {
        $resp = Invoke-RestMethod @params -ResponseHeadersVariable respHeaders -StatusCodeVariable code
        return @{ StatusCode = $code; Data = $resp }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $errBody = $_.ErrorDetails.Message
        if (-not $errBody -and $_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $errBody = $reader.ReadToEnd()
            }
        }
        return @{ StatusCode = $statusCode; Error = $errBody }
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  GITHUB CONTRIBUTIONS FEATURE TEST SUITE" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Step 1: Authentication
Write-Host "`n[STEP 1] Authenticating user with Auth Service..." -ForegroundColor Yellow
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$email = "dev_$timestamp@pulse.com"
$password = "Secret123!"

$regResp = Send-Req "POST" "$baseUrlAuth/register" @{} @{
    name = "Developer Test"
    email = $email
    password = $password
}

if ($regResp.StatusCode -ne 200 -and $regResp.StatusCode -ne 201) {
    Write-Host "Registration failed: $($regResp.Error)" -ForegroundColor Red
    # Try logging in if account existed
    $loginResp = Send-Req "POST" "$baseUrlAuth/login" @{} @{
        email = $email
        password = $password
    }
    $token = $loginResp.Data.accessToken
} else {
    $token = $regResp.Data.accessToken
}

if (-not $token) {
    Write-Host "Could not retrieve access token. Make sure auth-service (port 8081) and user-service (port 8083) are running." -ForegroundColor Red
    exit 1
}

$authHeaders = @{ "Authorization" = "Bearer $token" }
Write-Host "Logged in successfully as $email" -ForegroundColor Green

# Step 2: Initialize User Profile & Connect GitHub Account
Write-Host "`n[STEP 2] Connecting GitHub account..." -ForegroundColor Yellow
# Ensure profile exists
$null = Send-Req "POST" "$baseUrlUser/create-profile" $authHeaders @{
    name = "Developer Test"
    email = $email
}

# Connect GitHub username
$connectResp = Send-Req "POST" "$baseUrlUser/me/github/connect" $authHeaders @{
    githubUsername = "octocat"
}
Write-Host "Connected GitHub account: Status $($connectResp.StatusCode)" -ForegroundColor Green

# Step 3: Part 1 — Contribution Discovery
Write-Host "`n[STEP 3] Part 1: Discovering contribution repositories..." -ForegroundColor Yellow
$discoverResp = Send-Req "GET" "$baseUrlUser/me/github/contributions/repositories?first=10" $authHeaders
Write-Host "Discovery Response Status: $($discoverResp.StatusCode)" -ForegroundColor Green
if ($discoverResp.Data) {
    $discoverResp.Data | ConvertTo-Json -Depth 5
} else {
    Write-Host "Error or empty: $($discoverResp.Error)" -ForegroundColor Gray
}

# Step 4: Part 2 & 4 — Detailed Contribution Statistics & Verification
Write-Host "`n[STEP 4] Part 2 & 4: Fetching detailed contribution stats for facebook/react..." -ForegroundColor Yellow
$detailResp = Send-Req "GET" "$baseUrlUser/me/github/contributions/facebook/react" $authHeaders
Write-Host "Detail Response Status: $($detailResp.StatusCode)" -ForegroundColor Green
if ($detailResp.Data) {
    $detailResp.Data | ConvertTo-Json -Depth 5
} else {
    Write-Host "Error: $($detailResp.Error)" -ForegroundColor Gray
}

# Step 5: Part 3 — Selecting a Repository for Profile Display
Write-Host "`n[STEP 5] Part 3: Selecting repository for profile display..." -ForegroundColor Yellow
$selectResp = Send-Req "POST" "$baseUrlUser/me/github/contributions/select" $authHeaders @{
    owner = "facebook"
    repo = "react"
}
Write-Host "Select Response Status: $($selectResp.StatusCode)" -ForegroundColor Green
if ($selectResp.Data) {
    $selectResp.Data | ConvertTo-Json -Depth 5
} else {
    Write-Host "Expected if 0 PRs for octocat: $($selectResp.Error)" -ForegroundColor Gray
}

# Step 6: Part 3 — Reading Stored Selected Repositories from Database
Write-Host "`n[STEP 6] Part 3: Reading selected repositories from database (me)..." -ForegroundColor Yellow
$selectedResp = Send-Req "GET" "$baseUrlUser/me/github/contributions/selected" $authHeaders
Write-Host "Selected Repositories Status: $($selectedResp.StatusCode)" -ForegroundColor Green
if ($selectedResp.Data) {
    $selectedResp.Data | ConvertTo-Json -Depth 5
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  TEST SUITE COMPLETED" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
