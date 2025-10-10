#!/usr/bin/env python3

from flask import Flask, request, jsonify
import os
import logging
from datetime import datetime

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# Environment variables
FLAG = os.getenv("FLAG", "KMACTF{REDACTED}")
PORT = int(os.getenv("PORT", "8080"))

def get_timestamp():
    """Get current timestamp in ISO format"""
    return datetime.utcnow().isoformat() + "Z"

def send_error_response(error, message, status_code=400):
    """Send error response"""
    return jsonify({
        "error": error,
        "message": message,
        "timestamp": get_timestamp()
    }), status_code

def process_action(username, action):
    """Process different actions"""
    if action == "foo":
        return "bar", "Action 'foo' executed successfully", None
    
    elif action == "readFlag":
        if username == "admin":
            return FLAG, "Flag retrieved successfully - you are admin!", None
        else:
            return "Access denied", f"Flag access denied for user '{username}' - admin privileges required", None
    
    else:
        return None, "", f"unknown action: {action}. Available actions: foo, readFlag"

@app.route('/execute', methods=['POST'])
def execute_handler():
    """Handle execute requests from Node.js server"""
    try:
        # Check if request has form data
        if not request.form:
            return send_error_response(
                "Form parsing failed", 
                "No form data found in request", 
                400
            )
        
        # Extract form values
        username = request.form.get('username', '').strip()
        request_id = request.form.get('requestid', '').strip()
        action = request.form.get('action', '').strip()
        
        # Log request for debugging
        logger.info(f"Received request - Username: {username}, RequestID: {request_id}, Action: {action}")
        
        # Validate required fields
        if not username or not request_id or not action:
            return send_error_response(
                "Missing required fields",
                "username, requestid, and action are required",
                400
            )
        
        # Process action
        result, message, error = process_action(username, action)
        if error:
            return send_error_response("Action processing failed", error, 400)
        
        # Send success response
        response = {
            "requestid": request_id,
            "action": action,
            "result": result,
            "username": username,
            "timestamp": get_timestamp(),
            "message": message
        }
        
        return jsonify(response), 200
        
    except Exception as e:
        logger.error(f"Execute handler error: {str(e)}")
        return send_error_response(
            "Internal server error",
            str(e),
            500
        )

@app.route('/health', methods=['GET'])
def health_handler():
    """Health check endpoint"""
    response = {
        "status": "healthy",
        "service": "python-server",
        "timestamp": get_timestamp(),
        "actions": ["foo", "readFlag"],
        "flag_hint": "readFlag action requires admin username"
    }
    return jsonify(response), 200

@app.route('/', methods=['GET'])
def info_handler():
    """Server information endpoint"""
    response = {
        "message": "CTF Challenge - Python Flag Server",
        "service": "python-server",
        "timestamp": get_timestamp(),
        "endpoints": {
            "POST /execute": "Execute action (requires form data: username, requestid, action)",
            "GET /health": "Health check",
            "GET /": "Server information"
        },
        "actions": {
            "foo": "Returns 'bar'",
            "readFlag": "Returns flag if username is 'admin'"
        },
        "security_info": {
            "form_data": "Uses form-data to prevent parameter pollution",
            "admin_required": "Flag access requires username='admin'",
            "request_logging": "All requests are logged for debugging"
        }
    }
    return jsonify(response), 200

@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors"""
    return send_error_response(
        "Not found",
        f"Endpoint not found",
        404
    )

@app.errorhandler(405)
def method_not_allowed(error):
    """Handle 405 errors"""
    return send_error_response(
        "Method not allowed",
        f"Method {request.method} is not allowed for this endpoint",
        405
    )

@app.errorhandler(500)
def internal_error(error):
    """Handle 500 errors"""
    return send_error_response(
        "Internal server error",
        "An unexpected error occurred",
        500
    )

if __name__ == '__main__':
    logger.info(f"🚀 Python server starting on port {PORT}")
    logger.info(f"🎯 Flag: {FLAG}")
    logger.info(f"🔒 Admin username required for flag: 'admin'")
    logger.info(f"⚡ Available actions: foo -> bar, readFlag -> flag (admin only)")
    
    # Run Flask app
    app.run(
        host='0.0.0.0',
        port=PORT,
        debug=False,
        threaded=True
    )
