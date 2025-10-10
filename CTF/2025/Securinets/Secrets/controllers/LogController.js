const db = require("../config/database");

exports.createLog = async (req, res) => {
  try {
    const { userId, action } = req.body;

    if (!userId || !action) {
      return res.status(400).json({ error: "userId and action are required" });
    }

    const query = `
      INSERT INTO logs (userId, action)
      VALUES ($1, $2)
      RETURNING id
    `;
    const values = [userId, action];

    const result = await db.query(query, values);

    res.json({
      message: "Log created successfully",
      logId: result.rows[0].id,
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Database error" });
  }
};
