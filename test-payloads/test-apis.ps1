Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  PULSE - Comment & Like API Tests" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$BASE = "http://localhost:8084"
$AUTH_BASE = "http://localhost:8081"
$PAYLOADS = "test-payloads"

# ─── Step 0: Register & Login ───
Write-Host "[Step 0] Registering test user..." -ForegroundColor Yellow
$regResult = curl.exe -s -w "`n%{http_code}" -X POST "$AUTH_BASE/api/v1/auth/register" -H "Content-Type: application/json" -d "@$PAYLOADS/register.json"
$regLines = $regResult -split "`n"
$regStatus = $regLines[-1]
$regBody = ($regLines[0..($regLines.Length-2)] -join "`n")
Write-Host "  Status: $regStatus"
Write-Host "  Response: $regBody`n"

Write-Host "[Step 0] Logging in..." -ForegroundColor Yellow
$loginResult = curl.exe -s -w "`n%{http_code}" -X POST "$AUTH_BASE/api/v1/auth/login" -H "Content-Type: application/json" -d "@$PAYLOADS/login.json"
$loginLines = $loginResult -split "`n"
$loginStatus = $loginLines[-1]
$loginBody = ($loginLines[0..($loginLines.Length-2)] -join "`n")
Write-Host "  Status: $loginStatus"

$loginJson = $loginBody | ConvertFrom-Json
$TOKEN = $loginJson.accessToken
if (-not $TOKEN) {
    Write-Host "  ERROR: Could not extract accessToken. Aborting." -ForegroundColor Red
    Write-Host "  Response: $loginBody"
    exit 1
}
Write-Host "  Token obtained (first 50 chars): $($TOKEN.Substring(0, [Math]::Min(50, $TOKEN.Length)))..." -ForegroundColor Green

# ─── Step 1: Create a Post ───
Write-Host "`n[Step 1] Creating a post..." -ForegroundColor Yellow
$postResult = curl.exe -s -w "`n%{http_code}" -X POST "$BASE/api/v1/posts" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "@$PAYLOADS/create-post.json"
$postLines = $postResult -split "`n"
$postStatus = $postLines[-1]
$postBody = ($postLines[0..($postLines.Length-2)] -join "`n")
Write-Host "  Status: $postStatus"
Write-Host "  Response: $postBody"

$postJson = $postBody | ConvertFrom-Json
$POST_ID = $postJson.id
Write-Host "  Post ID: $POST_ID`n" -ForegroundColor Green

# ═══════════════════════════════════════
#  COMMENT API TESTS
# ═══════════════════════════════════════
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  COMMENT API TESTS" -ForegroundColor Magenta
Write-Host "========================================`n" -ForegroundColor Magenta

# ─── 2.1: Create Comment ───
Write-Host "[Test 2.1] POST /api/v1/posts/{postId}/comments - Create Comment" -ForegroundColor Yellow
$commentResult = curl.exe -s -w "`n%{http_code}" -X POST "$BASE/api/v1/posts/$POST_ID/comments" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "@$PAYLOADS/create-comment.json"
$commentLines = $commentResult -split "`n"
$commentStatus = $commentLines[-1]
$commentBody = ($commentLines[0..($commentLines.Length-2)] -join "`n")
Write-Host "  Status: $commentStatus (expected: 201)"
if ($commentStatus -eq "201") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL" -ForegroundColor Red }
Write-Host "  Response: $commentBody"

$commentJson = $commentBody | ConvertFrom-Json
$COMMENT_ID = $commentJson.id
Write-Host "  Comment ID: $COMMENT_ID`n" -ForegroundColor Green

# ─── 2.2: Get Comments ───
Write-Host "[Test 2.2] GET /api/v1/posts/{postId}/comments - Get All Comments" -ForegroundColor Yellow
$getCommentsResult = curl.exe -s -w "`n%{http_code}" -X GET "$BASE/api/v1/posts/$POST_ID/comments" -H "Authorization: Bearer $TOKEN"
$getCommentsLines = $getCommentsResult -split "`n"
$getCommentsStatus = $getCommentsLines[-1]
$getCommentsBody = ($getCommentsLines[0..($getCommentsLines.Length-2)] -join "`n")
Write-Host "  Status: $getCommentsStatus (expected: 200)"
if ($getCommentsStatus -eq "200") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL" -ForegroundColor Red }
Write-Host "  Response: $getCommentsBody`n"

