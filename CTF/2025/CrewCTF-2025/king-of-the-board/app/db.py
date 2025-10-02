import os
import sqlite3
import json
import string

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    is_admin INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    role TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TEXT NOT NULL
);
"""

PUBLIC_SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS global_info (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    global_message TEXT NOT NULL,
    status TEXT NOT NULL,
    uptime_seconds INTEGER NOT NULL,
    db TEXT NOT NULL,
    version TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
"""

def get_db_connection(db_path: str) -> sqlite3.Connection:
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    return conn

def init_db_if_needed(db_path: str):
    os.makedirs(os.path.dirname(db_path), exist_ok=True)
    conn = get_db_connection(db_path)
    try:
        conn.executescript(SCHEMA_SQL)
        conn.commit()
    finally:
        conn.close()

def init_public_db_if_needed(db_path: str, initial_message: str, initial_health_json: str):
    os.makedirs(os.path.dirname(db_path), exist_ok=True)
    conn = get_db_connection(db_path)
    try:
        conn.executescript(PUBLIC_SCHEMA_SQL)

        cur = conn.execute("SELECT COUNT(*) AS c FROM global_info")
        if cur.fetchone()[0] == 0:
            try:
                data = json.loads(initial_health_json)
            except Exception:
                data = {"status": "ok", "uptime_seconds": 0, "db": "ok", "version": "1.0.0"}
            conn.execute(
                "INSERT INTO global_info (id, global_message, status, uptime_seconds, db, version, updated_at) VALUES (1, ?, ?, ?, ?, ?, datetime('now'))",
                (
                    str(data.get("global_message", initial_message)),
                    str(data.get("status", "ok")),
                    int(data.get("uptime_seconds", 0)),
                    str(data.get("db", "ok")),
                    str(data.get("version", "1.0.0")),
                ),
            )
        conn.commit()
    finally:
        conn.close()

def get_global_message(db_path: str) -> str:
    conn = get_db_connection(db_path)
    try:
        cur = conn.execute("SELECT global_message FROM global_info WHERE id = 1")
        row = cur.fetchone()
        return row[0] if row else ""
    finally:
        conn.close()

def unsafe_set_global_message(db_path: str, new_content: str) -> None:
    assert len(new_content) <= 11 and all(c in string.printable for c in new_content), "Invalid content"
    conn = get_db_connection(db_path)
    try:
        conn.execute(f"UPDATE global_info SET global_message = '{new_content}' WHERE id = 1")
        conn.commit()
    finally:
        conn.close()

def get_public_health(db_path: str) -> dict:
    conn = get_db_connection(db_path)
    try:
        cur = conn.execute("SELECT status, uptime_seconds, db, version, updated_at FROM global_info WHERE id = 1")
        row = cur.fetchone()
        if not row:
            return {}
        return {
            "status": row["status"],
            "uptime_seconds": row["uptime_seconds"],
            "db": row["db"],
            "version": row["version"],
            "updated_at": row["updated_at"],
        }
    finally:
        conn.close()