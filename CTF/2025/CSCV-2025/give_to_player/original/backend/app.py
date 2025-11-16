from flask import Flask, request, jsonify, render_template
from auth import handle_token
import sqlite3
import hmac
import os
import sys
import datetime

app = Flask(__name__)
HMAC_SECRET = 'SuperS3cur3'

# Custom Jinja2 filter for timestamp formatting
@app.template_filter('timestamp_to_date')
def timestamp_to_date(timestamp):
    return datetime.datetime.fromtimestamp(timestamp).strftime('%Y-%m-%d %H:%M')

# Create template directory if not exists
TEMPLATE_DIR = os.path.join(os.path.dirname(__file__), 'uploads')
os.makedirs(TEMPLATE_DIR, exist_ok=True)

DATABASE = 'data/users.db'

def get_db():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    return conn

def run_query(query, args=None, commit=False):
    conn = get_db()
    cursor = conn.cursor()
    
    if args:
        cursor.execute(query, args)
    else:
        cursor.execute(query)

    if commit:
        conn.commit()
    else:
        results = cursor.fetchall()
        conn.close()
        return results

@app.post('/login')
def login():
    username = request.form.get('username')
    password = request.form.get('password')
    type = request.form.get('type')
    token = request.form.get('token')

    if not username:
        return jsonify({"error": "Missing username"}), 400

    exist = run_query("SELECT 1 FROM users WHERE username='?' LIMIT 1".replace('?', username))
    if not exist:
        run_query("INSERT INTO users (username, password) VALUES (?, '')", (username, ), True)

    if not type:
        return jsonify({"error": "Missing type"}), 400

    auth = False
    if type == 'password':
        if not exist:
            if not password:
                return jsonify({"error": "Missing password"}), 400
            pwd_hmac = hmac.new(HMAC_SECRET.encode(), password.encode(), 'sha256').hexdigest()
            run_query("UPDATE users SET password=? WHERE username=?", (pwd_hmac, username), True)
            auth = username
        else:
            if not password:
                return jsonify({"error": "Missing password"}), 400
            pwd_hmac = hmac.new(HMAC_SECRET.encode(), password.encode(), 'sha256').hexdigest()
            user = run_query("SELECT password FROM users WHERE username=? LIMIT 1", (username, ))
            if user and hmac.compare_digest(user[0][0], pwd_hmac):
                auth = username

    if token:
        if type == 'azure' or type == 'google':
            auth = handle_token(token, type)
        if auth != username:
            return jsonify({"error": "Authentication error"}), 401        

    if auth:
        print(f"Login as: {auth}", file=sys.stderr)
        return jsonify({"username": username, "role": "admin" if username == "admin" else "guest"}), 200

    return jsonify({"error": "Authentication error"}), 401
        

@app.route('/admin', methods=['GET', 'POST'])
def admin():
    real_ip = request.headers.get('X-Real-IP')
    local_pass = 1
    if real_ip and real_ip != '127.0.0.1':
        local_pass = 0
    if local_pass == 1:
        print("Admin access granted (localhost)")
    else:
        gateway_user = request.headers.get('X-Gateway-User')
        
        if not gateway_user:
            return jsonify({"error": "Missing X-Gateway-User header - authentication required"}), 401
        if gateway_user != 'admin':
            return jsonify({"error": "Admin access required"}), 403
        
        print(f"Admin access granted for gateway user: {gateway_user}")
    
    if request.method == 'GET':
        # List files in uploads directory for file manager UI
        files = []
        if os.path.exists(TEMPLATE_DIR):
            for filename in os.listdir(TEMPLATE_DIR):
                filepath = os.path.join(TEMPLATE_DIR, filename)
                if os.path.isfile(filepath):
                    file_stat = os.stat(filepath)
                    files.append({
                        'name': filename,
                        'size': file_stat.st_size,
                        'modified': file_stat.st_mtime
                    })
        
        return render_template('file_manager.html', files=files, upload_dir=TEMPLATE_DIR)
    
    # POST request - handle file upload (original logic)
    uploaded_files = []
    if request.files:
        for field_name in request.files:
            file = request.files[field_name]
            if file and file.filename:
                filename = file.filename
                print(f"Processing file upload: field={field_name}, filename={filename}")
                filepath = os.path.join(TEMPLATE_DIR, filename)
                
                try:
                    file.save(filepath)
                    file_size = os.path.getsize(filepath)
                    uploaded_files.append({
                        'field_name': field_name,
                        'filename': filename,
                        'size': file_size,
                        'saved_path': filepath
                    })
                    print(f"File uploaded: {filename} ({file_size} bytes)")
                except Exception as e:
                    print(f"Error saving file {filename}: {e}")
                    return jsonify({"error": f"Failed to save file {filename}: {str(e)}"}), 500
    
    form_data = dict(request.form) if request.form else {}
    
    return jsonify({
        "message": "File upload processed",
        "uploaded_files": uploaded_files,
        "form_data": form_data,
        "template_dir": TEMPLATE_DIR,
        "real_ip": real_ip
    })

@app.route('/uploads')
def serve_uploaded_file():
    from flask import send_from_directory
    filename = request.args.get('filename')
    if not filename:
        return jsonify({"error": "Missing filename parameter"}), 400
    return send_from_directory(TEMPLATE_DIR, filename)

@app.route('/', methods=['GET', 'POST', 'PUT', 'PATCH', 'DELETE'])
def catch_all():
    return jsonify({"message": len(render_template('./home.html', user="Alice")) })

if __name__ == '__main__':
    if not os.path.exists(DATABASE):
        with sqlite3.connect(DATABASE) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                CREATE TABLE users (
                    username VARCHAR(255) NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    PRIMARY KEY (username)
                );
            ''')

            cursor.execute('''
                INSERT INTO users VALUES ("admin", "24a4b7c9c2e40e2b2bffc829dbc9442cf8c2322fec13ac19ae682983e9a1095b");
            ''')
            conn.commit()

    app.run(host='0.0.0.0', port=5000, debug=False)
