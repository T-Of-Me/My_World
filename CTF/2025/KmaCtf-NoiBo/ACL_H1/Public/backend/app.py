from flask import Flask, request, render_template, Response, send_from_directory, session, render_template_string, redirect, url_for
import logging
import sys
import os
import uuid
import secrets

app = Flask(__name__, template_folder=os.path.join(os.path.dirname(__file__), 'templates'))
app.secret_key = secrets.token_hex(32)

BASE_UPLOAD_FOLDER = 'uploads'

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

ALLOWED_EXTENSIONS = {'txt', 'html'}
def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

def get_session_folder():
    if "session_id" not in session:
        session["session_id"] = uuid.uuid4().hex[:6]
    folder = os.path.join(BASE_UPLOAD_FOLDER, session["session_id"])
    os.makedirs(folder, exist_ok=True)
    return folder, session["session_id"]

@app.before_request
def log_request_info():
    logger.info(f"REQUEST: {request.method} {request.path}")
    if 'folder' not in session:
        folder_name = uuid.uuid4().hex
        session['folder'] = folder_name
        session_folder_path = os.path.join(BASE_UPLOAD_FOLDER, folder_name)
        os.makedirs(session_folder_path, exist_ok=True)
    else:
        session_folder_path = os.path.join(BASE_UPLOAD_FOLDER, session['folder'])
    app.config['UPLOAD_FOLDER'] = session_folder_path

# --- Routes ---
@app.route('/', methods=['GET'])
def index():
    return render_template('index.html', method=request.method, path=request.path)

@app.route('/upload', methods=['GET', 'POST'])
def upload_file():
    if request.method == 'POST':
        if 'file' not in request.files:
            return "No file part"
        file = request.files['file']
        if file.filename == '':
            return "No selected file"
        if file and allowed_file(file.filename):
            ext = file.filename.rsplit('.', 1)[1].lower()
            random_str = uuid.uuid4().hex[:16]
            filename = f"{random_str}.{ext}"
            filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
            file.save(filepath)
            return f"File uploaded successfully! Path: {filepath} <a href='/files'>See all files</a>"
        return "File type not allowed"
    return render_template("upload.html")

@app.route('/uploads/<folder>/<filename>')
def download_file(folder, filename):
    folder_path = os.path.join(BASE_UPLOAD_FOLDER, folder)
    return send_from_directory(
        folder_path,
        filename,
        mimetype='text/plain',
        as_attachment=False
    )

@app.route('/files', methods=['GET'])
def list_files():
    folder = session.get('folder')
    if not folder:
        folder = uuid.uuid4().hex
        session['folder'] = folder

    folder_path = os.path.join(BASE_UPLOAD_FOLDER, folder)
    os.makedirs(folder_path, exist_ok=True) 

    files = os.listdir(folder_path)
    file_urls = [f"uploads/{folder}/{f}" for f in files]
    return render_template("files.html", files=zip(files, file_urls))

# Internal access to render
@app.route('/render')
def render_file():
    filepath = request.args.get("filepath", "")
    if not os.path.isfile(filepath):
        return "File not found", 404
    with open(filepath) as f:
        content = f.read()
    return render_template_string(f"<pre>{ content }</pre>")

# --- Error Handlers ---
@app.errorhandler(404)
def not_found(error):
    logger.error(f"404 Error: Path '{request.path}' not found")
    return f"404 Not Found: The path '{request.path}' does not exist.", 404

@app.errorhandler(403)
def access_denied(error):
    logger.error("403 Error: Access Denied")
    return "403 Forbidden: Access Denied", 403

@app.errorhandler(400)
def bad_request(error):
    logger.error("400 Error: Bad Request")
    return "400 Bad Request", 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8088, debug=False)
