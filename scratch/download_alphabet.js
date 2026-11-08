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
        { id: '87055', name: 'letters_87055_c_is_for_car.svg' },
        { id: '87067', name: 'letters_87067_e_is_for_egg.svg' },
        { id: '87073', name: 'letters_87073_f_is_for_flower.svg' },
        { id: '88633', name: 'letters_88633_j_is_for_jar.svg' },
        { id: '88651', name: 'letters_88651_m_is_for_mug.svg' },
        { id: '88813', name: 'letters_88813_r_is_for_rainbow.svg' },
        { id: '88999', name: 'letters_88999_t_is_for_teapot.svg' },
        { id: '89005', name: 'letters_89005_u_is_for_umbrella.svg' },
        { id: '89029', name: 'letters_89029_y_is_for_yarn.svg' }
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