# ─── 2.3: Update Comment ───
Write-Host "[Test 2.3] PUT /api/v1/comments/{commentId} - Update Comment" -ForegroundColor Yellow
$updateResult = curl.exe -s -w "`n%{http_code}" -X PUT "$BASE/api/v1/comments/$COMMENT_ID" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "@$PAYLOADS/update-comment.json"
$updateLines = $updateResult -split "`n"
$updateStatus = $updateLines[-1]
$updateBody = ($updateLines[0..($updateLines.Length-2)] -join "`n")
Write-Host "  Status: $updateStatus (expected: 200)"
if ($updateStatus -eq "200") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL" -ForegroundColor Red }
Write-Host "  Response: $updateBody`n"

# ─── 2.4: Delete Comment ───
Write-Host "[Test 2.4] DELETE /api/v1/comments/{commentId} - Delete Comment" -ForegroundColor Yellow
$deleteResult = curl.exe -s -w "`n%{http_code}" -X DELETE "$BASE/api/v1/comments/$COMMENT_ID" -H "Authorization: Bearer $TOKEN"
$deleteLines = $deleteResult -split "`n"
$deleteStatus = $deleteLines[-1]
Write-Host "  Status: $deleteStatus (expected: 204)"
if ($deleteStatus -eq "204") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL" -ForegroundColor Red }

# ─── 2.5: Verify Deletion ───
Write-Host "`n[Test 2.5] GET /api/v1/posts/{postId}/comments - Verify Deletion" -ForegroundColor Yellow
$verifyResult = curl.exe -s -w "`n%{http_code}" -X GET "$BASE/api/v1/posts/$POST_ID/comments" -H "Authorization: Bearer $TOKEN"
$verifyLines = $verifyResult -split "`n"
$verifyStatus = $verifyLines[-1]
$verifyBody = ($verifyLines[0..($verifyLines.Length-2)] -join "`n")
Write-Host "  Status: $verifyStatus"
Write-Host "  Response: $verifyBody (expected: empty array [])`n"

# ═══════════════════════════════════════
#  LIKE API TESTS
# ═══════════════════════════════════════
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  LIKE API TESTS" -ForegroundColor Magenta
Write-Host "========================================`n" -ForegroundColor Magenta

# ─── 3.1: Like a Post ───
Write-Host "[Test 3.1] POST /api/v1/posts/{postId}/like - Like Post" -ForegroundColor Yellow
$likeResult = curl.exe -s -w "`n%{http_code}" -X POST "$BASE/api/v1/posts/$POST_ID/like" -H "Authorization: Bearer $TOKEN"
$likeLines = $likeResult -split "`n"
$likeStatus = $likeLines[-1]
$likeBody = ($likeLines[0..($likeLines.Length-2)] -join "`n")
Write-Host "  Status: $likeStatus (expected: 201)"
if ($likeStatus -eq "201") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL" -ForegroundColor Red }
Write-Host "  Response: $likeBody`n"

# ─── 3.2: Get Likes ───
Write-Host "[Test 3.2] GET /api/v1/posts/{postId}/likes - Get All Likes" -ForegroundColor Yellow
$getLikesResult = curl.exe -s -w "`n%{http_code}" -X GET "$BASE/api/v1/posts/$POST_ID/likes" -H "Authorization: Bearer $TOKEN"
$getLikesLines = $getLikesResult -split "`n"
$getLikesStatus = $getLikesLines[-1]
$getLikesBody = ($getLikesLines[0..($getLikesLines.Length-2)] -join "`n")
Write-Host "  Status: $getLikesStatus (expected: 200)"
if ($getLikesStatus -eq "200") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL" -ForegroundColor Red }
Write-Host "  Response: $getLikesBody`n"

