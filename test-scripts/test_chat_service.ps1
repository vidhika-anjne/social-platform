# ==============================================================================
# PULSE - Chat Service End-to-End API Test Script
# ==============================================================================
param(
    [string]$BaseUrl = "http://localhost:8087",
    [string]$AuthServiceUrl = "http://localhost:8081/api/v1/auth"
)

$ErrorActionPreference = "Continue"

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   PULSE CHAT SERVICE API TEST SUITE" -ForegroundColor Cyan
Write-Host "   Target URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "=======================================================`n" -ForegroundColor Cyan

# ------------------------------------------------------------------------------
# Helper: Generate valid JWT using HMAC-SHA384 and Pulse Secret Key
# ------------------------------------------------------------------------------
function New-PulseJwt {
    param(
        [string]$UserId,
        [string]$Email,
        [string]$Role = "USER",
        [string]$Secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    )

    $header = @{ alg = "HS384"; typ = "JWT" }
    $headerJson = $header | ConvertTo-Json -Compress
    $headerBytes = [System.Text.Encoding]::UTF8.GetBytes($headerJson)
    $headerBase64 = [Convert]::ToBase64String($headerBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')

    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $exp = $now + 7200
    $payload = @{
        sub   = $UserId
        email = $Email
        role  = $Role
        iat   = $now
        exp   = $exp
    }
    $payloadJson = $payload | ConvertTo-Json -Compress
    $payloadBytes = [System.Text.Encoding]::UTF8.GetBytes($payloadJson)
    $payloadBase64 = [Convert]::ToBase64String($payloadBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')

    $unsignedToken = "$headerBase64.$payloadBase64"
    $keyBytes = [Convert]::FromBase64String($Secret)
    $hmac = New-Object System.Security.Cryptography.HMACSHA384
    $hmac.Key = $keyBytes
    $sigBytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($unsignedToken))
    $sigBase64 = [Convert]::ToBase64String($sigBytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')

    return "$unsignedToken.$sigBase64"
}

# ------------------------------------------------------------------------------
# Helper: Send HTTP Request
# ------------------------------------------------------------------------------
function Send-ApiReq {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token = $null,
        $Body = $null
    )

    $url = "$BaseUrl$Path"
    $headers = @{
        "Content-Type" = "application/json"
    }
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $params = @{
        Method  = $Method
        Uri     = $url
        Headers = $headers
    }
    if ($Body) {
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }

    try {
        $resp = Invoke-RestMethod @params -ResponseHeadersVariable respHeaders -StatusCodeVariable code
        return @{ StatusCode = $code; Data = $resp; Success = $true }
    } catch {
        $statusCode = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 500 }
        $errText = ""
        if ($_.Exception.Response) {
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $errText = $reader.ReadToEnd()
            }
        }
        return @{ StatusCode = $statusCode; Error = $errText; Success = $false }
    }
}

# ------------------------------------------------------------------------------
# Test Setup: Create 3 Test Users
# ------------------------------------------------------------------------------
$userAId = [Guid]::NewGuid().ToString()
$userBId = [Guid]::NewGuid().ToString()
$userCId = [Guid]::NewGuid().ToString()

$tokenA = New-PulseJwt -UserId $userAId -Email "alice@pulse.com" -Role "USER"
$tokenB = New-PulseJwt -UserId $userBId -Email "bob@pulse.com" -Role "USER"
$tokenC = New-PulseJwt -UserId $userCId -Email "charlie@pulse.com" -Role "USER"

Write-Host "Test Identities Created:" -ForegroundColor Gray
Write-Host "  User A (Alice):   $userAId" -ForegroundColor Gray
Write-Host "  User B (Bob):     $userBId" -ForegroundColor Gray
Write-Host "  User C (Charlie): $userCId`n" -ForegroundColor Gray

$passed = 0
$failed = 0

function Assert-Test {
    param([string]$Name, [bool]$Condition, [string]$Details = "")
    if ($Condition) {
        Write-Host "  [PASS] $Name" -ForegroundColor Green
        $script:passed++
    } else {
        Write-Host "  [FAIL] $Name. $Details" -ForegroundColor Red
        $script:failed++
    }
}

