const https = require('https');
const fs = require('fs');
const path = require('path');

function fetchText(url) {
    return new Promise((resolve, reject) => {
        https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, res => {
            if ((res.statusCode === 301 || res.statusCode === 302) && res.headers.location) {
                let loc = res.headers.location;
                if (!loc.startsWith('http')) loc = 'https://openclipart.org' + loc;
                return fetchText(loc).then(resolve).catch(reject);
            }
            let b = '';
            res.on('data', c => b += c);
            res.on('end', () => resolve(b));
        }).on('error', reject);
    });
}

function downloadFollow(url, dest) {
    return new Promise((resolve, reject) => {
        https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, res => {
            if ((res.statusCode === 301 || res.statusCode === 302) && res.headers.location) {
                let loc = res.headers.location;
                if (!loc.startsWith('http')) loc = 'https://openclipart.org' + loc;
                return downloadFollow(loc, dest).then(resolve).catch(reject);
            }
            if (res.statusCode !== 200) {
                return reject(new Error(`HTTP ${res.statusCode} for ${url}`));
            }
            const f = fs.createWriteStream(dest);
            res.pipe(f);
            f.on('finish', () => { f.close(); resolve(); });
        }).on('error', reject);
    });
}

async function run() {
    const list = [
        { id: '8534', slug: 'simple-fruit-strawberry' },
        { id: '8535', slug: 'simple-fruit-pear' },
        { id: '8537', slug: 'simple-fruit-orange' },
        { id: '8531', slug: 'simple-fruit-grapes' },
        { id: '8532', slug: 'simple-fruit-peach' },
        { id: '8542', slug: 'simple-fruit-strawberry' },
        { id: '8543', slug: 'simple-fruit-pear' },
        { id: '8545', slug: 'simple-fruit-orange' },
        { id: '8539', slug: 'simple-fruit-grapes' }
    ];

    for (const item of list) {
        const dest = path.join('D:/DoodleStudio/scratch/downloaded_templates', `food_${item.id}_${item.slug}.svg`);
        const detailHtml = await fetchText(`https://openclipart.org/detail/${item.id}/${item.slug}`);
        const dlMatch = detailHtml.match(/href="([^"]*\/download\/[0-9]+\/[^"]*\.svg)"/i);
        console.log(`Item ${item.id} (${item.slug}) match:`, dlMatch ? dlMatch[1] : 'NOT FOUND');
        if (dlMatch) {
            let svgUrl = dlMatch[1];
            if (!svgUrl.startsWith('http')) svgUrl = 'https://openclipart.org' + svgUrl;
            console.log(`Downloading ${dest} from ${svgUrl}...`);
            await downloadFollow(svgUrl, dest);
        }
    }
    console.log('Finished!');
}

run().catch(console.error);
