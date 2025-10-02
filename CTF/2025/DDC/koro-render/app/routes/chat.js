const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');

router.get('/', auth, async (req, res) => {
  if (!req.session.user) {
    return res.redirect('/auth/login');
  }
  try {
    const user = req.session.user;
    res.render('chat', { title: 'Chat', user });
  } catch (error) {
    res.status(500).send('Server error');
  }
});

module.exports = router;
