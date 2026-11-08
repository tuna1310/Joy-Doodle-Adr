const https = require('https');

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

async function run() {
    const html = await fetchText('https://openclipart.org/search/?query=alphabet%20coloring');
    const regex = /href="\/detail\/([0-9]+)\/([^"]+)"/g;
    let m;
    const found = [];
    while ((m = regex.exec(html)) !== null) {
        if (!found.some(x => x.id === m[1])) {
            found.push({ id: m[1], slug: m[2] });
        }
    }
    console.log('All alphabet coloring:', found);
}

run();

