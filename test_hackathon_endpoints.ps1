$ErrorActionPreference = "Continue"

$baseUrlAuth = "http://localhost:8081/api/v1/auth"
$baseUrlPost = "http://localhost:8084/api/v1"

function Send-Req {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        $Body = $null
    )
    $params = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
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
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        return @{ StatusCode = $statusCode; Error = $errBody }
    }
}

Write-Host "=================== STEP 1: AUTHENTICATION ==================="
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

# Register Organizer
$orgEmail = "organizer_$timestamp@pulse.com"
$orgReg = Send-Req "POST" "$baseUrlAuth/register" @{} @{
    name = "Code Org"
    email = $orgEmail
    password = "Password@123"
    role = "ORGANIZER"
}
$orgToken = $orgReg.Data.accessToken
Write-Host "Organizer registered ($orgEmail): Status $($orgReg.StatusCode)"

# Register Student 1
$s1Email = "student1_$timestamp@pulse.com"
$s1Reg = Send-Req "POST" "$baseUrlAuth/register" @{} @{
    name = "Alice Student"
    email = $s1Email
    password = "Password@123"
    role = "USER"
}
$s1Token = $s1Reg.Data.accessToken
Write-Host "Student 1 registered ($s1Email): Status $($s1Reg.StatusCode)"

# Register Student 2
$s2Email = "student2_$timestamp@pulse.com"
$s2Reg = Send-Req "POST" "$baseUrlAuth/register" @{} @{
    name = "Bob Student"
    email = $s2Email
    password = "Password@123"
    role = "USER"
}
$s2Token = $s2Reg.Data.accessToken
Write-Host "Student 2 registered ($s2Email): Status $($s2Reg.StatusCode)"

# Register Student 3
$s3Email = "student3_$timestamp@pulse.com"
$s3Reg = Send-Req "POST" "$baseUrlAuth/register" @{} @{
    name = "Charlie Student"
    email = $s3Email
    password = "Password@123"
    role = "USER"
}
$s3Token = $s3Reg.Data.accessToken
Write-Host "Student 3 registered ($s3Email): Status $($s3Reg.StatusCode)"

$orgHeaders = @{ "Authorization" = "Bearer $orgToken" }
$s1Headers  = @{ "Authorization" = "Bearer $s1Token" }
$s2Headers  = @{ "Authorization" = "Bearer $s2Token" }
$s3Headers  = @{ "Authorization" = "Bearer $s3Token" }

Write-Host "`n=================== TEST 1: RBAC CHECK (STUDENT CANNOT CREATE HACKATHON) ==================="
$t1 = Send-Req "POST" "$baseUrlPost/hackathons" $s1Headers @{
    title = "Illegal Hackathon"
    description = "Should fail"
    mode = "ONLINE"
    startDate = "2026-11-01T10:00:00"
    endDate = "2026-11-03T18:00:00"
    registrationDeadline = "2026-10-31T23:59:59"
}
Write-Host "Expected 403 Forbidden -> Got $($t1.StatusCode): $($t1.Error)"

Write-Host "`n=================== TEST 2: ORGANIZER CREATES HACKATHON ==================="
$t2 = Send-Req "POST" "$baseUrlPost/hackathons" $orgHeaders @{
    title = "Pulse AI Hackathon 2026"
    description = "Build cutting-edge distributed microservices and AI applications"
    mode = "HYBRID"
    venue = "Bangalore Tech Hub / Online"
    websiteUrl = "https://pulse.dev/hackathon2026"
    startDate = "2026-11-01T10:00:00"
    endDate = "2026-11-03T18:00:00"
    registrationDeadline = "2026-10-31T23:59:59"
    minTeamSize = 1
    maxTeamSize = 2
}
Write-Host "Expected 201 Created -> Got $($t2.StatusCode)"
$hackathonId = $t2.Data.id
Write-Host "Created Hackathon ID: $hackathonId"

Write-Host "`n=================== TEST 3: LIST ALL HACKATHONS ==================="
$t3 = Send-Req "GET" "$baseUrlPost/hackathons" $s1Headers
Write-Host "Expected 200 OK -> Got $($t3.StatusCode) | Count: $($t3.Data.Count)"

Write-Host "`n=================== TEST 4: GET HACKATHON DETAILS ==================="
$t4 = Send-Req "GET" "$baseUrlPost/hackathons/$hackathonId" $s1Headers
Write-Host "Expected 200 OK -> Got $($t4.StatusCode) | Title: $($t4.Data.title) | Max Team Size: $($t4.Data.maxTeamSize) | Reg Count: $($t4.Data.registrationCount)"

Write-Host "`n=================== TEST 5: GET ORGANIZER'S HACKATHONS ==================="
$t5 = Send-Req "GET" "$baseUrlPost/hackathons/my" $orgHeaders
Write-Host "Expected 200 OK -> Got $($t5.StatusCode) | Organizer's hackathon count: $($t5.Data.Count)"

