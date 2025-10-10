const db = require("../config/database"); // pg Pool


const Secrets = {
    create: async (userId, title, secret) => {
        const query = `
      INSERT INTO secrets (ownerId, title, content)
      VALUES ($1, $2, $3)
      RETURNING *
    `;
        const values = [userId, title, secret];
        const res = await db.query(query, values);
        return res.rows[0];
    },
    findById: async (id) => {
        const query = `
      SELECT secrets.id, secrets.title, secrets.content, secrets.createdAt, secrets.ownerId, users.username
      FROM secrets
      INNER JOIN users ON secrets.ownerId = users.id
      WHERE secrets.id = $1
    `;
        const res = await db.query(query, [id]);
        return res.rows[0]; 
    },
    findAllByUser: async (userId) => {
        const query = `
      SELECT secrets.id, secrets.title, secrets.content, secrets.createdAt
      FROM secrets
      WHERE secrets.ownerId = $1
      ORDER BY secrets.createdAt DESC
    `;
        const res = await db.query(query, [userId]);
        return res.rows;
    },
};

module.exports = Secrets;
