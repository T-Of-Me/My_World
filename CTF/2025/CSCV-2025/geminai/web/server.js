require('dotenv').config();
const express = require('express');
const session = require('express-session');
const pgSession = require('connect-pg-simple')(session);
const { Pool } = require('pg');
const path = require('path');
const fs = require('fs');
const multer = require('multer');
const bcrypt = require('bcrypt');
const { v4: uuidv4 } = require('uuid');

const app = express();
const PORT = process.env.PORT || 3000;
const BACKEND_URL = process.env.BACKEND_URL || 'http://backend';

// Ensure uploads directory exists
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
  fs.mkdirSync(uploadsDir, { recursive: true });
}

// Database connection
const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'geminai_db',
  user: process.env.DB_USER || 'geminai_user',
  password: process.env.DB_PASSWORD || 'geminai_password',
});

// Handle database connection errors
pool.on('error', (err) => {
  console.error('Unexpected error on idle client', err);
});

// Check database health/connectivity
async function checkDatabaseHealth(maxRetries = 10, retryDelay = 2000) {
  for (let i = 0; i < maxRetries; i++) {
    try {
      const result = await pool.query('SELECT NOW()');
      console.log('Database is ready!');
      return true;
    } catch (error) {
      console.log(`Database not ready yet (attempt ${i + 1}/${maxRetries})...`);
      if (i < maxRetries - 1) {
        await new Promise(resolve => setTimeout(resolve, retryDelay));
      } else {
        console.error('Database connection failed after all retries:', error);
        return false;
      }
    }
  }
  return false;
}

