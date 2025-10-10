const express = require('express');
const cookieParser = require('cookie-parser');
const crypto = require('crypto');
const axios = require('axios');
const path = require('path');
const { browse } = require('./bot');

const HCAPTCHA_SITEKEY = process.env.HCAPTCHA_SITEKEY ?? 'H_SITEKEY'
const HCAPTCHA_SECRET = process.env.HCAPTCHA_SECRET ?? 'H_SECRET'
const FLAG = process.env.FLAG ?? 'crew{REDACTED}'
const BOT_COOKIE = crypto.randomBytes(32).toString('hex');

const app = express();
app.use(cookieParser());
app.use(express.urlencoded({extended:false}));

// Serve static files from the 'static' folder
app.use('/static', express.static('static'));


// Middleware to add security headers to all responses
app.use((req, res, next) => {
    // Prevent any attack
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('Content-Security-Policy', "script-src 'self' https://js.hcaptcha.com/1/api.js; style-src 'self'; img-src 'self'; font-src 'none'; connect-src 'none'; media-src 'none'; object-src 'none'; prefetch-src 'none'; frame-ancestors 'none'; form-action 'self';");
    res.setHeader('Referrer-Policy', 'no-referrer');
    res.setHeader('Permissions-Policy', 'accelerometer=(),attribution-reporting=(),autoplay=(),browsing-topics=(),camera=self,captured-surface-control=(),ch-device-memory=(),ch-downlink=(),ch-dpr=(),ch-ect=(),ch-prefers-color-scheme=(),ch-prefers-reduced-motion=(),ch-rtt=(),ch-save-data=(),ch-ua=(),ch-ua-arch=(),ch-ua-bitness=(),ch-ua-form-factors=(),ch-ua-full-version=(),ch-ua-full-version-list=(),ch-ua-mobile=(),ch-ua-model=(),ch-ua-platform=(),ch-ua-platform-version=(),ch-ua-wow64=(),ch-viewport-height=(),ch-viewport-width=(),ch-width=(),clipboard-read=(),clipboard-write=(),compute-pressure=(),cross-origin-isolated=(),deferred-fetch=(),digital-credentials-get=(),display-capture=self,encrypted-media=(),ethereum=(),fullscreen=(),gamepad=(),geolocation=(),gyroscope=(),hid=(),identity-credentials-get=(),idle-detection=(),join-ad-interest-group=(),keyboard-map=(),local-fonts=(),magnetometer=(),microphone=self,midi=(),otp-credentials=(),payment=(),picture-in-picture=(),private-aggregation=(),private-state-token-issuance=(),private-state-token-redemption=(),publickey-credentials-create=(),publickey-credentials-get=(),run-ad-auction=(),screen-wake-lock=(),serial=(),shared-storage=(),shared-storage-select-url=(),solana=(),storage-access=(),sync-xhr=(),unload=(),usb=(),window-management=(),xr-spatial-tracking=()');
    res.setHeader('Cache-Control', 'no-store');
    next();
});


app.get('/', (req, res) => {
    res.send(`<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CrewCTF</title>
    <link rel="stylesheet" href="/static/styles.css">
</head>
<body>
    <div class="menu">
      <img class="plusplusplus" width=64 src="/static/+++.png">
      <ul>
        <li><a href="/">Home</a></li>
        <li><a href="/report">Report to Professor</a></li>
        <li><a href="/ProfMeet">Office Hours Videoconference</a></li>
        <li><a href="/syllabus">Syllabus</a></li>
      </ul>
    </div>
    <h1>Web Lecture Final Scores 2025</h1>
    <table class="score-table">
        <thead>
            <tr>
                <th>Student Name</th>
                <th>Score</th>
            </tr>
        </thead>
        <tbody>
            <tr> <td>Tim Berners-Lee</td> <td>100</td> </tr> <tr> <td>Brendan Eich</td> <td>97</td> </tr> <tr> <td>Fredd</td> <td>91</td> </tr> <tr> <td>Satoooon</td> <td>91</td> </tr> <tr> <td>Clovis Mint</td> <td>84</td> </tr> <tr> <td>imarcex</td> <td>83</td> </tr> <tr> <td>hah4</td> <td>82</td> </tr> <tr> <td>Jackfromeast</td> <td>81</td> </tr> <tr> <td>7Rocky</td> <td>78</td> </tr> <tr> <td>Aali</td> <td>76</td> </tr> <tr> <td>Babafaba</td> <td>72</td> </tr> <tr> <td>CSN3RD</td> <td>70</td> </tr> <tr> <td>hiikunZ</td> <td>69</td> </tr> <tr> <td>H-mmer</td> <td>67</td> </tr> <tr> <td>ItayB</td> <td>67</td> </tr> <tr> <td>Kiona</td> <td>65</td> </tr> <tr> <td>KLPP</td> <td>65</td> </tr> <tr> <td>Linz</td> <td>55</td> </tr> <tr> <td>Moriarty</td> <td>48</td> </tr> <tr> <td>mk</td> <td>43</td> </tr> <tr> <td>Onirique</td> <td>40</td> </tr> <tr> <td>Oshawk</td> <td>36</td> </tr> <tr> <td>Shadowwws</td> <td>32</td> </tr> <tr> <td>Sealldev</td> <td>31</td> </tr> <tr> <td>You</td> <td>12</td> </tr>
        </tbody>
    </table>
    <footer>
        <p>&copy; 2025 Web Security Course. All rights reserved.</p>
    </footer>
</body>
</html>
`);
});

