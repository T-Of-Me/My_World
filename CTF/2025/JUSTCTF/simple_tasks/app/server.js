


const express = require('express');
const session = require('express-session');
const bodyParser = require('body-parser');
const path = require('path');
const app = express();
const crypto = require('crypto');
const puppeteer = require('puppeteer');

const FLAG = process.env.FLAG || "justCTF{this_is_example_flag_f8yemmwrdv}"
const SECRET = crypto.randomBytes(24).toString('hex');

app.use(bodyParser.urlencoded({ extended: false }));

app.use(session({ secret: SECRET, resave: false, saveUninitialized: true }));
app.use((req, res, next) => {
    const nonce = res.locals.nonce = crypto.randomBytes(16).toString('base64');
    res.setHeader("Content-Security-Policy", `script-src 'nonce-${nonce}'; style-src 'nonce-${nonce}'`);
    res.setHeader("Cache-Control", "no-store");

    next();
});


app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

const users = new Map();

function requireLogin(req, res, next) {
    if (!req.session.username) return res.redirect('/login');
    next();
}

app.get('/', (req, res) => {
    if (req.session.username) return res.redirect('/tasks');
    res.redirect('/login');
});

app.get('/register', (req, res) => res.render('register'));
app.post('/register', (req, res) => {
    const { username, password } = req.body;
    if (!users.has(username)) {
        users.set(username, { password, tasks: [] });
        req.session.username = username;
        res.redirect('/tasks');
    } else {
        res.send('Username already exists.');
    }
});

app.get('/login', (req, res) => res.render('login'));
app.post('/login', (req, res) => {
    const { username, password } = req.body;
    if (users.has(username) && users.get(username).password === password) {
        req.session.username = username;
        res.redirect('/tasks');
    } else {
        res.send('Invalid credentials.');
    }
});

app.get('/tasks', requireLogin, (req, res) => {
    const user = users.get(req.session.username);
    res.render('tasks', { tasks: user.tasks });
});



app.post('/tasks/create', requireLogin, (req, res) => {
    const user = users.get(req.session.username);
    user.tasks.unshift({ tasks: [] });
    res.redirect(`/tasks/0`);
});

app.get('/tasks/:id', requireLogin, (req, res) => {
    const user = users.get(req.session.username);
    const taskId = parseInt(req.params.id);
    if (isNaN(taskId) || taskId < 0 || taskId >= user.tasks.length) return res.send('Task not found.');
    const task = user.tasks[taskId];
    res.render('task', { taskId, tasks: task.tasks });
});

app.post('/tasks/:id', requireLogin, (req, res) => {
    const user = users.get(req.session.username);
    const taskId = parseInt(req.params.id);
    const { content } = req.body;
    if (typeof content !== 'string' || content.length > 2000) {
        return res.end("Invalid note");
    }


    if (isNaN(taskId) || taskId < 0 || taskId >= user.tasks.length) return res.status(404).send('Task not found.');

    const task = user.tasks[taskId];
    task.tasks.unshift(content);
    res.redirect(`/tasks/${taskId}`);
});

app.get('/tasks/delete/:id', requireLogin, (req, res) => {
    const user = users.get(req.session.username);
    const taskId = parseInt(req.params.id);

    if (user.tasks[taskId]) {
        user.tasks.splice(taskId, 1);
        res.redirect(`/tasks`);
        return;
    }

    res.send("Task not found");
});

app.get('/tasks/delete/:id/:pos', requireLogin, (req, res) => {
    const user = users.get(req.session.username);
    const taskId = parseInt(req.params.id);
    const taskPos = parseInt(req.params.pos);

    const task = user.tasks[taskId];
    if (task && task.tasks[taskPos] != null) {
        task.tasks.splice(taskPos, 1);
        res.redirect(`/tasks/${taskId}`);
        return;
    }

    res.send("Task not found");
});

app.get('/tasks/preview/:id/:pos', requireLogin, (req, res) => {
    res.set("Content-Type", "text/html; charset=utf-8");
    res.setHeader("Content-Security-Policy", `script-src 'none'`);

    const user = users.get(req.session.username);
    const taskId = parseInt(req.params.id);
    const taskPos = parseInt(req.params.pos);

    const task = user.tasks[taskId];
    if (task && task.tasks[taskPos] != null) {
        res.end(task.tasks[taskPos]);
        return;
    }

    res.end("Task not found");
});

const adminTokens = new Set();

app.get('/token', (req, res) => {
    if (adminTokens.has(req.query.token)) {
        res.end(FLAG);
        return;
    }
    res.send('nope');
});

app.get('/bot', (req, res) => {
    const { url } = req.query;
    if (url) {
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
            return res.send(`<h2>Invalid URL!</h2>`);
        }

        console.log('Submitted URL:', url);
        try {
            visit(url);
        } catch (e) {
            console.log(e);
        }

        return res.send(`<h2>Visiting ... </h2>`);
    }

    return res.render('bot');
});


const sleep = d => new Promise(r => setTimeout(r, d));

let browser;

const visit = async (url) => {
    if (browser) {
        await browser.close();
        await sleep(2000);
        console.log("Terminated ongoing job.");
    }
    try {
        browser = await puppeteer.launch({
            browser: 'chrome',
            headless: true,
            args: ["--disable-features=HttpsFirstBalancedModeAutoEnable", "--no-sandbox"]
        });

        const ctx = await browser.createBrowserContext();
        let page;
        page = await ctx.newPage();

        const token = crypto.randomBytes(24).toString('hex');

        users.set(`admin_${token}`, { password: token, tasks: [{ tasks: [`justToken{${token}}`] }] });
        adminTokens.add(token);

        await page.goto(`http://localhost:3000/login`, { timeout: 3000, waitUntil: 'domcontentloaded' });
        await page.waitForSelector('input[name=username]');
        await page.type('input[name=username]', `admin_${token}`);
        await page.type('input[name=password]', token);
        await page.click('button[type=submit]');
        await sleep(1000);
        await page.close();

        page = await ctx.newPage();
        await page.goto(url, { timeout: 3000, waitUntil: 'domcontentloaded' });

        await sleep(1000 * 60 * 2);

        await browser.close();
        browser = null;
    } catch (err) {
        console.log(err);
    } finally {
        console.log('close');
        if (browser) await browser.close();
    }
};


app.listen(3000, () => console.log('Server running on http://localhost:3000'));
