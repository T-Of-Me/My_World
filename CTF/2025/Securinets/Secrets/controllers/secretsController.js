const Secrets = require("../models/Secrets");


exports.viewSecrets = async (req, res) => {
    try {
        const secrets = await Secrets.findAllByUser(req.user.id);
        res.render("secrets", { secrets, user: req.user, csrfToken: req.csrfToken() });
    } catch (err) {
        console.error(err);
        res.status(500).send("Failed to retrieve secrets");
    }
};

exports.addSecret = async (req, res) => {
    const { title, content } = req.body;
    if (!title || !content) {
        return res.status(400).send("Title and content are required");
    }

    try {
        await Secrets.create(req.user.id, title, content);
        res.redirect("/secrets");
    } catch (err) {
        console.error(err);
        res.status(500).send("Failed to add secret");
    }
};

exports.viewSecretById = async (req, res) => {
    const secretId = req.params.id;
    try {
        const secret = await Secrets.findById(secretId);
        if (!secret) {
            return res.status(404).send("Secret not found");
        }

        
        
        if (secret.ownerid != req.user.id) {
            return res.status(403).send("Access denied");
        }
        res.render("secret-detail", { secret, user: req.user, csrfToken: req.csrfToken() });
    } catch (err) {
        console.error(err);
        res.status(500).send("Failed to retrieve secret");
    }
};

