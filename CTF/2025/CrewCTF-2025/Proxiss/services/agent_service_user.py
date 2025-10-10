from flask import Flask, request
import os
import gzip
import zlib
import io

app = Flask(__name__)

@app.before_request
def decompress_request():
    encoding = request.headers.get("Content-Encoding", "").lower()
    if encoding in ("gzip", "deflate"):
        raw_data = request.get_data()
        try:
            if encoding == "gzip":
                data = gzip.GzipFile(fileobj=io.BytesIO(raw_data)).read()
            elif encoding == "deflate":
                try:
                    data = zlib.decompress(raw_data)
                except zlib.error:
                    data = zlib.decompress(raw_data, -zlib.MAX_WBITS)
            request._cached_data = data
            request.environ["wsgi.input"] = io.BytesIO(data)
            request.content_length = len(data)
        except Exception as e:
            return f"Failed to decompress request body: {e}", 400

@app.route("/<path:path>", methods=["GET", "POST"])
def hello(path):
    if request.method == "POST":
        message = request.data
        print(f"Received message: {message}", flush=True)
        if b"GIVE ME FLAG!" in message:
            return "OK HERE IS YOUR FLAG: " + os.getenv("FLAG1", "Fake flag 1")
    return "Hello from User!"

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("USER_PORT", 1337)))
