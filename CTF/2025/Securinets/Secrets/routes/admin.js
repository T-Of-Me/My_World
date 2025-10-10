const express = require("express");
const router = express.Router();
const adminController = require("../controllers/adminController");
const authMiddleware = require("../middleware/authMiddleware");
const userController = require("../controllers/userController");
const { route } = require("./auth");

router.get("/msgs", authMiddleware, adminController.showall);
router.post("/msgs", authMiddleware, adminController.showMsgs);

router.get("/users", authMiddleware, adminController.showAllUsers);

router.post("/addAdmin", authMiddleware, userController.addAdmin);

module.exports = router;
 