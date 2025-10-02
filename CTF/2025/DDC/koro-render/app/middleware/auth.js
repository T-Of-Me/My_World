
module.exports = async (req, res, next) => {
  if (!req.session.accessToken) {
    return res.redirect('/auth/login');
  }
  try {
    const request = fetch(`${process.env.AUTH_SERVICE_URL}/me`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${req.session.accessToken}`
      }
    });
    const response = await request;
    if (!response.ok) {
      if (response.status === 401) {
        return res.redirect('/auth/login');
      }
      
      res.status(response.status).send({ error: response.message });
      return;
    }

    const user = await response.json();
    if (!user) {
      return res.redirect('/auth/login');
    }

    req.user = user;
    next();
  } catch (error) {
    res.status(500).send(error);
  }
};