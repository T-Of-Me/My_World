var express = require('express');
var router = express.Router();
var User = require('../models/user.model');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const nodemailer = require('nodemailer');
require('dotenv').config();

/* GET home page. */
router.post('/register', async function(req, res, next) {
  let { username, password, email, name } = req.body;
  if (!username || !password || !email || !name) {
    return res.status(400).json({ message: 'Missing required fields' });
  }

  username = username.toLowerCase();
  email = email.toLowerCase();
  
  if (!/^[a-zA-Z0-9_]+$/.test(username))
    return res.status(400).json({ message: 'Username can only contain letters, numbers, and underscores' });

  if (!/^[a-zA-Z ]+$/.test(name))
    return res.status(400).json({ message: 'Name can only contain letters and spaces' });

  if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email))
    return res.status(400).json({ message: 'Invalid email format' });

  let findUsername = await User.findOne({ username });
  if (findUsername) {
    return res.status(400).json({ message: 'Username already exists' });
  }

  let findEmail = await User.findOne({ email });
  if (findEmail) {
    return res.status(400).json({ message: 'Email already exists' });
  }

  if (password.length < 8) {
    return res.status(400).json({ message: 'Password must be at least 8 characters long' });
  }

  let user = new User({
    name,
    username,
    email,
    password
  });

  try {
    await user.save();
    res.status(200).json({ message: 'User registered successfully' });
  } catch (error) {
    console.error('Error saving user:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

router.post('/login', async function(req, res, next) {
  let { username, password } = req.body;
  if (!username || !password) {
    return res.status(400).json({ message: 'Missing required fields' });
  }

  let user = await User.findOne({ username });
  if (!user) {
    return res.status(401).json({ message: 'Invalid username or password' });
  }

  let compare = await user.comparePassword(password);

  if (!compare) {
    return res.status(401).json({ message: 'Invalid username or password' });
  }

  const secret = process.env.JWT_SECRET || 'f4ke_jwt_secret';
  const token = jwt.sign({ id: user._id, username: user.username }, secret, { expiresIn: '1d' });

  res.status(200).json({ token });
});

router.post('/lookup', async function(req, res, next) {
  let { username } = req.body;

  if (!username) {
    return res.status(400).json({ message: 'Missing username' });
  }

  try {
    const resultArr = await User.collection.find({ $where: `this.username == '${username}'` }).toArray();
    if (!resultArr.length) {
      return res.status(404).json({ message: 'User not found' });
    }

    let data = resultArr[0];

    // let maskedEmail = email.replace(/(.{2})(.*)(.{2})(@.*)/, (match, p1, p2, p3, p4) => {
    //   return p1 + '*'.repeat(p2.length) + p3 + p4;
    // });

    res.status(200).json({ email: data.email, username: data.username });
  } catch (err) {
    console.error('Error looking up user:', err);
    res.status(500).json({ message: 'Internal server error' });
  }
});

router.post("/recover/sendEmail", async function(req, res, next) {
  let { email, captchaResponse } = req.body;
  if (!email || !captchaResponse) {
    return res.status(400).json({ message: 'Missing data' });
  }

  const secret = process.env.GOOGLE_RECAPTCHA_SECRET;
  const recaptchaResponse = await fetch(`https://www.google.com/recaptcha/api/siteverify`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: new URLSearchParams({
      secret,
      response: captchaResponse,
    }).toString()
  });

  const recaptchaData = await recaptchaResponse.json();
  if (!recaptchaData.success) {
    return res.status(400).json({ message: 'Invalid captcha' });
  }

  let user = await User.findOne({ email });
  if (!user) {
    return res.status(404).json({ message: 'User not found' });
  }
  
  const newPassword = crypto.randomBytes(10).toString('hex');

  user.password = newPassword;

  const transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: process.env.SMTP_PORT,
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS,
    },
  });

  const mailOptions = {
    from: `"${process.env.SMTP_FROM_NAME}" <${process.env.SMTP_FROM_EMAIL}>`,
    to: "hidden_email@ddctf.io",
    subject: 'Password Recovery',
    html: `
      <p>Hello ${user.name},</p>
      <p>You requested a password reset. Here is your new password:</p>
      <p><strong>${newPassword}</strong></p>
      <p>Please change your password after logging in.</p>
      <br>
      <p>Best regards,</p>
      <p>Koro-Render</p>
    `,
  };

  try {
    await transporter.sendMail(mailOptions);
    await user.save();
    res.status(200).json({ message: 'Success, please check your inbox' });
  } catch (error) {
    res.status(500).json({ message: 'Internal server error' });
  }
});

// Middleware to check if user is authenticated
let authMiddlware = (req, res, next) => {
  const token = req.headers['authorization']?.split(' ')[1];
  if (!token) {
    return res.status(401).json({ message: 'Unauthorized' });
  }
  const secret = process.env.JWT_SECRET || 'f4ke_jwt_secret';
  jwt.verify(token, secret, (err, decoded) => {
    if (err) {
      return res.status(401).json({ message: 'Unauthorized' });
    }
    req.user = decoded;
    next();
  });
};

router.get("/me", authMiddlware, async (req, res) => {
  try {
    const user = await User.findById(req.user.id).select('-password -__v -forceNewPassword -nextPasswordRequest');
    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }
    res.status(200).json(user);
  } catch (error) {
    console.error('Error fetching user:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

module.exports = router;
