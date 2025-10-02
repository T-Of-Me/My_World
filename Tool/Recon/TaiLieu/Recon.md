# IP Ranges 
- [Tìm trong này](https://bgp.he.net/)
- ![alt text](image.png)
=> từ tên domain lấy được ASN
# Reverse Lookup from ASN using amass
- Lấy domain và IP thuộc ASN
    - Thu thập intel (passive) — lấy domain và IP thuộc ASN
        - ![alt text](image-1.png)
        - `amass intel -asn 36459 | tee domains.txt`
# Finding Subdomains 
## Linked (spidering) 
- Dùng để tìm subdomain 
    - ![alt text](image-2.png)
    - `gospider -s https://gitbook.com --subs -d 2`
- Làm sạch tên miền 
    - ![alt text](image-3.png)
    - `$ gospider -s https://gitbook.com --subs -d 2 cat subs.txt | grep -F '[subdomains]' | cut -d'/' -f3 | sort -u | tee linked-subdomains.txt
www.gitbook.com
docs.gitbook.com
blog.gitbook.com`
## Scraping 
- **subfinder** gom nhiều kỹ thuật open-source (CT, Google, GitHub, v.v.) để trả về danh sách subdomain:
    - Chưa tải được mạng yếu vl 
    - `subfinder -d gitbook.com -o scraped-subdomains.txt`
## Brute Force
- [best-dns-wordlist](https://wordlists-cdn.assetnote.io/data/manual/best-dns-wordlist.txt)
- [DNS công bố](https://public-dns.info/nameservers.txt)
- Tìm DNS resolver 
    - ![alt text](image-5.png)
- Puredns Bruteforce
    - ![](image-6.png)
    - `puredns bruteforce best-dns-wordlist.txt gitbook.com -r resolvers.txt -w brute-subdomains.txt`

    