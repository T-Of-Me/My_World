const express = require("express");
const router = express.Router();
const msgController = require("../controllers/msgController");
const authMiddleware = require("../middleware/authMiddleware");

router.get("/", authMiddleware, msgController.showMsgForm);

router.post("/", authMiddleware, msgController.submitMsg);

module.exports = router;
