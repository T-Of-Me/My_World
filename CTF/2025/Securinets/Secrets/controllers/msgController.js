const Msg = require("../models/Msg");

exports.showMsgForm = (req, res) => {
  res.render("msg", { userId: req.user.id });
};

exports.submitMsg = async (req, res) => {
  try {
    const userId = req.user.id;
    const msg = req.body.msg;
    const type = req.body.type || "general";

    if (!msg) return res.status(400).json({ error: "Message required" });

    // Create message in Postgres
    const newMsg = await Msg.create(userId, msg, type);

    res.json({
      message: "Msg submitted",
      msg: newMsg, // optional: return created message
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to save Msg" });
  }
};
