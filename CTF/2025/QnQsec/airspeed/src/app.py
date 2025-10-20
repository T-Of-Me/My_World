import os
from flask import Flask, request
import airspeed

app = Flask(__name__)

# Airspeed loader for file-based templates and partials
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
TEMPLATES_DIR = os.path.join(BASE_DIR, 'templates')
CONTENT_DIR = os.path.join(BASE_DIR, 'static', 'content')
loader = airspeed.CachingFileLoader(TEMPLATES_DIR)

def _read_text_file(path: str) -> str:
    try:
        with open(path, 'r', encoding='utf-8') as f:
            return f.read()
    except Exception:
        return ''

LYRICS_TEXT = _read_text_file(os.path.join(CONTENT_DIR, 'lyrics.txt'))

@app.route('/')
def index():
    return render_vm('home.vm', {
        'title': 'Yung Lean Appreciation — Ginseng Strip 2002',
    })


def render_vm(template_name: str, context: dict | None = None) -> str:
    context = context or {}
    template = loader.load_template(template_name)
    return template.merge(context, loader=loader)


@app.route('/lean')
def lean_home():
    return render_vm('home.vm', {
        'title': 'Yung Lean Appreciation — Ginseng Strip 2002',
    })


@app.route('/lyrics')
def lyrics():
    return render_vm('lyrics.vm', {
        'title': 'Lyrics — Ginseng Strip 2002',
        'lyrics': LYRICS_TEXT,
    })


@app.route('/listen')
def listen():
    # Official video ID for embedding (subject to availability)
    youtube_id = 'vrQWhFysPKY'
    return render_vm('listen.vm', {
        'title': 'Listen — Ginseng Strip 2002',
        'youtube_id': youtube_id,
    })


@app.route('/about')
def about():
    return render_vm('about.vm', {
        'title': 'About — Yung Lean & The Track',
    })

@app.route('/debug', methods=['POST'])
def debug():
    name = request.json.get('name', 'World')
    return airspeed.Template(f"Hello, {name}").merge({})


@app.errorhandler(404)
def not_found(e):
    return render_vm('404.vm', {'title': 'Not Found'}), 404


if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)