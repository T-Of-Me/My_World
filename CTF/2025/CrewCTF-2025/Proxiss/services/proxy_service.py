from flask import Flask, request, Response
import requests
import os

app = Flask(__name__)

TARGET_HOST = "10.0.0.5"
TARGET_PORT = int(os.getenv("USER_PORT", 1337))
ADMIN_PORT = int(os.getenv("ADMIN_PORT", 3000))

@app.route("/admin_check", methods=["GET"])
def admin_check():
    try:
        resp = requests.get(f"http://{TARGET_HOST}:{ADMIN_PORT}/admin", timeout=3)
        if "I am admin" in resp.text:
            return "OK"
        return "Forbidden", 403
    except requests.RequestException:
        return "Error contacting admin service", 500


@app.route("/", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
def home():
    return "Welcome to the Proxy Service!"

@app.route("/<path:path>", methods=["GET", "POST", "PUT", "DELETE", "PATCH"])
def proxy(path):
    try: 
        # Build target URL
        target_url = f"http://{TARGET_HOST}:{TARGET_PORT}/{path}"

        # Forward request method, headers, and body
        resp = requests.request(
            method=request.method,
            url=target_url,
            headers={key: value for key, value in request.headers if key.lower() != "host"},
            data=request.get_data(),
            cookies=request.cookies,
            allow_redirects=False
        )

        # Return response with original status code and headers
        excluded_headers = {"content-encoding", "content-length", "transfer-encoding", "connection"}
        headers = [(name, value) for name, value in resp.raw.headers.items() if name.lower() not in excluded_headers]

        return Response(resp.content, resp.status_code, headers)
    except Exception as e:
        return Response("Something went wrong", 500)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000)
