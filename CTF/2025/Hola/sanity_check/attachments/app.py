from flask import Flask, render_template, request, jsonify, session, redirect, url_for
import os
from functools import wraps

app = Flask(__name__)

app.secret_key=os.urandom(32)

PORT=5000
NUMBER_OF_BITS = 32*16
# Check if flag.txt exists and read the flag from it, else use a default flag
FLAG = open('flag.txt', 'r').read().strip() if os.path.exists('flag.txt') and open('flag.txt', 'r').read().strip().startswith("HOLACTF{") else 'HOLACTF{this_is_a_flag}'

default_data = '0'*NUMBER_OF_BITS
os.makedirs('user', exist_ok=True)

def is_valid_input(input):
    """Check if input is valid or not"""
    if input == '' or len(input) != NUMBER_OF_BITS:
        return False
    try:
        for char in input:
            if int(char) != 0 and int(char) != 1:
                return False
    except ValueError:
        return False
    return True

def save_to_file(string, filename):
    """Save data to a file"""
    with open(filename, "w", encoding="utf-8") as file:
        file.write(str(string))


def read_file(filename):
    """Read data from a file"""
    with open(filename, 'r', encoding="utf-8") as f:
        data=f.read()
        return data

def is_User_Exist(f):
    @wraps(f)
    def wrapper(*args, **kwargs):
        if 'username' not in session:
            return redirect(url_for('index'))
        filename = get_user_filename()
        if not os.path.exists(filename):
            return redirect(url_for('index'))
        return f(*args, **kwargs)
    return wrapper

def get_user_filename():
    """Get user filename from session"""
    return 'user/'+os.path.basename(session.get('username'))+'.txt'

@app.route('/', methods=['GET','POST'])
def index():
    os.makedirs("user", exist_ok=True)
    if request.method == 'POST':
        session['username'] = request.form['username']
        save_to_file(default_data, get_user_filename())
        return redirect(url_for('dashboard'))
    return render_template('index.html')

@app.route('/dashboard', methods=['GET'])
@is_User_Exist
def dashboard():
    try:
        data=read_file(get_user_filename())
    except Exception as e:
        return jsonify({'error':str(e)})

    return render_template('dashboard.html', data=data, max_bits=NUMBER_OF_BITS)

@app.route('/update', methods=['POST'])
@is_User_Exist
def update():
    try:
        data = request.json
        if(not is_valid_input(data['data'])):
            return jsonify({'error':'Invalid input'})
        save_to_file(data['data'], get_user_filename())
        return jsonify({'status': 'updated', 'new_state': data['data']})
    except Exception as e:
        return jsonify({'error':e})

@app.route('/get_flag', methods=['GET'])
@is_User_Exist
def get_flag():
    data=read_file(get_user_filename())
    response = FLAG if "Holactf" in data else "No :v"
    return jsonify({'flag': response})

@app.route('/logout', methods=['GET'])
@is_User_Exist
def logout():
    if(os.path.exists(get_user_filename())):
        os.remove(get_user_filename())
    session.pop('username', None)
    return redirect(url_for('index'))

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=PORT)

