const express = require("express");
const router = express.Router();
const logController = require("../controllers/LogController");
const authMiddleware = require("../middleware/authMiddleware");


router.post("/", authMiddleware, logController.createLog);
router.post("/:id", authMiddleware, logController.createLog);



module.exports = router;
