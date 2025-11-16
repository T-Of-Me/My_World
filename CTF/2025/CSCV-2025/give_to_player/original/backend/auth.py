import requests
from Crypto.Signature import pkcs1_15
from Crypto.PublicKey import RSA
from Crypto.Hash import SHA256
import libverifysso
import sys
import ctypes
import base64
import json
import time
import libverifysso

def b64decode(s):
    return base64.urlsafe_b64decode(s + "==")

def verify_signature(token):
    try:
        parts = token.split('.')
        if len(parts) != 3: return None
        headers = json.loads(b64decode(parts[0]).decode())
        claims = json.loads(b64decode(parts[1]).decode())
        kid = headers['kid']
        iss = claims['iss']
        jwks_uri = requests.get(f"{iss}/.well-known/openid-configuration", allow_redirects=False).json()['jwks_uri']
        keys = requests.get(jwks_uri, allow_redirects=False).json()['keys']
        for key in keys:
            if key['kid'] == kid:
                n = b64decode(key['n'])
                e = b64decode(key['e'])
                public_key = RSA.construct((int.from_bytes(n, byteorder='big'), int.from_bytes(e, byteorder='big')))
                signature = b64decode(parts[2])
                data = f"{parts[0]}.{parts[1]}".encode('utf-8')
                hash_data = SHA256.new(data)
                pkcs1_15.new(public_key).verify(hash_data, signature)
                return claims
    except Exception as e:
        print(f"verify_signature error: {e}", file=sys.stderr)
    return None

def handle_token(token, type):
    try:
        print(f"Calling handle_azure_token: {type} - {token}", file=sys.stderr)
        payload = verify_signature(token)
        print(payload, file=sys.stderr)
        user = 'guest'
        user = libverifysso.check_claims(payload, type)
        return user
    except Exception as e:
        print(f"handle_azure_token error: {e}", file=sys.stderr)