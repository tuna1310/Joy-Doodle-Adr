const https = require('https');
const fs = require('fs');
const path = require('path');

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
        { id: '8534', name: 'food_8534_strawberry.svg' },
        { id: '8535', name: 'food_8535_pear.svg' },
        { id: '8537', name: 'food_8537_orange.svg' },
        { id: '8531', name: 'food_8531_grapes.svg' },
        { id: '8532', name: 'food_8532_peach.svg' },
        { id: '8542', name: 'food_8542_strawberry.svg' },
        { id: '8543', name: 'food_8543_pear.svg' },
        { id: '8545', name: 'food_8545_orange.svg' },
        { id: '8539', name: 'food_8539_grapes.svg' }
    ];

    for (const item of list) {
        const dest = path.join('D:/DoodleStudio/scratch/downloaded_templates', item.name);
        try {
            await downloadFollow(`https://openclipart.org/download/${item.id}`, dest);
            console.log(`Downloaded ${item.name} (${fs.statSync(dest).size} bytes)`);
        } catch (e) {
            console.log(`Failed ${item.name}: ${e.message}`);
        }
    }
}

run();