app.get('/report', (req, res) => {
	res.send(`<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>CrewCTF</title>
    <link rel="stylesheet" href="/static/styles.css">
    <script src="https://js.hcaptcha.com/1/api.js" async defer></script>
</head>
<body>
    <div class="menu">
      <img class="plusplusplus" width=64 src="/static/+++.png">
      <ul>
        <li><a href="/">Home</a></li>
        <li><a href="/report">Report to Professor</a></li>
        <li><a href="/ProfMeet">Office Hours Videoconference</a></li>
        <li><a href="/syllabus">Syllabus</a></li>
      </ul>
    </div>

    <h1>Report to Professor</h1>
    <form method="POST">
        <p>Be specific about what you want to be reviewed. He’ll look at it when possible, so don’t waste his time.</p>
        <input name="student" placeholder="Your Name or Student ID"/>
        <textarea name="complain" placeholder="Your Request to Professor."/></textarea>
        <div class="h-captcha" data-sitekey="${HCAPTCHA_SITEKEY}"></div>
        <input type="submit" />
    </form>
    <footer>
        <p>&copy; 2025 Web Security Course. All rights reserved.</p>
    </footer>
  </body>
</html>`);
});

app.post('/report', async (req, res) => {
    const student = req.body.student;
    const complain = req.body.complain;
    const hcaptchaResponse = req.body['h-captcha-response'];

    if(typeof complain !== 'string' || typeof student !== 'string'){
        res.status(400).send('Missing student or the complain');
        return;
    }
    if(typeof hcaptchaResponse !== 'string'){
        res.status(400).send('Missing hCaptcha');
        return;
    }

    try{
        // HCaptcha check
        const hCaptchaValidation = await axios.post('https://api.hcaptcha.com/siteverify', 
        {
            secret: HCAPTCHA_SECRET,
            response: hcaptchaResponse
        },
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        }

        );
       
        if (!hCaptchaValidation.data.success) {
            return res.status(400).send('hCaptcha verification failed.');
        }

        browse(student, complain, BOT_COOKIE);
        res.send('Thank you for your report.');
    }catch(e){
    	console.log(e);
        res.status(500).send('Error');
    }
});

app.get('/professor', (req, res) => {
    const cookie = req.cookies['professor']; 
    if (cookie === BOT_COOKIE) {
	res.send(`<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Professor Dashboard</title>
    <link rel="stylesheet" href="/static/professor.css">
    <link rel="stylesheet" href="/static/styles.css">
</head>
<body>
    <div class="menu">
      <img class="plusplusplus" width=64 src="/static/+++.png">
      <ul>
        <li><a href="/">Home</a></li>
        <li><a href="/report">Report to Professor</a></li>
        <li><a href="/ProfMeet">Office Hours Videoconference</a></li>
        <li><a href="/syllabus">Syllabus</a></li>
      </ul>
      <div class="profile">
        <img id="prof-icon" src="/static/imgs/${Math.random() < 0.5 ? "icon2.jpg" : "icon.webp"}">
      </div>
    </div>
    <h1>Student Complain</h1>
    <p>The student’s complaining again, as usual. They’re always complaining—just a bunch of assholes. They act like the world owes them something. Seriously, they’re just attention-seekers. Don’t forget what your mom used to say: <b><i>${FLAG}</i></b></p>
    <div>
        Student: 
        <p id="student"></p>
    </div>
    <div>
        <p id="complain"></p>
    </div>
</body>
<script src="/static/main.js"></script>
</html>`);
    } else {
        return res.status(400).send('Your are not admin!');
    }
});

app.get('/syllabus', (req, res) => {
    res.sendFile(path.join(__dirname,'./static/syllabus.html'));
});

app.get('/profmeet', (req, res) => {
    // Element number 4123 in my TO-DO list
    // maybe for new year's resolutions of 2026
	res.send(`<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>CrewCTF</title>
    <link rel="stylesheet" href="/static/styles.css">
    <script src="https://js.hcaptcha.com/1/api.js" async defer></script>
</head>
<body>
    <div class="menu">
      <img class="plusplusplus" width=64 src="/static/+++.png">
      <ul>
        <li><a href="/">Home</a></li>
        <li><a href="/report">Report to Professor</a></li>
        <li><a href="/ProfMeet">Office Hours Videoconference</a></li>
        <li><a href="/syllabus">Syllabus</a></li>
      </ul>
    </div>

    <h1>Office Hours Meeting</h1>
    <h1>🚧 Under construction! 🚧</h1>
    <footer>
        <p>&copy; 2025 Web Security Course. All rights reserved.</p>
    </footer>
  </body>
</html>`);
});

app.listen(1337, "0.0.0.0",  () => {
    console.log('Server is running at http://localhost:1337');
});
