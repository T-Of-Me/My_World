import os
import json
import sqlite3
import secrets
import datetime
import threading
from flask import Flask, render_template, request, redirect, url_for, session, flash, abort, jsonify
from werkzeug.security import generate_password_hash, check_password_hash

from .llm import run_admin_assistant
from .db import (
	get_db_connection,
	init_db_if_needed,
	init_public_db_if_needed,
	get_global_message,
	unsafe_set_global_message,
	get_public_health,
)

app = Flask(__name__)
app.secret_key = os.getenv("FLASK_SECRET_KEY", secrets.token_hex(32))

DATABASE_PATH = os.getenv("DATABASE_PATH", "/data/app.db")
PUBLIC_DATABASE_PATH = os.getenv("PUBLIC_DATABASE_PATH", "/data/public.db")
ADMIN_USERNAME = os.getenv("ADMIN_USERNAME", "admin")
ADMIN_TRIGGER_MESSAGE = os.getenv("ADMIN_TRIGGER_MESSAGE", "What is the current system health?")
SITE_HEALTH_JSON = os.getenv("SITE_HEALTH_JSON", '{"status":"ok","uptime_seconds":12345,"version":"1.0.0"}')
INITIAL_GLOBAL_MESSAGE = os.getenv("INITIAL_GLOBAL_MESSAGE", "Welcome to King of the Board! Post your message to be seen by all.")
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD")
FLAG = os.getenv("FLAG", "crew{test_flag}")

for path in (DATABASE_PATH, PUBLIC_DATABASE_PATH):
	try:
		if os.path.exists(path):
			os.remove(path)
	except Exception:
		pass

init_db_if_needed(DATABASE_PATH)
init_public_db_if_needed(PUBLIC_DATABASE_PATH, INITIAL_GLOBAL_MESSAGE, SITE_HEALTH_JSON)


def ensure_admin_exists():
	conn = get_db_connection(DATABASE_PATH)
	try:
		cur = conn.execute("SELECT id, is_admin FROM users WHERE username = ?", (ADMIN_USERNAME,))
		row = cur.fetchone()
		if row is None:
			plain_password = ADMIN_PASSWORD or secrets.token_urlsafe(12)
			password_hash = generate_password_hash(plain_password)
			conn.execute(
				"INSERT INTO users (username, password_hash, is_admin, created_at) VALUES (?,?,1,?)",
				(ADMIN_USERNAME, password_hash, datetime.datetime.utcnow().isoformat()),
			)
			conn.commit()
			try:
				app.logger.info(f"[INIT] Created admin '{ADMIN_USERNAME}' account")
			except Exception:
				pass
		else:
			if row["is_admin"] != 1:
				conn.execute("UPDATE users SET is_admin = 1 WHERE id = ?", (row["id"],))
			if ADMIN_PASSWORD:
				password_hash = generate_password_hash(ADMIN_PASSWORD)
				conn.execute("UPDATE users SET password_hash = ? WHERE id = ?", (password_hash, row["id"]))
			conn.commit()
			try:
				app.logger.info(f"[INIT] Ensured admin '{ADMIN_USERNAME}' privileges/password")
			except Exception:
				pass
	finally:
		conn.close()


ensure_admin_exists()

# Single-process lock to ensure only one admin assistant run at a time
ADMIN_ASSISTANT_LOCK = threading.Lock()


def current_user():
	user_id = session.get("user_id")
	if not user_id:
		return None
	conn = get_db_connection(DATABASE_PATH)
	try:
		cur = conn.execute("SELECT id, username, is_admin FROM users WHERE id = ?", (user_id,))
		row = cur.fetchone()
		return dict(row) if row else None
	finally:
		conn.close()


def login_required(f):
	from functools import wraps
	@wraps(f)
	def wrapper(*args, **kwargs):
		if not current_user():
			return redirect(url_for('login', next=request.path))
		return f(*args, **kwargs)
	return wrapper


def admin_required(f):
	from functools import wraps
	@wraps(f)
	def wrapper(*args, **kwargs):
		user = current_user()
		if not user or not user.get("is_admin"):
			abort(403)
		return f(*args, **kwargs)
	return wrapper


@app.route("/")
def index():
	user = current_user()
	global_message = get_global_message(PUBLIC_DATABASE_PATH)
	return render_template("index.html", user=user, global_message=global_message, title="King of the Board")


@app.route("/home")
@login_required
def home():
	user = current_user()
	is_admin = bool(user and user.get("is_admin"))
	return render_template("home.html", user=user, is_admin=is_admin, flag=FLAG, title="Home")


@app.route("/register", methods=["GET", "POST"])
def register():
	if request.method == "POST":
		username = request.form.get("username", "").strip()
		password = request.form.get("password", "")
		if not username or not password:
			flash("Username and password are required", "error")
			return redirect(url_for("register"))
		if username == ADMIN_USERNAME:
			flash("This username is reserved", "error")
			return redirect(url_for("register"))
		conn = get_db_connection(DATABASE_PATH)
		try:
			password_hash = generate_password_hash(password)
			conn.execute(
				"INSERT INTO users (username, password_hash, is_admin, created_at) VALUES (?,?,0,?)",
				(username, password_hash, datetime.datetime.utcnow().isoformat()),
			)
			conn.commit()
			flash("Registered successfully. Please log in.", "success")
			return redirect(url_for("login"))
		except sqlite3.IntegrityError:
			flash("Username already exists", "error")
			return redirect(url_for("register"))
		finally:
			conn.close()
	return render_template("register.html")


