const fs = require('fs');
const path = require('path');
const https = require('https');

const outDir = path.join(__dirname, 'downloaded_templates');
if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

function fetchText(url) {
    return new Promise((resolve, reject) => {
        const req = https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } }, (res) => {
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
        const req = https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } }, (res) => {
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
    // Động vật cả con
    { query: 'dog coloring', catId: 'animals', catVi: 'Động vật' },
    { query: 'elephant coloring', catId: 'animals', catVi: 'Động vật' },
    { query: 'bear coloring', catId: 'animals', catVi: 'Động vật' },
    { query: 'rabbit coloring', catId: 'animals', catVi: 'Động vật' },
    { query: 'bird coloring', catId: 'animals', catVi: 'Động vật' },
    { query: 'butterfly coloring', catId: 'animals', catVi: 'Động vật' },
    { query: 'horse coloring', catId: 'animals', catVi: 'Động vật' },

    // Phương tiện chung chung
    { query: 'bus coloring', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'airplane coloring', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'sailboat coloring', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'train coloring', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'truck coloring', catId: 'vehicles', catVi: 'Phương tiện' },
    { query: 'bicycle coloring', catId: 'vehicles', catVi: 'Phương tiện' },

    // Biển
    { query: 'dolphin coloring', catId: 'sea', catVi: 'Biển' },
    { query: 'turtle coloring', catId: 'sea', catVi: 'Biển' },
    { query: 'crab coloring', catId: 'sea', catVi: 'Biển' },
    { query: 'starfish coloring', catId: 'sea', catVi: 'Biển' },
    { query: 'octopus coloring', catId: 'sea', catVi: 'Biển' },

    // Hoa lá & Cây
    { query: 'tulip coloring', catId: 'flowers', catVi: 'Hoa lá' },
    { query: 'rose coloring', catId: 'flowers', catVi: 'Hoa lá' },
    { query: 'tree coloring', catId: 'flowers', catVi: 'Hoa lá' },
    { query: 'leaf coloring', catId: 'flowers', catVi: 'Hoa lá' },

    // Chữ số & Chữ cái
    { query: 'number 1 coloring', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'number 2 coloring', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'number 3 coloring', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'number 4 coloring', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'number 5 coloring', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'letter B coloring', catId: 'letters', catVi: 'Chữ số & chữ cái' },
    { query: 'letter D coloring', catId: 'letters', catVi: 'Chữ số & chữ cái' }
];

async function run() {
    console.log(`Starting search across ${queries.length} queries...`);
    const results = [];

    for (const q of queries) {
        console.log(`Searching for "${q.query}"...`);
        try {
            const hits = await searchCandidates(q.query);
            console.log(`  -> Found ${hits.length} hits`);
            // Check top 3 hits
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
                    // Filter: size < 120KB and > 1KB
                    if (stat.size > 800 && stat.size < 120000) {
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
            console.log(`  Search error for ${q.query}: ${e.message}`);
        }
    }

    fs.writeFileSync(path.join(__dirname, 'search_v2_results.json'), JSON.stringify(results, null, 2));
    console.log(`\nSearch complete! Found ${results.length} candidate files.`);
}

run().catch(console.error);