// Initialize database tables
async function initDatabase() {
  try {
    // Generated flag table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS flag (
        flag VARCHAR(255) UNIQUE NOT NULL
      )
    `);
    // Generated users table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        username VARCHAR(255) UNIQUE NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL,
        password VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Sessions table (for connect-pg-simple)
    await pool.query(`
      CREATE TABLE IF NOT EXISTS session (
        sid VARCHAR NOT NULL COLLATE "default",
        sess JSON NOT NULL,
        expire TIMESTAMP(6) NOT NULL,
        CONSTRAINT session_pkey PRIMARY KEY (sid)
      )
    `);
    await pool.query(`
      CREATE INDEX IF NOT EXISTS IDX_session_expire ON session(expire)
    `);

    // Generated ai_models table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS ai_models (
        id SERIAL PRIMARY KEY,
        model_id VARCHAR(50) UNIQUE NOT NULL,
        name VARCHAR(255) NOT NULL,
        available BOOLEAN DEFAULT false,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Generated generated_images table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS generated_images (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        original_image VARCHAR(255),
        generated_image VARCHAR(255),
        model_name VARCHAR(255),
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Generated generated_logs table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS generated_logs (
        id SERIAL PRIMARY KEY,
        request_id VARCHAR(255) UNIQUE NOT NULL,
        trace_id VARCHAR(255),
        user_id VARCHAR(255),
        model_id VARCHAR(255),
        model_name VARCHAR(255),
        original_image VARCHAR(255),
        generated_image VARCHAR(255),
        total_time INTEGER,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    await pool.query(`
      CREATE INDEX IF NOT EXISTS IDX_generated_logs_request_id ON generated_logs(request_id)
    `);

    // Insert flag
    await pool.query(`
      INSERT INTO flag (flag) VALUES ('CSCV2025{This_is_a_fake_flag!}') ON CONFLICT (flag) DO NOTHING
    `);

    const verifyFlag = await pool.query('SELECT COUNT(*) as count FROM flag');
    console.log('Flags in database:', verifyFlag.rows[0].count);

    // Insert default AI models
    await pool.query(`
      INSERT INTO ai_models (model_id, name, available) 
      VALUES 
        ('geminai-o3', 'Geminai O3', true),
        ('geminai-o4-mini', 'Geminai O4 Mini', false),
        ('geminai-4.5', 'Geminai 4.5', false),
        ('geminai-4o', 'Geminai 4O', false),
        ('geminai-4.1', 'Geminai 4.1', false)
      ON CONFLICT (model_id) DO NOTHING
    `);

    // Verify models were inserted
    const verifyResult = await pool.query('SELECT COUNT(*) as count FROM ai_models');
    console.log('AI Models in database:', verifyResult.rows[0].count);

    // Insert admin account
    await pool.query(`
      INSERT INTO users (username, email, password) VALUES ('admin', 'admin@geminai.io', '75170fc230cd88f32e475ff4087f81d9')
      ON CONFLICT (username) DO NOTHING
    `);

    const verifyAdmin = await pool.query('SELECT COUNT(*) as count FROM users WHERE username = $1', ['admin']);
    console.log('Admin accounts in database:', verifyAdmin.rows[0].count);

    console.log('Database initialized successfully');
  } catch (error) {
    console.error('Error initializing database:', error);
  }
}

function generateRandomString(length = 8) {
  return Math.random()
    .toString(32)
    .replace(/[^a-z0-9]+/g, '')
    .substring(0, length)
    .toUpperCase();
}

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

// Proxy /uploads/** to backend
app.get('/uploads/*', async (req, res) => {
  try {
    const filePath = req.path;

    const validPathPattern = /^\/uploads\/(request|result)-[a-f0-9]+\.png$/i;
    if (!validPathPattern.test(filePath)) {
      return res.status(403).send('Hacker detected!!!');
    }

    const backendUrl = `${BACKEND_URL}${filePath}`;

    const response = await fetch(backendUrl);

    if (!response.ok) {
      return res.status(response.status).send('File not found');
    }

    // Set appropriate content type
    const contentType = response.headers.get('content-type') || 'application/octet-stream';
    res.set('Content-Type', contentType);

    // Get file buffer from response
    const arrayBuffer = await response.arrayBuffer();
    const buffer = Buffer.from(arrayBuffer);
    res.send(buffer);
  } catch (error) {
    console.error('Proxy error:', error);
    res.status(500).send('Error proxying file');
  }
});

// Session configuration
app.use(session({
  store: new pgSession({
    pool: pool,
    tableName: 'session'
  }),
  secret: process.env.SESSION_SECRET || 'your-secret-key-change-in-production',
  resave: false,
  saveUninitialized: false,
  cookie: {
    secure: false, // Set to true in production with HTTPS
    maxAge: 30 * 24 * 60 * 60 * 1000 // 30 days
  }
}));

// View engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Multer configuration for file uploads (memory storage to get buffer)
const storage = multer.memoryStorage();

const upload = multer({
  storage: storage,
  limits: { fileSize: 1 * 1024 * 1024 }, // 1MB
  fileFilter: (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|gif|webp/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);

    if (mimetype && extname) {
      return cb(null, true);
    } else {
      cb(new Error('Only image files are allowed!'));
    }
  }
});

// Authentication middleware
const requireAuth = (req, res, next) => {
  if (req.session.userId) {
    next();
  } else {
    res.redirect('/login');
  }
};

// Routes
app.get('/', (req, res) => {
  if (req.session.userId) {
    res.redirect('/dashboard');
  } else {
    res.redirect('/login');
  }
});

app.get('/register', (req, res) => {
  if (req.session.userId) {
    return res.redirect('/dashboard');
  }
  res.render('register', { error: null });
});

app.post('/register', async (req, res) => {
  try {
    const { username, email, password } = req.body;

    if (!username || !email || !password) {
      return res.render('register', { error: 'All fields are required' });
    }

    if (password.length < 6) {
      return res.render('register', { error: 'Password must be at least 6 characters' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const result = await pool.query(
      'INSERT INTO users (username, email, password) VALUES ($1, $2, $3) RETURNING id, username, email',
      [username, email, hashedPassword]
    );

    req.session.userId = result.rows[0].id;
    req.session.username = result.rows[0].username;
    res.redirect('/dashboard');
  } catch (error) {
    if (error.code === '23505') { // Unique violation
      res.render('register', { error: 'Username or email already exists' });
    } else {
      console.error('Registration error:', error);
      res.render('register', { error: 'An error occurred during registration' });
    }
  }
});

app.get('/login', (req, res) => {
  if (req.session.userId) {
    return res.redirect('/dashboard');
  }
  res.render('login', { error: null });
});

app.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return res.render('login', { error: 'Username and password are required' });
    }

    const result = await pool.query('SELECT * FROM users WHERE username = $1', [username]);

    if (result.rows.length === 0) {
      return res.render('login', { error: 'Invalid username or password' });
    }

    const user = result.rows[0];
    const validPassword = await bcrypt.compare(password, user.password);

    if (!validPassword) {
      return res.render('login', { error: 'Invalid username or password' });
    }

    req.session.userId = user.id;
    req.session.username = user.username;
    res.redirect('/dashboard');
  } catch (error) {
    console.error('Login error:', error);
    res.render('login', { error: 'An error occurred during login' });
  }
});

app.get('/logout', (req, res) => {
  req.session.destroy((err) => {
    if (err) {
      console.error('Logout error:', err);
    }
    res.redirect('/login');
  });
});

app.get('/dashboard', requireAuth, async (req, res) => {
  try {
    // Get all AI models first
    let modelsResult;
    try {
      modelsResult = await pool.query(
        'SELECT * FROM ai_models ORDER BY id'
      );
      if (modelsResult.rows.length === 0) {
        console.log('WARNING: No models found in database!');
      }
    } catch (err) {
      console.error('Error fetching models:', err.message);
      console.error('Error details:', err);
      modelsResult = { rows: [] };
    }

    // Get user's generated images
    const imagesResult = await pool.query(
      `SELECT * 
       FROM generated_images 
       WHERE user_id = $1 
       ORDER BY created_at DESC LIMIT 12`,
      [req.session.userId]
    );

    res.render('dashboard', {
      username: req.session.username,
      userId: req.session.userId,
      images: imagesResult.rows,
      models: modelsResult.rows
    });
  } catch (error) {
    console.error('Dashboard error:', error);
    res.render('dashboard', {
      username: req.session.username,
      userId: req.session.userId,
      images: [],
      models: []
    });
  }
});

app.post('/generate', requireAuth, async (req, res) => {
  upload.single('image')(req, res, async function (err) {
    if (err instanceof multer.MulterError) {
      if (err.code === 'LIMIT_FILE_SIZE') {
        return res.status(400).json({ error: 'File size exceeds the 1MB limit' });
      }
      return res.status(400).json({ error: err.message });
    } else if (err) {
      return res.status(500).json({ error: err.message });
    }

    // Proceed with handling the request
    try {
      const { modelId } = req.body;

      if (!modelId) {
        return res.status(400).json({ error: 'Model ID is required' });
      }

      if (!req.file) {
        return res.status(400).json({ error: 'No image file uploaded' });
      }

      // Get model from database
      const modelResult = await pool.query(
        'SELECT * FROM ai_models WHERE model_id = $1',
        [modelId]
      );

      if (modelResult.rows.length === 0) {
        return res.status(400).json({ error: 'Invalid model ID' });
      }

      const model = modelResult.rows[0];

      if (!model.available) {
        return res.status(400).json({ error: 'This model is not available yet' });
      }

      // Get file buffer (bytes array) from uploaded file
      const fileBuffer = req.file.buffer;
      const fileName = req.file.originalname;
      const mimeType = req.file.mimetype;

      let requestId = "0x";
      for (let i = 0; i < 8; i++) {
        requestId += generateRandomString(5 + i % 2);
      }

      let traceId = uuidv4();

      // Create FormData
      const formData = new FormData();
      formData.append('traceId', traceId);

      // Convert Buffer to Blob
      const blob = new Blob([fileBuffer], { type: mimeType });
      formData.append('image', blob, fileName);

      formData.append('requestId', requestId);
      formData.append('userId', req.session.userId.toString());
      formData.append('modelId', model.id.toString());

      // Send request to backend service
      const response = await fetch(`${BACKEND_URL}/generate`, {
        method: 'POST',
        body: formData
      });

      if (!response.ok) {
        const errorText = await response.text();
        return res.status(response.status).json({
          error: errorText || 'Backend service error'
        });
      }

      res.json(await response.json());
    } catch (error) {
      console.error('Generation error:', error);
      res.status(500).json({ error: 'An error occurred during image generation' });
    }
  });
});

// Initialize database and start server
async function startServer() {
  // Wait for database to be ready
  const dbReady = await checkDatabaseHealth();

  if (!dbReady) {
    console.error('Failed to connect to database. Exiting...');
    process.exit(1);
  }

  // Initialize database tables
  try {
    await initDatabase();
  } catch (error) {
    console.error('Error initializing database:', error);
    process.exit(1);
  }

  console.log(`SESSION_SECRET = ${process.env.SESSION_SECRET}`);

  // Start server
  app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
  });
}

startServer();

