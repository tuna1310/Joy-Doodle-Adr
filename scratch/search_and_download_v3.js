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
                try { fs.unlinkSync(destPath); } catch (e) {}
                return downloadFile(redirectUrl, destPath).then(resolve).catch(reject);
            }
            if (res.statusCode !== 200) {
                file.close();
                try { if (fs.existsSync(destPath)) fs.unlinkSync(destPath); } catch (e) {}
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
            try { if (fs.existsSync(destPath)) fs.unlinkSync(destPath); } catch (e) {}
            reject(err);
        });
        req.setTimeout(20000, () => {
            req.destroy();
            file.close();
            try { if (fs.existsSync(destPath)) fs.unlinkSync(destPath); } catch (e) {}
            reject(new Error(`Timeout downloading ${url}`));
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

async function getClipDetail(id, slug) {
    const detailUrl = `https://openclipart.org/detail/${id}/${slug}`;
    const html = await fetchText(detailUrl);
    const jsonLdMatch = html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/i);
    if (!jsonLdMatch) return null;
    let ld;
    try {
        ld = JSON.parse(jsonLdMatch[1].trim());
    } catch (e) {
        return null;
    }
    const artist = (ld.artist && ld.artist.name) || (ld.author && ld.author.name) || 'Anonymous';
    const title = ld.name || slug;
    const svgUrl = (ld.image && ld.image.url) ? ld.image.url : `https://openclipart.org/download/${id}`;
    return {
        id,
        slug,
        detailUrl,
        title,
        artist,
        license: 'CC0 1.0 Universal (Public Domain)',
        svgUrl
    };
}

const queries = [
    { query: 'sailboat outline', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'boat outline', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'train outline', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'bus outline', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'truck outline', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'bicycle outline', catId: 'vehicles', catVi: 'Phương tiện' },

    { query: 'dolphin outline', catId: 'sea', catVi: 'Biển' },
    { query: 'crab outline', catId: 'sea', catVi: 'Biển' },
    { query: 'starfish outline', catId: 'sea', catVi: 'Biển' },
    { query: 'turtle outline', catId: 'sea', catVi: 'Biển' },

    { query: 'tulip outline', catId: 'flowers', catVi: 'Hoa lá' },
    { query: 'rose outline', catId: 'flowers', catVi: 'Hoa lá' },
    { query: 'tree outline', catId: 'flowers', catVi: 'Hoa lá' },
    { query: 'leaf outline', catId: 'flowers', catVi: 'Hoa lá' },

    { query: 'elephant outline', catId: 'animals', catVi: 'Động vật' },
    { query: 'dog outline', catId: 'animals', catVi: 'Động vật' },
    { query: 'bear outline', catId: 'animals', catVi: 'Động vật' },
    { query: 'rabbit outline', catId: 'animals', catVi: 'Động vật' },
    { query: 'bird outline', catId: 'animals', catVi: 'Động vật' },

    { query: 'number 1 line art', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'number 2 line art', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'number 3 line art', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'number 4 line art', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'number 5 line art', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'letter E for', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'letter F for', catId: 'letters', catVi: 'Chữ số & chữ cái' }
];

async function run() {
    const results = [];
    for (const q of queries) {
        console.log(`Searching for "${q.query}"...`);
        try {
            const hits = await searchCandidates(q.query);
            console.log(`  -> Found ${hits.length} hits`);
            for (const hit of hits.slice(0, 3)) {
                try {
                    const detail = await getClipDetail(hit.id, hit.slug);
                    if (!detail) continue;

                    const localFile = `${q.catId}_${hit.id}_${hit.slug.substring(0, 20).replace(/[^a-zA-Z0-9]/g, '_')}.svg`;
                    const destPath = path.join(outDir, localFile);

                    if (!fs.existsSync(destPath)) {
                        await downloadFile(detail.svgUrl, destPath);
                    }

                    const stat = fs.statSync(destPath);
                    if (stat.size > 500 && stat.size < 100000) {
                        results.push({
                            ...detail,
                            localFile,
                            fileSizeBytes: stat.size,
                            category: q.catVi,
                            catId: q.catId,
                            queryMatched: q.query
                        });
                        console.log(`    + Added: ${hit.id} (${detail.title}) - ${stat.size} bytes`);
                    } else {
                        console.log(`    - Skipped (size ${stat.size}): ${hit.id}`);
                    }
                } catch (err) {
                    console.log(`    x Error getting ${hit.id}: ${err.message}`);
                }
            }
        } catch (e) {
            console.log(`  Search error: ${e.message}`);
        }
    }

    fs.writeFileSync(path.join(__dirname, 'search_v3_results.json'), JSON.stringify(results, null, 2));
    console.log(`\nSearch V3 complete! Found ${results.length} files.`);
}

run().catch(console.error);

