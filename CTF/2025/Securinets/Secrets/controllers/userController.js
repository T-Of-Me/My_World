const User = require("../models/User");

exports.getProfile = async (req, res) => {
  try {
    const userId = parseInt(req.query.id);

    // Only allow access if current user is the same or admin
    if (req.user.id !== userId && req.user.role !== "admin") {
      return res.status(403).json({ error: "Access denied" });
    }

    const user = await User.findById(userId);
    if (!user) return res.status(404).json({ error: "User not found" });

    res.render("profile", { user, currUser: req.user });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Database error" });
  }
};

exports.updateDescription = async (req, res) => {
  try {
    const { description } = req.body;
    const updatedUser = await User.updateDescription(req.user.id, description);
    res.json({ message: "Description updated", user: updatedUser });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to update description" });
  }
};

exports.addAdmin = async (req, res) => {
  try {
    const { userId } = req.body;

    if (req.user.role !== "admin") {
      return res.status(403).json({ error: "Access denied" });
    }

    const updatedUser = await User.updateRole(userId, "admin");
    res.json({ message: "Role updated", user: updatedUser });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to update role" });
  }
};
