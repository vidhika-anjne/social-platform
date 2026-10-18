import urllib.request
import urllib.error
import json
import time

BASE_AUTH = "http://localhost:8081/api/v1/auth"
BASE_POST = "http://localhost:8084/api/v1"

def req(method, url, data=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    body = json.dumps(data).encode("utf-8") if data is not None else None
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            status = response.getcode()
            res_body = response.read().decode("utf-8")
            return status, json.loads(res_body) if res_body else None
    except urllib.error.HTTPError as e:
        status = e.code
        err_body = e.read().decode("utf-8")
        try:
            parsed = json.loads(err_body)
        except Exception:
            parsed = err_body
        return status, parsed
    except Exception as e:
        return 0, str(e)

print("=" * 60)
print("RUNNING END-TO-END HACKATHON TEST SUITE")
print("=" * 60)

ts = int(time.time() * 1000)

# 1. Register Organizer
org_email = f"org_{ts}@pulse.com"
status, body = req("POST", f"{BASE_AUTH}/register", {
    "name": "Org Admin",
    "email": org_email,
    "password": "Password@123",
    "role": "ORGANIZER"
})
print(f"1. Register Organizer ({org_email}) -> Status: {status}")
assert status == 200 or status == 201, f"Failed: {body}"
org_token = body["accessToken"]

# 2. Register Student 1
s1_email = f"alice_{ts}@pulse.com"
status, body = req("POST", f"{BASE_AUTH}/register", {
    "name": "Alice Smith",
    "email": s1_email,
    "password": "Password@123",
    "role": "USER"
})
print(f"2. Register Student 1 ({s1_email}) -> Status: {status}")
s1_token = body["accessToken"]

# 3. Register Student 2
s2_email = f"bob_{ts}@pulse.com"
status, body = req("POST", f"{BASE_AUTH}/register", {
    "name": "Bob Jones",
    "email": s2_email,
    "password": "Password@123",
    "role": "USER"
})
print(f"3. Register Student 2 ({s2_email}) -> Status: {status}")
s2_token = body["accessToken"]

# 4. Register Student 3
s3_email = f"charlie_{ts}@pulse.com"
status, body = req("POST", f"{BASE_AUTH}/register", {
    "name": "Charlie Brown",
    "email": s3_email,
    "password": "Password@123",
    "role": "USER"
})
print(f"4. Register Student 3 ({s3_email}) -> Status: {status}")
s3_token = body["accessToken"]

# TEST 1: Student attempts to create hackathon (RBAC forbidden)
print("\n--- TEST 1: Student tries to create Hackathon (RBAC Check) ---")
status, body = req("POST", f"{BASE_POST}/hackathons", {
    "title": "Unauthorized Hackathon",
    "description": "Should fail",
    "mode": "ONLINE",
    "startDate": "2026-11-01T10:00:00",
    "endDate": "2026-11-03T18:00:00",
    "registrationDeadline": "2026-10-31T23:59:59"
}, token=s1_token)
print(f"Status: {status} (Expected 403) | Response: {body}")
assert status == 403, f"Expected 403, got {status}"

# TEST 2: Organizer creates hackathon
print("\n--- TEST 2: Organizer creates Hackathon ---")
hackathon_payload = {
    "title": "Pulse AI & Cloud Hackathon 2026",
    "description": "Build high-scale microservices and generative AI solutions",
    "mode": "HYBRID",
    "venue": "Bangalore Innovation Center / Virtual",
    "websiteUrl": "https://pulse.dev/hackathon2026",
    "startDate": "2026-11-01T10:00:00",
    "endDate": "2026-11-03T18:00:00",
    "registrationDeadline": "2026-10-31T23:59:59",
    "minTeamSize": 1,
    "maxTeamSize": 2
}
status, body = req("POST", f"{BASE_POST}/hackathons", hackathon_payload, token=org_token)
print(f"Status: {status} (Expected 201)")
assert status == 201, f"Expected 201, got {status}: {body}"
hackathon_id = body["id"]
print(f"Created Hackathon ID: {hackathon_id}")
print(f"Title: {body['title']} | MaxTeamSize: {body['maxTeamSize']} | Status: {body['status']}")

# TEST 3: Browse all Hackathons
print("\n--- TEST 3: Browse Hackathons ---")
status, body = req("GET", f"{BASE_POST}/hackathons", token=s1_token)
print(f"Status: {status} (Expected 200) | Found {len(body)} hackathons")
assert status == 200

# TEST 4: Get Hackathon Details
print("\n--- TEST 4: Get Hackathon Details ---")
status, body = req("GET", f"{BASE_POST}/hackathons/{hackathon_id}", token=s1_token)
print(f"Status: {status} (Expected 200) | Reg Count: {body['registrationCount']}")
assert status == 200
assert body["registrationCount"] == 0

# TEST 5: Organizer view own hackathons
print("\n--- TEST 5: Organizer view My Hackathons ---")
status, body = req("GET", f"{BASE_POST}/hackathons/my", token=org_token)
print(f"Status: {status} (Expected 200) | Organizer Hackathons: {len(body)}")
assert status == 200

# TEST 6: Student 1 registers with team 'CyberKnights'
print("\n--- TEST 6: Student 1 registers with team 'CyberKnights' ---")
status, body = req("POST", f"{BASE_POST}/hackathons/{hackathon_id}/register", {
    "teamName": "CyberKnights"
}, token=s1_token)
print(f"Status: {status} (Expected 201) | Reg ID: {body['id']} | Team: {body['teamName']} | Status: {body['status']}")
assert status == 201
s1_reg_id = body["id"]

# TEST 7: Student 1 registers again (Duplicate check)
print("\n--- TEST 7: Duplicate Registration Check ---")
status, body = req("POST", f"{BASE_POST}/hackathons/{hackathon_id}/register", {
    "teamName": "CyberKnights"
}, token=s1_token)
print(f"Status: {status} (Expected 400) | Response: {body}")
assert status == 400

# TEST 8: Student 2 joins team 'CyberKnights' (Capacity becomes 2/2)
print("\n--- TEST 8: Student 2 joins team 'CyberKnights' ---")
status, body = req("POST", f"{BASE_POST}/hackathons/{hackathon_id}/register", {
    "teamName": "CyberKnights"
}, token=s2_token)
print(f"Status: {status} (Expected 201) | Reg ID: {body['id']} | Team: {body['teamName']}")
assert status == 201

# TEST 9: Student 3 joins 'CyberKnights' (Team Capacity Exceeded check)
print("\n--- TEST 9: Student 3 joins 'CyberKnights' (Capacity Exceeded Check) ---")
status, body = req("POST", f"{BASE_POST}/hackathons/{hackathon_id}/register", {
    "teamName": "CyberKnights"
}, token=s3_token)
print(f"Status: {status} (Expected 400) | Response: {body}")
assert status == 400, f"Expected 400, got {status}: {body}"

# TEST 10: Query Team Roster for 'CyberKnights'
print("\n--- TEST 10: Query Team Roster ---")
status, body = req("GET", f"{BASE_POST}/hackathons/{hackathon_id}/teams/CyberKnights", token=s1_token)
print(f"Status: {status} (Expected 200) | Team Members: {len(body)}")
assert status == 200
assert len(body) == 2

# TEST 11: Student 1 views My Registrations
print("\n--- TEST 11: Student 1 My Registrations ---")
status, body = req("GET", f"{BASE_POST}/hackathons/my-registrations", token=s1_token)
print(f"Status: {status} (Expected 200) | My Registrations: {len(body)}")
assert status == 200
assert len(body) == 1

# TEST 12: Organizer views all registrations
print("\n--- TEST 12: Organizer views registrations ---")
status, body = req("GET", f"{BASE_POST}/hackathons/{hackathon_id}/registrations", token=org_token)
print(f"Status: {status} (Expected 200) | Total Registrations: {len(body)}")
assert status == 200
assert len(body) == 2

# TEST 13: Organizer awards 'WINNER' status to Student 1
print("\n--- TEST 13: Organizer updates registration status to 'WINNER' ---")
status, body = req("PUT", f"{BASE_POST}/hackathons/{hackathon_id}/registrations/{s1_reg_id}/status", {
    "status": "WINNER"
}, token=org_token)
print(f"Status: {status} (Expected 200) | Updated Status: {body['status']}")
assert status == 200
assert body["status"] == "WINNER"

# TEST 14: Student 1 creates post tagging hackathon as WINNER
print("\n--- TEST 14: Student creates post tagging hackathon ---")
status, body = req("POST", f"{BASE_POST}/posts", {
    "content": "Thrilled to share that team CyberKnights won 1st Place at Pulse AI Hackathon 2026! Big thanks to the organizers!",
    "hackathonId": hackathon_id,
    "hackathonTagType": "WINNER"
}, token=s1_token)
print(f"Status: {status} | Body: {body}")
assert status == 201, f"Failed with {status}: {body}"
print(f"Post ID: {body['id']} | Tagged Hackathon: {body['hackathonId']} | Tag Type: {body['hackathonTagType']}")
assert body["hackathonId"] == hackathon_id
assert body["hackathonTagType"] == "WINNER"

# TEST 15: Showcase feed of posts tagged with this hackathon
print("\n--- TEST 15: Showcase feed of posts tagged with Hackathon ---")
status, body = req("GET", f"{BASE_POST}/hackathons/{hackathon_id}/posts", token=s1_token)
print(f"Status: {status} (Expected 200) | Tagged Posts: {len(body)}")
assert status == 200
assert len(body) >= 1
print(f"Post snippet: \"{body[0]['content'][:60]}...\"")

# TEST 16: Organizer updates hackathon details
print("\n--- TEST 16: Organizer updates hackathon ---")
status, body = req("PUT", f"{BASE_POST}/hackathons/{hackathon_id}", {
    "title": "Pulse AI & Cloud Hackathon 2026 [WINNERS ANNOUNCED]"
}, token=org_token)
print(f"Status: {status} (Expected 200) | New Title: {body['title']}")
assert status == 200
assert "WINNERS ANNOUNCED" in body["title"]

print("\n" + "=" * 60)
print("ALL 16 HACKATHON ENDPOINT TESTS PASSED WITH 100% SUCCESS!")
print("=" * 60)
