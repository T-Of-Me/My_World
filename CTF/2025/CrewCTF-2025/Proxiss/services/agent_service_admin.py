from flask import Flask
import os
app = Flask(__name__)

@app.route("/admin")
def hello():
    return "I am admin, here is your flag: " + os.getenv("FLAG2", "Fake flag 2")

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.getenv("ADMIN_PORT", 3000)))
