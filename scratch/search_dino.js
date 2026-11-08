const https = require('https');

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

async function searchCandidates(query) {
    const url = `https://openclipart.org/search/?query=${encodeURIComponent(query)}`;
    const html = await fetchText(url);
    const regex = /href="\/detail\/([0-9]+)\/([^"\/]+)"/g;
    const matches = [];
    let m;
    while ((m = regex.exec(html)) !== null) {
        if (!matches.some(x => x.id === m[1])) {
            matches.push({ id: m[1], slug: m[2] });
        }
    }
    return matches;
}

async function main() {
    console.log('Searching stegosaurus:');
    const stego = await searchCandidates('stegosaurus');
    console.log(stego);

    console.log('Searching pterodactyl:');
    const ptero = await searchCandidates('pterodactyl');
    console.log(ptero);

    console.log('Searching brontosaurus:');
    const bronto = await searchCandidates('brontosaurus');
    console.log(bronto);
}

main().catch(console.error);

