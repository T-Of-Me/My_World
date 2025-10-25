import puppeteer from "puppeteer";
import { computeExecutablePath } from "@puppeteer/browsers";
const FLAG = process.env.FLAG || "flag{test_flag_0a1d}";

const BASE_URL = process.env.BASE_URL || "http://frontend:1337";

const firefoxExecutablePath = computeExecutablePath({
    browser: 'firefox',
    buildId: 'stable',
    cacheDir: '/root/.cache/puppeteer/',
});

async function visit(url) {
    let browser;
    try {
        browser = await puppeteer.launch({
            browser: "firefox",
            headless: true,
            executablePath: firefoxExecutablePath,
            pipe: true,
            ignoreHTTPSErrors: true
        });

        const ctx = await browser.createBrowserContext();
        const page = await ctx.newPage();
        
        await page.goto(BASE_URL, { timeout: 2000 });
        await page.evaluate((flag) => {
          localStorage.setItem("flag", flag);  
        }, FLAG)

        console.log(`Visiting: ${url}`);
        await page.goto(url, { timeout: 2000 });

        await new Promise((r) => setTimeout(r, 2000));
    } catch (err) {
        console.log(err);
    } finally {
        if (browser) await browser.close();
    }
}

export default visit;