const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const User = require("../models/User");

const JWT_SECRET = process.env.JWT_SECRET || "supersecret";

exports.register = async (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return res.status(400).json({ error: "Username and password are required" });
    }

    const hash = await bcrypt.hash(password, 10);

    // Create user
    const newUser = await User.create(username, hash);

    res.json({ message: "User registered", user: { id: newUser.id, username: newUser.username } });
  } catch (err) {

    // Handle duplicate username error from Postgres
    if (err.code === "23505") {
      return res.status(400).json({ error: "User exists" });
    }

    res.status(500).json({ error: "Error registering user" });
  }
};

exports.login = async (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return res.status(400).json({ error: "Username and password are required" });
    }

    const user = await User.findByUsername(username);
    if (!user) return res.status(400).json({ error: "User not found" });

    const match = await bcrypt.compare(password, user.password);
    if (!match) return res.status(400).json({ error: "Invalid credentials" });

    const token = jwt.sign({ id: user.id, role: user.role }, JWT_SECRET, { expiresIn: "1h" });

    res.cookie("token", token, {
      secure: false, // set true in production with HTTPS
      maxAge: 3600000,
    });

    res.json({ message: "Login successful", id: user.id });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Login failed" });
  }
};
