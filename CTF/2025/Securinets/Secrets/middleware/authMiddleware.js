const jwt = require("jsonwebtoken");
const JWT_SECRET = process.env.JWT_SECRET || "supersecret";

module.exports = (req, res, next) => {
  const token = req.cookies.token; // read from cookie
  if (!token) return res.redirect("/login");

  jwt.verify(token, JWT_SECRET, (err, decoded) => {
    if (err) return res.redirect("/login");
    req.user = decoded;
    next();
  });
};
