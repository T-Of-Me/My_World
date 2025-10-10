const db = require("../config/database");

const User = {
  // Create a new user
  create: async (username, password, description = "") => {
    const query = `
      INSERT INTO users (username, password, description)
      VALUES ($1, $2, $3)
      RETURNING id, username, description, role
    `;
    const values = [username, password, description];
    const res = await db.query(query, values);
    return res.rows[0];
  },

  // Find a user by username
  findByUsername: async (username) => {
    const query = `SELECT * FROM users WHERE username = $1`;
    const res = await db.query(query, [username]);
    return res.rows[0];
  },

  // Find a user by ID
  findById: async (id) => {
    const query = `SELECT id, username, description, role FROM users WHERE id = $1`;
    const res = await db.query(query, [id]);
    return res.rows[0];
  },

  // Update user description
  updateDescription: async (id, description) => {
    const query = `UPDATE users SET description = $1 WHERE id = $2 RETURNING id, username, description, role`;
    const res = await db.query(query, [description, id]);
    return res.rows[0];
  },

  // Get all users (id, username, description, role)
  findAll: async () => {
    const query = `SELECT id, username, description, role FROM users ORDER BY id`;
    const res = await db.query(query);
    return res.rows;
  },

  // Update user role
  updateRole: async (id, role) => {
    const query = `UPDATE users SET role = $1 WHERE id = $2 RETURNING id, username, description, role`;
    const res = await db.query(query, [role, id]);
    return res.rows[0];
  },
};

module.exports = User;
