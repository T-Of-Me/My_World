const express = require("express");
const router = express.Router();
const User = require("../models/User");
const puppeteer = require("puppeteer");
const jwt = require("jsonwebtoken");
const authMiddleware = require("../middleware/authMiddleware");

const JWT_SECRET = process.env.JWT_SECRET || "supersecret";

router.get("/", authMiddleware, (req, res) => {
  res.render("reportPage", { csrfToken: req.csrfToken() });
});

router.post("/", authMiddleware, async (req, res) => {
  const { url } = req.body;

  if (!url || !url.startsWith("http://localhost:3000")) {
    return res.status(400).send("Invalid URL");
  }

  try {
    const admin = await User.findById(1);
    if (!admin) throw new Error("Admin not found");

    const token = jwt.sign({ id: admin.id, role: admin.role }, JWT_SECRET, { expiresIn: "1h" });

    // Launch Puppeteer
    const browser = await puppeteer.launch({
      headless: true,
      args: ["--no-sandbox", "--disable-setuid-sandbox"],
    });

    const page = await browser.newPage();

    // Set admin token cookie
    await page.setCookie({
      name: "token",
      value: token,
      domain: "localhost",
      path: "/",
    });

    // Visit the reported URL
    await page.goto(url, { waitUntil: "networkidle2" });

    await browser.close();

    res.status(200).send("Thanks for your report");
  } catch (error) {
    console.error(error);
    res.status(200).send("Thanks for your report");
  }
});

module.exports = router;