Write-Host "`n=================== TEST 6: STUDENT 1 REGISTERS WITH TEAM 'CyberKnights' ==================="
$t6 = Send-Req "POST" "$baseUrlPost/hackathons/$hackathonId/register" $s1Headers @{
    teamName = "CyberKnights"
}
Write-Host "Expected 201 Created -> Got $($t6.StatusCode) | Registration ID: $($t6.Data.id) | Status: $($t6.Data.status)"
$s1RegId = $t6.Data.id

Write-Host "`n=================== TEST 7: STUDENT 1 TRIES TO REGISTER AGAIN (DUPLICATE) ==================="
$t7 = Send-Req "POST" "$baseUrlPost/hackathons/$hackathonId/register" $s1Headers @{
    teamName = "CyberKnights"
}
Write-Host "Expected 400 Bad Request -> Got $($t7.StatusCode): $($t7.Error)"

Write-Host "`n=================== TEST 8: STUDENT 2 JOINS TEAM 'CyberKnights' ==================="
$t8 = Send-Req "POST" "$baseUrlPost/hackathons/$hackathonId/register" $s2Headers @{
    teamName = "CyberKnights"
}
Write-Host "Expected 201 Created -> Got $($t8.StatusCode) | Registration ID: $($t8.Data.id)"

Write-Host "`n=================== TEST 9: STUDENT 3 JOINS 'CyberKnights' (CAPACITY EXCEEDED) ==================="
$t9 = Send-Req "POST" "$baseUrlPost/hackathons/$hackathonId/register" $s3Headers @{
    teamName = "CyberKnights"
}
Write-Host "Expected 400 Bad Request -> Got $($t9.StatusCode): $($t9.Error)"

Write-Host "`n=================== TEST 10: GET TEAM MEMBERS OF 'CyberKnights' ==================="
$t10 = Send-Req "GET" "$baseUrlPost/hackathons/$hackathonId/teams/CyberKnights" $s1Headers
Write-Host "Expected 200 OK -> Got $($t10.StatusCode) | Team members count: $($t10.Data.Count)"

Write-Host "`n=================== TEST 11: STUDENT 1 CHECKS MY-REGISTRATIONS ==================="
$t11 = Send-Req "GET" "$baseUrlPost/hackathons/my-registrations" $s1Headers
Write-Host "Expected 200 OK -> Got $($t11.StatusCode) | Registered hackathons count: $($t11.Data.Count)"

Write-Host "`n=================== TEST 12: ORGANIZER VIEWS REGISTRATIONS ==================="
$t12 = Send-Req "GET" "$baseUrlPost/hackathons/$hackathonId/registrations" $orgHeaders
Write-Host "Expected 200 OK -> Got $($t12.StatusCode) | Registrations count: $($t12.Data.Count)"

Write-Host "`n=================== TEST 13: ORGANIZER UPDATES STUDENT 1 STATUS TO 'WINNER' ==================="
$t13 = Send-Req "PUT" "$baseUrlPost/hackathons/$hackathonId/registrations/$s1RegId/status" $orgHeaders @{
    status = "WINNER"
}
Write-Host "Expected 200 OK -> Got $($t13.StatusCode) | New Status: $($t13.Data.status)"

Write-Host "`n=================== TEST 14: STUDENT 1 CREATES POST TAGGING HACKATHON ==================="
$t14 = Send-Req "POST" "$baseUrlPost/posts" $s1Headers @{
    content = "Proud to announce our team CyberKnights took 1st place in the Pulse AI Hackathon 2026!"
    hackathonId = $hackathonId
    hackathonTagType = "WINNER"
}
Write-Host "Expected 201 Created -> Got $($t14.StatusCode) | Post ID: $($t14.Data.id) | Tagged Hackathon: $($t14.Data.hackathonId) | Tag Type: $($t14.Data.hackathonTagType)"
$postId = $t14.Data.id

Write-Host "`n=================== TEST 15: GET POSTS TAGGED WITH HACKATHON ==================="
$t15 = Send-Req "GET" "$baseUrlPost/hackathons/$hackathonId/posts" $s1Headers
Write-Host "Expected 200 OK -> Got $($t15.StatusCode) | Showcase posts count: $($t15.Data.Count)"

Write-Host "`n=================== TEST 16: ORGANIZER UPDATES HACKATHON ==================="
$t16 = Send-Req "PUT" "$baseUrlPost/hackathons/$hackathonId" $orgHeaders @{
    title = "Pulse AI Hackathon 2026 [RESULTS ANNOUNCED]"
}
Write-Host "Expected 200 OK -> Got $($t16.StatusCode) | Updated Title: $($t16.Data.title)"

Write-Host "`n=================== ALL TESTS COMPLETED! ==================="
