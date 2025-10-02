const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');

module.exports = function() {
  // Profile routes
  router.get('/', auth, async (req, res) => {
    try {
      const user = req.session.user;
      res.render('profile', { user });
    } catch (error) {
      res.status(500).send('Server Error');
    }
  });

  return router;
};
