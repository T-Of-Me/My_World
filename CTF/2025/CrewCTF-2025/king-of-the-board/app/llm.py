import os
import json
import datetime
import sqlite3
from typing import List, Dict, Any
import openai
from openai import OpenAI
from werkzeug.security import generate_password_hash

from .db import get_db_connection, get_public_health


MODEL = os.getenv("LLM_MODEL", "gpt-5")
REASONING_EFFORT = os.getenv("REASONING_EFFORT", "medium")
# Set global client timeout
client = OpenAI(timeout=60.0)


def get_tools_spec() -> list:
	return [
		{
			"type": "function",
			"function": {
				"name": "reset_password",
				"description": "Reset a user's password to a new value.",
				"parameters": {
					"type": "object",
					"properties": {
						"username": {"type": "string", "description": "The username whose password to reset"},
						"new_password": {"type": "string", "description": "The new password to set"}
					},
					"required": ["username", "new_password"]
				}
			}
		},
		{
			"type": "function",
			"function": {
				"name": "get_site_health",
				"description": "Return the current site health information as JSON.",
				"parameters": {"type": "object", "properties": {}}
			}
		},
	]


def tool_reset_password(db_path: str, username: str, new_password: str) -> Dict[str, Any]:
	conn = get_db_connection(db_path)
	try:
		cur = conn.execute("SELECT id FROM users WHERE username = ?", (username,))
		row = cur.fetchone()
		if row is None:
			return {"ok": False, "error": "user_not_found"}
		password_hash = generate_password_hash(new_password)
		conn.execute("UPDATE users SET password_hash = ? WHERE id = ?", (password_hash, row["id"]))
		conn.commit()
		return {"ok": True, "username": username}
	finally:
		conn.close()


def tool_get_site_health(public_db_path: str) -> Dict[str, Any]:
	# Returns the public health record, possibly manipulated via injection later
	return get_public_health(public_db_path)


def _create_completion_with_reasoning(local_messages: List[Dict[str, str]]):
	try:
		return client.chat.completions.create(
			model=MODEL,
			messages=local_messages,
			tools=get_tools_spec(),
			tool_choice="auto",
			extra_body={"reasoning": {"effort": REASONING_EFFORT}},
			timeout=60.0,
		)
	except openai.BadRequestError:
		# Retry without reasoning if the model rejects the parameter
		return client.chat.completions.create(
			model=MODEL,
			messages=local_messages,
			tools=get_tools_spec(),
			tool_choice="auto",
			timeout=60.0,
		)


def run_admin_assistant(messages: List[Dict[str, str]], site_health_json: str, db_path: str) -> str:
	# messages: list of {'role': 'system'|'user'|'assistant'|'tool', 'content': str}
	# Loop to handle tool calls until we get a final assistant message
	local_messages = list(messages)
	public_db_path = os.getenv("PUBLIC_DATABASE_PATH", "/data/public.db")
	for _ in range(6):
		resp = _create_completion_with_reasoning(local_messages)
		msg = resp.choices[0].message
		if msg.tool_calls:
			# Append the assistant message that contains tool_calls first
			assistant_dict = {
				"role": "assistant",
				"content": msg.content or "",
				"tool_calls": [
					{
						"id": tc.id,
						"type": tc.type,
						"function": {
							"name": tc.function.name,
							"arguments": tc.function.arguments,
						},
					}
					for tc in msg.tool_calls
				],
			}
			local_messages.append(assistant_dict)

			# Then run tools and append tool responses
			for tool_call in msg.tool_calls:
				name = tool_call.function.name
				args = {}
				if tool_call.function.arguments:
					try:
						args = json.loads(tool_call.function.arguments)
					except Exception:
						args = {}
				if name == "reset_password":
					username = args.get("username", "")
					new_password = args.get("new_password", "")
					result = tool_reset_password(db_path, username, new_password)
				elif name == "get_site_health":
					result = tool_get_site_health(public_db_path)
				else:
					result = {"error": "unknown_tool"}
				local_messages.append({
					"role": "tool",
					"tool_call_id": tool_call.id,
					"name": name,
					"content": json.dumps(result),
				})
			continue
		# No tool calls -> final message
		return msg.content or ""
	return ""
