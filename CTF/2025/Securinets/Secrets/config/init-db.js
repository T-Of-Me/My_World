const pool = require("./database");
const bcrypt = require("bcrypt");




(async () => {
  try {
    // Users table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        username TEXT UNIQUE,
        password TEXT NOT NULL,
        description TEXT DEFAULT 'Administrator account',
        role TEXT DEFAULT 'user'
      )
    `);

    // Messages table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS msgs (
        id SERIAL PRIMARY KEY,
        userId INT NOT NULL REFERENCES users(id),
        msg TEXT NOT NULL,
        type TEXT DEFAULT 'general',
        createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Flags table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS flags (
        id SERIAL PRIMARY KEY,
        flag TEXT NOT NULL
      )
    `);

    // Logs table
    await pool.query(`
      CREATE TABLE IF NOT EXISTS logs (
        id SERIAL PRIMARY KEY,
        userId INT REFERENCES users(id),
        action TEXT NOT NULL,
        createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Insert hardcoded flag if not exists
    const flag = "CTF{hardcoded_flag_here}";
    await pool.query(
      `INSERT INTO flags (flag) VALUES ($1) ON CONFLICT DO NOTHING`,
      [flag]
    );
    console.log("✅ Flag ensured in flags table");

    // Create admin user if not exists
    const adminPassword = "admin123";
    const hash = await bcrypt.hash(adminPassword, 10);

    const res = await pool.query(
      `SELECT * FROM users WHERE username = $1`,
      ["admin"]
    );

    if (res.rowCount === 0) {
      await pool.query(
        `INSERT INTO users (username, password, description, role) VALUES ($1, $2, $3, $4)`,
        ["admin", hash, "This is the admin account", "admin"]
      );
      console.log("✅ Admin user created with username 'admin'");
    } else {
      console.log("ℹ️ Admin user already exists");
    }

    await pool.end();
  } catch (err) {
    console.error("❌ Error initializing database:", err);
    process.exit(1);
  }
})();