# ─── 3.3: Duplicate Like (should fail) ───
Write-Host "[Test 3.3] POST /api/v1/posts/{postId}/like - Duplicate Like (should fail)" -ForegroundColor Yellow
$dupLikeResult = curl.exe -s -w "`n%{http_code}" -X POST "$BASE/api/v1/posts/$POST_ID/like" -H "Authorization: Bearer $TOKEN"
$dupLikeLines = $dupLikeResult -split "`n"
$dupLikeStatus = $dupLikeLines[-1]
$dupLikeBody = ($dupLikeLines[0..($dupLikeLines.Length-2)] -join "`n")
Write-Host "  Status: $dupLikeStatus (expected: 409 Conflict)"
if ($dupLikeStatus -eq "409") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL (got $dupLikeStatus)" -ForegroundColor Red }
Write-Host "  Response: $dupLikeBody`n"

# ─── 3.4: Unlike a Post ───
Write-Host "[Test 3.4] DELETE /api/v1/posts/{postId}/like - Unlike Post" -ForegroundColor Yellow
$unlikeResult = curl.exe -s -w "`n%{http_code}" -X DELETE "$BASE/api/v1/posts/$POST_ID/like" -H "Authorization: Bearer $TOKEN"
$unlikeLines = $unlikeResult -split "`n"
$unlikeStatus = $unlikeLines[-1]
Write-Host "  Status: $unlikeStatus (expected: 204)"
if ($unlikeStatus -eq "204") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL" -ForegroundColor Red }

# ─── 3.5: Verify Unlike ───
Write-Host "`n[Test 3.5] GET /api/v1/posts/{postId}/likes - Verify Unlike" -ForegroundColor Yellow
$verifyUnlikeResult = curl.exe -s -w "`n%{http_code}" -X GET "$BASE/api/v1/posts/$POST_ID/likes" -H "Authorization: Bearer $TOKEN"
$verifyUnlikeLines = $verifyUnlikeResult -split "`n"
$verifyUnlikeStatus = $verifyUnlikeLines[-1]
$verifyUnlikeBody = ($verifyUnlikeLines[0..($verifyUnlikeLines.Length-2)] -join "`n")
Write-Host "  Status: $verifyUnlikeStatus"
Write-Host "  Response: $verifyUnlikeBody (expected: empty array [])`n"

# ═══════════════════════════════════════
#  EDGE CASE TESTS
# ═══════════════════════════════════════
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  EDGE CASE TESTS" -ForegroundColor Magenta
Write-Host "========================================`n" -ForegroundColor Magenta

# ─── 4.1: Comment on non-existent post ───
Write-Host "[Test 4.1] Comment on non-existent post (expected: 404)" -ForegroundColor Yellow
$fakePostResult = curl.exe -s -w "`n%{http_code}" -X POST "$BASE/api/v1/posts/00000000-0000-0000-0000-000000000000/comments" -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d "@$PAYLOADS/create-comment.json"
$fakePostLines = $fakePostResult -split "`n"
$fakePostStatus = $fakePostLines[-1]
$fakePostBody = ($fakePostLines[0..($fakePostLines.Length-2)] -join "`n")
Write-Host "  Status: $fakePostStatus (expected: 404)"
if ($fakePostStatus -eq "404") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL (got $fakePostStatus)" -ForegroundColor Red }
Write-Host "  Response: $fakePostBody`n"

# ─── 4.2: No auth token (expected: 401) ───
Write-Host "[Test 4.2] Request without auth token (expected: 401)" -ForegroundColor Yellow
$noAuthResult = curl.exe -s -w "`n%{http_code}" -X GET "$BASE/api/v1/posts/$POST_ID/comments"
$noAuthLines = $noAuthResult -split "`n"
$noAuthStatus = $noAuthLines[-1]
Write-Host "  Status: $noAuthStatus (expected: 401)"
if ($noAuthStatus -eq "401") { Write-Host "  PASS" -ForegroundColor Green } else { Write-Host "  FAIL (got $noAuthStatus)" -ForegroundColor Red }

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  ALL TESTS COMPLETED!" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan
