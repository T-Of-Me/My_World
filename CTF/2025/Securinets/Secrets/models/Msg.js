const db = require("../config/database"); // pg Pool
const { filterBy: filterHelper } = require("../helpers/filterHelper"); // keep helper

const Msg = {
  // Create a new message
  create: async (userId, msg, type = "general") => {
    const query = `
      INSERT INTO msgs (userId, msg, type)
      VALUES ($1, $2, $3)
      RETURNING *
    `;
    const values = [userId, msg, type];
    const res = await db.query(query, values);
    return res.rows[0];
  },

  // Find a message by ID
  findById: async (id) => {
    const query = `
      SELECT msgs.id, msgs.msg, msgs.type, msgs.createdAt, users.username
      FROM msgs
      INNER JOIN users ON msgs.userId = users.id
      WHERE msgs.id = $1
    `;
    const res = await db.query(query, [id]);
    return res.rows[0]; // undefined if not found
  },

  // Find all messages with optional filter
  findAll: async (filterField = null, keyword = null) => {
    const { clause, params } = filterHelper("msgs", filterField, keyword);

    const query = `
      SELECT msgs.id, msgs.msg, msgs.type, msgs.createdAt, users.username
      FROM msgs
      INNER JOIN users ON msgs.userId = users.id
      ${clause || ""}
      ORDER BY msgs.createdAt DESC
    `;


    const res = await db.query(query, params || []);
    return res.rows;
  },
};

module.exports = Msg;
