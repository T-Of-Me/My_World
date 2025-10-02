const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const path = require('path');
const flash = require('connect-flash');
const ejs = require('ejs');
var XSSFilter = require('xssfilter');
var xssFilter = new XSSFilter();
require('dotenv').config();

const app = express();
const server = http.createServer(app);
const DEFAULT_AVATAR = '/images/default-avatar.png';

const session = require('express-session')({
  secret: process.env.SESSION_SECRET,
  resave: false,
  saveUninitialized: false,
  cookie: { secure: process.env.NODE_ENV === 'production' }
});
const sharedsession = require("express-socket.io-session");

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(express.static('public'));
app.use(session);
app.use(flash());

// Set view engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// Routes
const authRoutes = require('./routes/auth');
const chatRoutes = require('./routes/chat');

app.use('/auth', authRoutes);
app.use('/chat', chatRoutes);
app.use('/images', express.static(path.join(__dirname, 'public/images')));

// Root route
app.get('/', (req, res) => {
  res.render('home', { title: 'KORO-RENDER' });
});

const profileRoutes = require('./routes/profile')();
app.use('/profile', profileRoutes);
// Socket.io

const io = socketIo(server);
io.use(sharedsession(session));

const rateLimit = require('express-rate-limit');

const loginLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: 'Too many login attempts. Please try again later.',
});

app.use('/auth/login', loginLimiter);

io.on('connection', async (socket) => {
  if (!socket.handshake.session.accessToken) {
    socket.emit('unauthorized', 'You are not authorized to join the chat');
    return socket.disconnect();
  }

  let request = await fetch(`${process.env.AUTH_SERVICE_URL}/me`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${socket.handshake.session.accessToken}`
    }
  });

  let response = await request.json();

  if (request.status !== 200) {
    if (request.status === 401) {
      socket.emit('unauthorized', 'Unauthorized');
      return socket.disconnect();
    }
    socket.emit('error', { message: response.message || 'Internal Server Error' });
    return socket.disconnect();
  }
  const user = response;
  if (user) {
    socket.userId = user._id;
    socket.username = user.username;
    socket.avatar = '/images/default-avatar.png';
    socket.join(socket.username);

    io.to(socket.username).emit('message', {
      username: 'Korosensei',
      text: `Nurufufufu~ Welcome ${socket.username}! I am your beloved octopus teacher, Korosensei! I can taste-test your templates for any dangerous ingredients. Please share your template with me, and I'll evaluate it with my super-speed analysis! Nurufufufu~`,
      avatar: '/images/korosensei.png',
      timestamp: new Date()
    });
  } else {
    return socket.disconnect();
  }

  socket.on('message', async (msg, room) => {
    let userMsg = xssFilter.filter(msg.text);

    io.to(room).emit('message', {
      username: socket.username,
      text: `<textarea readonly>${userMsg}</textarea>`,
      avatar: socket.avatar,
      timestamp: new Date()
    });

    const forbidden = require('./data/bad_words.json');

    if (forbidden.some(term => userMsg.includes(term))) {
      io.to(room).emit('message', {
        username: "Korosensei",
        text: 'Please be polite, your template contains profanity words.',
        avatar: '/images/korosensei.png',
        timestamp: new Date()
      });
      return
    }

    try {
      ejs.render(userMsg, {});
      io.to(room).emit('message', {
        username: "Korosensei",
        text: 'Your template is safe!',
        avatar: '/images/korosensei.png',
        timestamp: new Date()
      });
    } catch (error) {
      io.to(room).emit('message', {
        username: "Korosensei",
        text: 'Your template is not working properly!',
        avatar: '/images/korosensei.png',
        timestamp: new Date()
      });
    }
  });
});

const PORT = process.env.PORT || 1212;
server.listen(PORT, () => console.log(`Server running on port ${PORT}`));
