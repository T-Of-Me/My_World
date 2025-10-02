const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'nope';
const PYTHON_SERVER = process.env.PYTHON_SERVER || 'http://python-server:8080';

// In-memory user storage
const users = {};

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Generate request ID
const generateRequestId = () => {
    return Math.floor(Math.random() * 1e11).toString().padStart(11, '0');
};

// Middleware to add requestId to all responses
app.use((req, res, next) => {

    if (req.headers['x-request-id']) {
        req.requestId = req.headers['x-request-id'];
    } else {
    req.requestId = generateRequestId();
    }
    // Add request ID to response headers
    res.setHeader('X-Request-ID', req.requestId);
    
    // Log request with ID
    console.log(`[${new Date().toISOString()}] Request ${req.requestId}: ${req.method} ${req.path}`);
    
    next();
});

// JWT middleware
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ error: 'Access token required' });
    }

    jwt.verify(token, JWT_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({ error: 'Invalid or expired token' });
        }
        req.user = user;
        next();
    });
};

// Routes

// Home page
app.get('/', (req, res) => {
    res.json({
        message: 'CTF Challenge - Node.js + Python Server',
        service: 'nodejs-server',
        endpoints: {
            'POST /register': 'Register new user (username > 5 chars)',
            'POST /login': 'Login and get JWT token',
            'POST /action': 'Execute action on Python server (requires auth)',
            'GET /health': 'Health check'
        },
        instructions: [
            '1. Register with username > 5 characters',
            '2. Login to get JWT token',
            '3. Use /action endpoint with action=foo or action=readFlag'
        ]
    });
});

// Register endpoint
app.post('/register', async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({
            error: 'Username and password are required'
        });
    }

    if( typeof username !== 'string' || typeof password !== 'string' ) {
        return res.status(400).json({
            error: 'Username and password must be strings'
        });
    }

    // Validate username length (must be > 5 characters)
    if (username.length <= 5) {
        return res.status(400).json({
            error: 'Username must be longer than 5 characters'
        });
    }

    if (users[username]) {
        return res.status(400).json({
            error: 'User already exists'
        });
    }

    try {
        const hashedPassword = await bcrypt.hash(password, 10);
        users[username] = {
            username,
            password: hashedPassword,
            createdAt: new Date().toISOString()
        };

        res.json({
            message: 'User registered successfully',
            username: username,
            hint: 'Now you can login to get JWT token'
        });
    } catch (error) {
        res.status(500).json({
            error: 'Registration failed',
            message: error.message
        });
    }
});

// Login endpoint
app.post('/login', async (req, res) => {
    const { username, password } = req.body;

    if (!username || !password) {
        return res.status(400).json({
            error: 'Username and password are required'
        });
    }

    if(typeof username !== 'string' || typeof password !== 'string') {
        return res.status(400).json({
            error: 'Username and password must be strings'
        });
    }

    const user = users[username];
    if (!user) {
        return res.status(401).json({
            error: 'Invalid credentials'
        });
    }

    try {
        const validPassword = await bcrypt.compare(password, user.password);
        if (!validPassword) {
            return res.status(401).json({
                error: 'Invalid credentials'
            });
        }

        // Create JWT token
        const token = jwt.sign(
            { 
                username: user.username,
                iat: Math.floor(Date.now() / 1000)
            },
            JWT_SECRET,
            { expiresIn: '24h' }
        );

        res.json({
            message: 'Login successful',
            token: token,
            username: user.username,
            hint: 'Use this token in Authorization: Bearer <token> header'
        });
    } catch (error) {
        res.status(500).json({
            error: 'Login failed',
            message: error.message
        });
    }
});

// Action endpoint - proxy to Python server with form data
app.post('/action', authenticateToken, async (req, res) => {
    const { action } = req.body;
    
    if (!action) {
        return res.status(400).json({
            error: 'Action parameter is required',
            available_actions: ['foo', 'readFlag']
        });
    }

    try {
        // Create form data to send to Python server (to prevent param pollution)
        const formData = new FormData();
        formData.append('requestid', req.requestId);
        formData.append('action', action);
        formData.append('username', req.user.username);

        console.log(`[${new Date().toISOString()}] Proxying to Python server:`, {
            username: req.user.username,
            requestId: req.requestId,
            action: action
        });

        // Send request to Python server
        const response = await fetch(`${PYTHON_SERVER}/execute`, {
            method: 'POST',
            body: formData
        });

        const pythonData = await response.json();

        // Check if Python server returned an error
        if (!response.ok) {
            return res.status(response.status).json({
                error: 'Python server error',
                message: pythonData.message || pythonData.error || 'Unknown error',
                python_response: pythonData
            });
        }

        // Return response from Python server
        res.json({
            message: 'Action executed successfully',
            nodejs_info: {
                authenticated_user: req.user.username,
                request_id: req.requestId,
                action: action
            },
            python_response: pythonData
        });

    } catch (error) {
        console.error('Error calling Python server:', error.message);
        
        res.status(500).json({
            error: 'Request failed',
            message: error.message,
            details: 'Unable to connect to Python server'
        });
    }
});

// Health check
app.get('/health', (req, res) => {
    res.json({
        status: 'healthy',
        service: 'nodejs-server',
        requestId: req.requestId,
        timestamp: new Date().toISOString(),
        users_count: Object.keys(users).length,
        python_server: PYTHON_SERVER
    });
});

// Debug endpoint (for testing)
// app.get('/debug', authenticateToken, (req, res) => {
//     res.json({
//         message: 'Debug information',
//         authenticated_user: req.user,
//         available_users: Object.keys(users),
//         jwt_secret_hint: JWT_SECRET.substring(0, 10) + '...',
//         golang_server: GOLANG_SERVER
//     });
// });

// Error handler
app.use((err, req, res, next) => {
    console.error(`Request ${req.requestId}: ${err.stack}`);
    res.status(500).json({
        error: 'Internal server error',
        message: err.message,
        requestId: req.requestId
    });
});

// 404 handler
app.use((req, res) => {
    res.status(404).json({
        error: 'Not found',
        message: `Endpoint ${req.method} ${req.path} not found`,
        requestId: req.requestId
    });
});

app.listen(PORT, () => {
    console.log(`🚀 Node.js server running on port ${PORT}`);
    console.log(`🔗 Python server: ${PYTHON_SERVER}`);
    console.log(`🔑 JWT Secret: ${JWT_SECRET}`);
    console.log(`📝 Users registered: ${Object.keys(users).length}`);
});
