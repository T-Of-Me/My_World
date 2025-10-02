from flask import Flask, render_template, request, url_for, render_template_string, jsonify
import random
import re

app = Flask(__name__)
RANDOM_SEED=random.randint(0,50)
PORT = 4321

attack_types = {
    "normal_attack": {
        "name": "Normal Attack",
        "description": "Use your staff to bonk enemy 🪄",
        "damage": random.randint(1, 10),
        "type": "attack",
        "cooldown": 0
    },
    "power_of_friendship": {
        "name": "Power of Friendship",
        "description": "Friendship is power 🫶",
        "damage": random.randint(11, 30),
        "type": "attack",
        "cooldown": 3
    },
    "holy_heal": {
        "name": "Holy Heal",
        "description": "Heal a little hp ❤️",
        "damage": random.randint(10, 20),
        "type": "heal",
        "cooldown": 3
    }
}

def valid_template(template):
    pattern = r"^[a-zA-Z0-9 ]+$"    
    if not re.match(pattern, template):
        random.seed(RANDOM_SEED) 
        char_list = list(template)
        random.shuffle(char_list)
        template = ''.join(char_list)
    return template

def special_filter(user_input):
    simple_filter=["flag", "*", "\"", "'", "\\", "/", ";", ":", "~", "`", "+", "=", "&", "^", "%",
     "$", "#", "@", "!", "\n", "|", "import", "os", "request", "attr", "sys", "builtins", "class", 
     "subclass", "config", "json", "sessions", "self", "templat", "view", "wrapper", "test", "log", 
     "help", "cli", "blueprints", "signals", "typing", "ctx", "mro", "base", "url", "cycler", "get", 
     "join", "name", "g.", "lipsum", "application", "render"]
    for char_num in range(len(simple_filter)):
        if simple_filter[char_num] in user_input.lower():
            return False
    return True
    
@app.route("/")
def dashboard():
    return render_template("dashboard.html")

@app.route("/api/list_attack_types")
def list_attack_types():
    return jsonify(attack_types)

@app.route("/api/cast_attack")
def cast_attack():
    attack_name = request.args.get("attack_name", "")
    if attack_name in attack_types:
        attack = attack_types[attack_name]
        return jsonify(attack)
    else:
        try:
            attack_name=valid_template(attack_name)
            if not special_filter(attack_name):
                return jsonify({"error": "Creating magic is failed"}), 404
            template=render_template_string("<i>No magic name "+attack_name+ " here, try again!</i>")    
            return jsonify({"error": template}), 404
        except Exception as e:
            return jsonify({"error": "There is something wrong here: "+str(e)}), 404

if __name__ == "__main__":
    print(f"Random seed: {RANDOM_SEED}")
    app.run(host="0.0.0.0", port=PORT)