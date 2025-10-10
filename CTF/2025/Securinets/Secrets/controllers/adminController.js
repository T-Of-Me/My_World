const Msg = require("../models/Msg");
const User = require("../models/User");

exports.showMsgs = async (req, res) => {
  if (req.user.role !== "admin") {
    return res.status(403).send("Access denied");
  }

  const { filterBy: filterField, keyword } = req.body;

  try {
    const rows = await Msg.findAll(filterField, keyword);
    
    res.render("admin-msgs", {
      msgs: rows,
      filterBy: filterField,
      keyword,
      csrfToken: req.csrfToken(),
    });
  } catch (err) {
    res.status(400).send("Bad request");
  }
};

exports.showall = async (req, res) => {
  if (req.user.role !== "admin") {
    return res.status(403).send("Access denied");
  }

  try {
    const rows = await Msg.findAll();
    res.render("admin-msgs", {
      msgs: rows,
      filterBy: null,
      keyword: null,
      csrfToken: req.csrfToken(),
    });
  } catch (err) {
    res.status(400).send("Bad request");
  }
};

exports.showAllUsers = async (req, res) => {
  if (req.user.role !== "admin") {
    return res.status(403).send("Access denied");
  }

  try {
    const users = await User.findAll();
    res.render("admin-users", { users, user: req.user, csrfToken: req.csrfToken() });
  } catch (err) {
    res.status(500).send("Failed to retrieve users");
  }
};