# ------------------------------------------------------------------------------
# Test 1: Unauthorized Request
# ------------------------------------------------------------------------------
Write-Host "[Test 1] GET /api/v1/chats without Token (Expect 401)" -ForegroundColor Yellow
$res1 = Send-ApiReq -Method "GET" -Path "/api/v1/chats"
Assert-Test "Unauthorized request returns 401" ($res1.StatusCode -eq 401) "Got status: $($res1.StatusCode)"

# ------------------------------------------------------------------------------
# Test 2: Create 1-to-1 Conversation
# ------------------------------------------------------------------------------
Write-Host "`n[Test 2] POST /api/v1/chats - Create 1-to-1 conversation between Alice and Bob" -ForegroundColor Yellow
$bodyOneToOne = @{
    type = "ONE_TO_ONE"
    recipientId = $userBId
}
$res2 = Send-ApiReq -Method "POST" -Path "/api/v1/chats" -Token $tokenA -Body $bodyOneToOne
$oneToOneId = $res2.Data.id
Assert-Test "1-to-1 chat created (status 201)" ($res2.StatusCode -eq 201 -and $oneToOneId) "Got status: $($res2.StatusCode)"
Assert-Test "Type is ONE_TO_ONE" ($res2.Data.type -eq "ONE_TO_ONE")
Assert-Test "Has 2 members" ($res2.Data.members.Count -eq 2)

# ------------------------------------------------------------------------------
# Test 3: Deduplication of 1-to-1 Conversation
# ------------------------------------------------------------------------------
Write-Host "`n[Test 3] POST /api/v1/chats - Repeat 1-to-1 chat creation (Should return existing)" -ForegroundColor Yellow
$res3 = Send-ApiReq -Method "POST" -Path "/api/v1/chats" -Token $tokenA -Body $bodyOneToOne
Assert-Test "Duplicate 1-to-1 returns existing conversation ID" ($res3.Data.id -eq $oneToOneId) "Expected ID: $oneToOneId, got: $($res3.Data.id)"

# ------------------------------------------------------------------------------
# Test 4: Create GROUP Conversation
# ------------------------------------------------------------------------------
Write-Host "`n[Test 4] POST /api/v1/chats - Create GROUP conversation" -ForegroundColor Yellow
$bodyGroup = @{
    type = "GROUP"
    name = "Pulse Core Hackers"
    memberIds = @($userBId)
}
$res4 = Send-ApiReq -Method "POST" -Path "/api/v1/chats" -Token $tokenA -Body $bodyGroup
$groupId = $res4.Data.id
Assert-Test "GROUP chat created (status 201)" ($res4.StatusCode -eq 201 -and $groupId) "Got status: $($res4.StatusCode)"
Assert-Test "Group name matches" ($res4.Data.name -eq "Pulse Core Hackers")

# ------------------------------------------------------------------------------
# Test 5: List User Conversations
# ------------------------------------------------------------------------------
Write-Host "`n[Test 5] GET /api/v1/chats - List Alice's conversations" -ForegroundColor Yellow
$res5 = Send-ApiReq -Method "GET" -Path "/api/v1/chats" -Token $tokenA
Assert-Test "User conversations retrieved (status 200)" ($res5.StatusCode -eq 200) "Got status: $($res5.StatusCode)"
Assert-Test "User has at least 2 conversations" ($res5.Data.Count -ge 2) "Count: $($res5.Data.Count)"

# ------------------------------------------------------------------------------
# Test 6: Get Conversation by ID (Member vs Non-Member)
# ------------------------------------------------------------------------------
Write-Host "`n[Test 6] GET /api/v1/chats/{id} - Member access vs Non-member forbidden" -ForegroundColor Yellow
$res6a = Send-ApiReq -Method "GET" -Path "/api/v1/chats/$groupId" -Token $tokenB
Assert-Test "Member (Bob) can view group details (200)" ($res6a.StatusCode -eq 200) "Got status: $($res6a.StatusCode)"

