const puppeteer = require('puppeteer-core');

const TIMEOUT = 10000;
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

async function browse(student, complain, cookie){
    let browser;
    try{
        const url = `http://localhost:1337/professor?student=${encodeURIComponent(student)}&complain=${encodeURIComponent(complain)}`
        console.log(`Opening browser for ${url}`);
        browser = await puppeteer.launch({
            headless: false,
            pipe: true,
            executablePath: '/usr/bin/chromium',
            ignoreHTTPSErrors: true, 
            acceptInsecureCerts: true,
            args: [
                '--ignore-certificate-errors',
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--auto-select-desktop-capture-source=E',
                '--disable-wasm',
                '--disable-dev-shm-usage',
                '--disable-gpu',
                '--disable-crash-reporter',
                '--no-crashpad',
                '--jitless'
            ]
        });
        await Promise.race([
            sleep(TIMEOUT),
            visit(browser, url, cookie),
        ]);
    }catch(e){
        console.error('Failed to browse:', e);
    }finally{
        if(browser){
            try{
                await browser.close();
            }catch(e){
                console.error('Failed to close browser:', e);
            }
        }
    }
}

async function visit(ctx, url, cookie){
    page = await ctx.newPage();

    // Set a cookie
    await page.setCookie({
      name: 'professor',
      value: cookie,
      domain: 'localhost:1337',
      path: '/', 
      httpOnly: true, 
      sameSite: 'Strict' 
    });
    
    await page.setRequestInterception(true);
    // Prevent going out of the chall
    ctx.on("targetcreated", async (target) => {
        if (target.type() === "page") {
            const newPage = await target.page();
            await newPage.close(); // close immediately
        }
    });
    page.evaluateOnNewDocument(() => {
        window.open = () => {};
    });
    page.on('request', request => {
        let navigation = request.isNavigationRequest();
        let url = request.url();
        let frame = request.frame();
        if(navigation && frame === page.mainFrame() && !url.startsWith('http://localhost:1337/')){
            request.abort();
        } else{
            request.continue(); 
        }
    });

    console.log('Visiting ', url);
    await page.goto(url);
    await sleep(TIMEOUT);
    await page.close();
}

module.exports = {browse};
