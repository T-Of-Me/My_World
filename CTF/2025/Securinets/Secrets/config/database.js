// config/database.js
const { Pool } = require("pg");

const pool = new Pool({
  host: process.env.DB_HOST || "postgres",
  port: parseInt(process.env.DB_PORT) || 5432,
  user: process.env.DB_USER || "ctfuser",
  password: process.env.DB_PASS || "ctfpass",
  database: process.env.DB_NAME || "ctfdb",
});

async function connectWithRetry(retries = 10, delay = 1000) {
  for (let i = 0; i < retries; i++) {
    try {
      await pool.query("SELECT 1"); // test connection
      console.log("✅ Database connected");
      return pool;
    } catch (err) {
      console.log(`Waiting for database... attempt ${i + 1}`);
      await new Promise((res) => setTimeout(res, delay));
    }
  }
  throw new Error("❌ Could not connect to database after multiple attempts");
}

// Immediately try to connect when this module is imported
connectWithRetry().catch((err) => {
  console.error(err);
  process.exit(1); // exit if DB cannot be reached
});

module.exports = pool;
