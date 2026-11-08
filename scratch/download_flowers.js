const fs = require('fs');
const path = require('path');
const https = require('https');

const outDir = path.join(__dirname, 'downloaded_templates');

function fetchText(url) {
    return new Promise((resolve, reject) => {
        const req = https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, (res) => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                let redirectUrl = res.headers.location;
                if (!redirectUrl.startsWith('http')) {
                    redirectUrl = 'https://openclipart.org' + redirectUrl;
                }
                return fetchText(redirectUrl).then(resolve).catch(reject);
            }
            if (res.statusCode !== 200) {
                return reject(new Error(`HTTP ${res.statusCode} for ${url}`));
            }
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => resolve(data));
        });
        req.on('error', reject);
        req.setTimeout(12000, () => {
            req.destroy();
            reject(new Error(`Timeout for ${url}`));
        });
    });
}

function downloadFile(url, destPath) {
    return new Promise((resolve, reject) => {
        const file = fs.createWriteStream(destPath);
        const req = https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, (res) => {
            if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
                let redirectUrl = res.headers.location;
                if (!redirectUrl.startsWith('http')) {
                    redirectUrl = 'https://openclipart.org' + redirectUrl;
                }
                file.close();
                fs.unlinkSync(destPath);
                return downloadFile(redirectUrl, destPath).then(resolve).catch(reject);
            }
            if (res.statusCode !== 200) {
                file.close();
                if (fs.existsSync(destPath)) fs.unlinkSync(destPath);
                return reject(new Error(`HTTP ${res.statusCode} for ${url}`));
            }
            res.pipe(file);
            file.on('finish', () => {
                file.close();
                resolve();
            });
        });
        req.on('error', err => {
            file.close();
            if (fs.existsSync(destPath)) fs.unlinkSync(destPath);
            reject(err);
        });
        req.setTimeout(20000, () => {
            req.destroy();
            file.close();
            if (fs.existsSync(destPath)) fs.unlinkSync(destPath);
            reject(new Error(`Timeout downloading ${url}`));
        });
    });
}

const candidates = [
    { id: '352740', slug: 'imaginary-flower-head', catId: 'flowers', catVi: 'Hoa lá' },
    { id: '352672', slug: 'simple-flower-head-lineart', catId: 'flowers', catVi: 'Hoa lá' },
    { id: '333586', slug: 'lilly-flower-abstract-art-coloring-page', catId: 'flowers', catVi: 'Hoa lá' }
];

async function run() {
    for (const c of candidates) {
        const detailUrl = `https://openclipart.org/detail/${c.id}/${c.slug}`;
        console.log(`Checking ${detailUrl}...`);
        try {
            const html = await fetchText(detailUrl);
            const m = html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/i);
            if (!m) {
                console.log(`No JSON-LD for ${c.id}`);
                continue;
            }
            const ld = JSON.parse(m[1].trim());
            const artist = (ld.artist && ld.artist.name) || (ld.author && ld.author.name) || 'Anonymous';
            const title = ld.name || c.slug;
            const svgUrl = (ld.image && ld.image.url) ? ld.image.url : `https://openclipart.org/download/${c.id}`;
            const localFile = `${c.catId}_${c.id}_${c.slug.substring(0, 20).replace(/[^a-zA-Z0-9]/g, '_')}.svg`;
            const destPath = path.join(outDir, localFile);
            await downloadFile(svgUrl, destPath);
            const stat = fs.statSync(destPath);
            console.log(`Downloaded ${localFile}: ${stat.size} bytes | Artist: ${artist} | Title: ${title}`);
        } catch (e) {
            console.log(`Error ${c.id}: ${e.message}`);
        }
    }
}

run().catch(console.error);

