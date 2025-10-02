const express = require('express');
const router = express.Router();
const nodemailer = require('nodemailer');
require('dotenv').config();

router.get('/register', (req, res) => {
  res.render('register', { title: 'Register' });
});

router.post('/register', async (req, res) => {
  try {
    const { username, email, password, name } = req.body;

    // Convert username and email to lowercase
    const lowerCaseUsername = username.toLowerCase();
    const lowerCaseEmail = email.toLowerCase();

    let request = await fetch(`${process.env.AUTH_SERVICE_URL}/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        name: name,
        username: lowerCaseUsername,
        email: lowerCaseEmail,
        password: password,
      }),
    });

    let response = await request.json();

    if (request.status !== 200) {
      return res.status(request.status).render('register', { 
        title: 'Register', 
        message: response.message
      });
    }

    res.redirect('/auth/login?username=' + encodeURIComponent(lowerCaseUsername) + '&message=' + encodeURIComponent('Registration successful. Please log in.'));
  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).render('register', { 
      title: 'Register', 
      message: 'Registration failed. Please try again.' 
    });
  }
});

router.get("/", (req, res) => {
  res.redirect('/auth/login');
});

router.get('/login', (req, res) => {
  let { username, message } = req.query;
  res.render('login', { username, message });
});

// Login route
router.post('/login', async (req, res) => {
  const { username, password } = req.body;

  try {
    let request = await fetch(`${process.env.AUTH_SERVICE_URL}/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        username: username,
        password: password,
      }),
    });

    let response = await request.json();

    if (request.status !== 200) {
      return res.status(request.status).render('login', {
        username: username,
        message: response.message || 'Login failed. Please try again.',
      });
    }

    let userReq = await fetch(`${process.env.AUTH_SERVICE_URL}/me`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${response.token}`,
      },
    });

    let user = await userReq.json();

    req.session.user = user;
    req.session.accessToken = response.token;

    res.redirect('/chat');
  } catch (err) {
    console.error('Login error:', err);
    res.render('login', { username, message: 'Internal Server Error' });
  }
});

router.get('/logout', async (req, res) => {
  try {
    let user = req.session.user;
    req.session.destroy();
    res.redirect('/auth/login?username=' + encodeURIComponent(user.username));
  } catch (error) {
    res.status(500).send(error);
  }
});

router.get('/recoverPassword', (req, res) => {
  res.render('recoverPassword', { title: 'Recover Password' });
});

router.post('/recoverPassword', async (req, res) => {
  const { email } = req.body;

  if (!email) {
    return res.status(400).render('recoverPassword', {
      message: 'Missing email',
    });
  }

  try {
    let request = await fetch(`${process.env.AUTH_SERVICE_URL}/recover/sendEmail`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email: email.toLowerCase(), captchaResponse: req.body['g-recaptcha-response'] }),
    });
    let response = await request.json();
    if (request.status !== 200) {
      return res.status(request.status).render('recoverPassword', {
        message: response.message || 'Internal server error',
      });
    } else {
      res.render('recoverPassword', {
        message: 'New password has been sent to your email.',
      });
    }

    // const { username, resetToken } = response;

    // V1
    // const transporter = nodemailer.createTransport({
    //   host: process.env.SMTP_HOST,
    //   port: process.env.SMTP_PORT,
    //   auth: {
    //     user: process.env.SMTP_USER,
    //     pass: process.env.SMTP_PASS,
    //   },
    // });

    // const mailOptions = {
    //   from: `"${process.env.SMTP_FROM}" <${process.env.SMTP_USER}>`,
    //   to: "hidden_email@ddc.com",
    //   subject: 'Password Recovery',
    //   html: `
    //     <p>Hello ${username},</p>
    //     <p>You requested a password reset. Click the link below to reset your password:</p>
    //     <p><a href="${process.env.BASE_URL}/auth/resetPassword?token=${token}">Reset Password</a></p>
    //     <p>If you did not request this, please ignore this email.</p>
    //   `,
    // };

    // try { 
    //   await transporter.sendMail(mailOptions);
    //   res.render('recoverPassword', {
    //     title: 'Recover Password',
    //     message: 'Recovery email sent. Please check your inbox.',
    //   });
    // } catch (emailError) {
    //   console.error('Email sending error:', emailError);
    //   res.status(500).render('recoverPassword', {
    //     title: 'Recover Password',
    //     message: 'Failed to send recovery email. Please contact the administrator.',
    //   });
    // }

  } catch (error) {
    console.error('Recover password error:', error);
    res.status(500).render('recoverPassword', {
      message: 'Internal server error. Please try again later.',
    });
  }
});

module.exports = router;