@app.route("/login", methods=["GET", "POST"])
def login():
	if request.method == "POST":
		username = request.form.get("username", "")
		password = request.form.get("password", "")
		conn = get_db_connection(DATABASE_PATH)
		try:
			cur = conn.execute("SELECT id, username, password_hash, is_admin FROM users WHERE username = ?", (username,))
			row = cur.fetchone()
			if row and check_password_hash(row["password_hash"], password):
				session["user_id"] = row["id"]
				flash("Logged in", "success")
				next_url = request.args.get("next")
				if not next_url or not next_url.startswith("/"):
					next_url = url_for("index")
				return redirect(next_url)
			else:
				flash("Invalid credentials", "error")
		finally:
			conn.close()
	return render_template("login.html")


@app.route("/logout")
@login_required
def logout():
	session.pop("user_id", None)
	flash("Logged out", "success")
	return redirect(url_for("index"))


def _run_assistant_background():
	try:
		assistant_message = run_admin_assistant(get_all_messages_for_llm(), site_health_json=SITE_HEALTH_JSON, db_path=DATABASE_PATH)
		if assistant_message:
			append_message("assistant", assistant_message)
	finally:
		# Release the lock so another admin request can proceed
		try:
			ADMIN_ASSISTANT_LOCK.release()
		except Exception:
			pass


@app.route("/admin", methods=["GET"])  # chat view
@admin_required
def admin_chat():
	messages = get_admin_messages(limit=50)
	processing = any(m["role"] == "user" for m in messages) and not any(m["role"] == "assistant" for m in messages)
	return render_template("admin_chat.html", messages=messages, user=current_user(), processing=processing)


@app.route("/admin/send", methods=["POST"]) 
@admin_required
def admin_send():
	content = request.form.get("content", "").strip()
	if not content:
		return redirect(url_for("admin_chat"))
	# Ensure only one admin assistant run at a time
	if not ADMIN_ASSISTANT_LOCK.acquire(blocking=False):
		flash("Assistant is busy. Please wait for the current message to finish.", "error")
		return redirect(url_for("admin_chat"))
	# Reset conversation to last exchange: clear table and queue the message
	try:
		conn = get_db_connection(DATABASE_PATH)
		try:
			conn.execute("DELETE FROM messages")
			conn.commit()
		finally:
			conn.close()
		append_message("user", content)
		# Start background thread so request doesn't hang; lock released when done
		threading.Thread(target=_run_assistant_background, daemon=True).start()
		flash("Message queued. The assistant will reply shortly.", "success")
		return redirect(url_for("admin_chat"))
	except Exception:
		# On error, release the lock to avoid deadlock
		try:
			ADMIN_ASSISTANT_LOCK.release()
		except Exception:
			pass
		raise


@app.route("/solve", methods=["GET", "POST"]) 
def solve():
	print("Solve endpoint accessed")
	if request.method == "POST":
		# Ensure only one admin assistant run at a time
		if not ADMIN_ASSISTANT_LOCK.acquire(blocking=False):
			flash("Assistant is busy. Please try again later.", "error")
			return redirect(url_for("solve"))
		try:
			conn = get_db_connection(DATABASE_PATH)
			try:
				conn.execute("DELETE FROM messages")
				conn.commit()
			finally:
				conn.close()
			append_message("user", ADMIN_TRIGGER_MESSAGE)
			threading.Thread(target=_run_assistant_background, daemon=True).start()
			flash("Message sent.", "success")
			return redirect(url_for("solve"))
		except Exception:
			try:
				ADMIN_ASSISTANT_LOCK.release()
			except Exception:
				pass
			raise
	
	messages = get_admin_messages(limit=2)
	processing = any(m["role"] == "user" for m in messages) and not any(m["role"] == "assistant" for m in messages)
	completed = any(m["role"] == "assistant" for m in messages)
	return render_template("solve.html", trigger_message=ADMIN_TRIGGER_MESSAGE, processing=processing, completed=completed)


@app.route("/message", methods=["POST"]) 
@login_required
def update_global_message():
	new_content = request.form.get("content", "")
	if not new_content:
		flash("Message cannot be empty.", "error")
		return redirect(url_for("index"))
	try:
		unsafe_set_global_message(PUBLIC_DATABASE_PATH, new_content)
	except:
		flash("Error updating global message. Limit is 11 characters (printable only).", "error")
	else:
		flash("Global message updated.", "success")
	return redirect(url_for("index"))


def append_message(role: str, content: str):
	conn = get_db_connection(DATABASE_PATH)
	try:
		conn.execute(
			"INSERT INTO messages (role, content, created_at) VALUES (?,?,?)",
			(role, content, datetime.datetime.utcnow().isoformat()),
		)
		conn.commit()
	finally:
		conn.close()


def get_admin_messages(limit: int = 50):
	conn = get_db_connection(DATABASE_PATH)
	try:
		cur = conn.execute(
			"SELECT id, role, content, created_at FROM messages ORDER BY id DESC LIMIT ?",
			(limit,),
		)
		rows = [dict(row) for row in cur.fetchall()]
		rows.reverse()
		return rows
	finally:
		conn.close()


def get_all_messages_for_llm():
	messages = get_admin_messages(limit=100)
	# Prepend system prompt from environment
	system_prompt = os.getenv("SYSTEM_PROMPT", "You are an admin assistant.")
	return [{"role": "system", "content": system_prompt}] + [
		{"role": m["role"], "content": m["content"]} for m in messages
	]


@app.route("/healthz")
def healthz():
	return jsonify({"status": "ok"})