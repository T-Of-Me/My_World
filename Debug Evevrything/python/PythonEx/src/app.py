from flask import Flask, jsonify
import datetime, socket, sys, os

app = Flask(__name__)

@app.get("/")
def index():
    print("Request received")
    
    return jsonify({
        "message": "Hello from Flask in Docker!",
        "time": datetime.datetime.now().isoformat(),
        "hostname": socket.gethostname(),
        "python": sys.version.split()[0]
    })


if __name__ == "__main__":
    port = int(os.getenv("PORT", 8000))
    app.run(host="0.0.0.0", port=port, debug=True)