$res6b = Send-ApiReq -Method "GET" -Path "/api/v1/chats/$groupId" -Token $tokenC
Assert-Test "Non-member (Charlie) is forbidden (403)" ($res6b.StatusCode -eq 403) "Got status: $($res6b.StatusCode)"

# ------------------------------------------------------------------------------
# Test 7: Get Members List
# ------------------------------------------------------------------------------
Write-Host "`n[Test 7] GET /api/v1/chats/{id}/members - Retrieve group members" -ForegroundColor Yellow
$res7 = Send-ApiReq -Method "GET" -Path "/api/v1/chats/$groupId/members" -Token $tokenA
Assert-Test "Members list retrieved (status 200)" ($res7.StatusCode -eq 200)
Assert-Test "Group currently has 2 members" ($res7.Data.Count -eq 2)

# ------------------------------------------------------------------------------
# Test 8: Add Member to Group
# ------------------------------------------------------------------------------
Write-Host "`n[Test 8] POST /api/v1/chats/{id}/members - Add Charlie to group" -ForegroundColor Yellow
$bodyAddMember = @{ userId = $userCId }
$res8 = Send-ApiReq -Method "POST" -Path "/api/v1/chats/$groupId/members" -Token $tokenA -Body $bodyAddMember
Assert-Test "Member added (status 200)" ($res8.StatusCode -eq 200)
Assert-Test "Group now has 3 members" ($res8.Data.Count -eq 3)

# ------------------------------------------------------------------------------
# Test 9: Remove Member from Group
# ------------------------------------------------------------------------------
Write-Host "`n[Test 9] DELETE /api/v1/chats/{id}/members/{userId} - Remove Bob" -ForegroundColor Yellow
$res9 = Send-ApiReq -Method "DELETE" -Path "/api/v1/chats/$groupId/members/$userBId" -Token $tokenA
Assert-Test "Member removed (status 204)" ($res9.StatusCode -eq 204)

$res9Check = Send-ApiReq -Method "GET" -Path "/api/v1/chats/$groupId/members" -Token $tokenA
Assert-Test "Members count is now 2" ($res9Check.Data.Count -eq 2)

# ------------------------------------------------------------------------------
# Test 10: Message History Pagination
# ------------------------------------------------------------------------------
Write-Host "`n[Test 10] GET /api/v1/chats/{id}/messages - Fetch paginated messages" -ForegroundColor Yellow
$res10 = Send-ApiReq -Method "GET" -Path "/api/v1/chats/$groupId/messages?page=0&size=10&direction=desc" -Token $tokenA
Assert-Test "Message history retrieved (status 200)" ($res10.StatusCode -eq 200)

# ------------------------------------------------------------------------------
# Test 11: Delete Conversation (Non-creator rejected, Creator allowed)
# ------------------------------------------------------------------------------
Write-Host "`n[Test 11] DELETE /api/v1/chats/{id} - Delete conversation permission check" -ForegroundColor Yellow
$res11a = Send-ApiReq -Method "DELETE" -Path "/api/v1/chats/$groupId" -Token $tokenC
Assert-Test "Non-creator (Charlie) cannot delete conversation (403)" ($res11a.StatusCode -eq 403) "Got: $($res11a.StatusCode)"

$res11b = Send-ApiReq -Method "DELETE" -Path "/api/v1/chats/$groupId" -Token $tokenA
Assert-Test "Creator (Alice) can delete conversation (204)" ($res11b.StatusCode -eq 204) "Got: $($res11b.StatusCode)"

# Clean up 1-to-1 conversation
$null = Send-ApiReq -Method "DELETE" -Path "/api/v1/chats/$oneToOneId" -Token $tokenA

# ------------------------------------------------------------------------------
# Summary
# ------------------------------------------------------------------------------
Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   TEST RESULTS SUMMARY" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "  Passed: $passed" -ForegroundColor Green
Write-Host "  Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })
Write-Host "=======================================================`n" -ForegroundColor Cyan

if ($failed -eq 0) {
    exit 0
} else {
    exit 1
}
