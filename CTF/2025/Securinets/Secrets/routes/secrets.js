const express = require("express");
const router = express.Router();
const secretController = require("../controllers/secretsController");
const authMiddleware = require("../middleware/authMiddleware");

router.get("/", authMiddleware, secretController.viewSecrets);
router.get("/:id", authMiddleware, secretController.viewSecretById);
router.post("/", authMiddleware, secretController.addSecret);

module.exports = router;