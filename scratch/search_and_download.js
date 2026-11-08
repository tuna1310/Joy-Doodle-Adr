const fs = require('fs');
const path = require('path');
const https = require('https');

const outDir = path.join(__dirname, 'downloaded_templates');
if (!fs.existsSync(outDir)) {
    fs.mkdirSync(outDir, { recursive: true });
}

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

async function searchCandidateIds(query) {
    const searchUrl = `https://openclipart.org/search/?query=${encodeURIComponent(query)}`;
    try {
        const html = await fetchText(searchUrl);
        const regex = /href="\/detail\/([0-9]+)\/([^"]+)"/g;
        const matches = [];
        let m;
        while ((m = regex.exec(html)) !== null) {
            matches.push({ id: m[1], slug: m[2] });
        }
        return matches;
    } catch (e) {
        console.error(`Search failed for ${query}:`, e.message);
        return [];
    }
}

async function getClipDetail(id, slug) {
    const detailUrl = `https://openclipart.org/detail/${id}/${slug}`;
    try {
        const html = await fetchText(detailUrl);
        // Look for JSON-LD
        const jsonLdMatch = html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/i);
        if (!jsonLdMatch) return null;
        let ld;
        try {
            ld = JSON.parse(jsonLdMatch[1].trim());
        } catch (e) {
            return null;
        }

        let artist = 'Anonymous';
        if (ld.artist && ld.artist.name) artist = ld.artist.name;
        else if (ld.author && ld.author.name) artist = ld.author.name;

        let title = ld.name || slug;
        let svgUrl = null;
        if (ld.image && ld.image.url) svgUrl = ld.image.url;
        else if (ld.url) svgUrl = ld.url;

        if (!svgUrl) {
            svgUrl = `https://openclipart.org/download/${id}`;
        }

        return {
            id,
            slug,
            detailUrl,
            title,
            artist,
            license: 'CC0 1.0 Universal (Public Domain)',
            svgUrl
        };
    } catch (e) {
        console.error(`Detail failed for ${id}:`, e.message);
        return null;
    }
}

async function run() {
    const categories = [
        {
            catId: 'animals',
            catNameVi: 'Động vật',
            queries: ['coloring cat', 'coloring dog', 'coloring elephant', 'coloring bear', 'coloring rabbit', 'coloring lion', 'coloring owl']
        },
        {
            catId: 'flowers',
            catNameVi: 'Hoa lá',
            queries: ['coloring sunflower', 'coloring tulip', 'coloring rose', 'coloring daisy', 'coloring lotus', 'flower outline coloring']
        },
        {
            catId: 'food',
            catNameVi: 'Trái cây & Đồ ăn',
            queries: ['coloring apple', 'coloring banana', 'coloring strawberry', 'ice cream coloring', 'cupcake coloring', 'fruit coloring']
        },
        {
            catId: 'vehicles',
            catNameVi: 'Phương tiện',
            queries: ['coloring car', 'coloring bus', 'coloring airplane', 'sailboat coloring', 'train coloring', 'truck coloring']
        },
        {
            catId: 'dinosaurs',
            catNameVi: 'Khủng long',
            queries: ['coloring dinosaur', 'dino outline', 'stegosaurus coloring', 'trex coloring', 'triceratops coloring']
        },
        {
            catId: 'sea',
            catNameVi: 'Biển',
            queries: ['coloring fish', 'dolphin coloring', 'turtle coloring', 'octopus coloring', 'whale coloring', 'seahorse coloring']
        },
        {
            catId: 'scenery',
            catNameVi: 'Phong cảnh đơn giản',
            queries: ['coloring house', 'coloring rainbow', 'mountain coloring', 'lighthouse coloring', 'balloon coloring']
        },
        {
            catId: 'letters',
            catNameVi: 'Chữ số và chữ cái',
            queries: ['letter coloring', 'number coloring', 'alphabet coloring', 'letter a coloring', 'number 1 coloring']
        }
    ];

    const results = [];

    for (const cat of categories) {
        console.log(`\n=== Finding for Category: ${cat.catNameVi} (${cat.catId}) ===`);
        const foundForCat = [];
        const seenIds = new Set();

        for (const q of cat.queries) {
            if (foundForCat.length >= 5) break;
            const candidates = await searchCandidateIds(q);
            for (const cand of candidates) {
                if (foundForCat.length >= 5) break;
                if (seenIds.has(cand.id)) continue;
                seenIds.add(cand.id);

                const detail = await getClipDetail(cand.id, cand.slug);
                if (!detail) continue;

                // Download SVG
                const filename = `${cat.catId}_${cand.id}_${cand.slug.substring(0, 20).replace(/[^a-zA-Z0-9]/g, '_')}.svg`;
                const destPath = path.join(outDir, filename);

                try {
                    console.log(`Downloading: ${detail.title} (${cand.id}) -> ${filename}`);
                    await downloadFile(detail.svgUrl, destPath);
                    const stat = fs.statSync(destPath);
                    if (stat.size > 800) {
                        detail.localFile = filename;
                        detail.fileSizeBytes = stat.size;
                        detail.category = cat.catNameVi;
                        detail.catId = cat.catId;
                        foundForCat.push(detail);
                        console.log(`  -> SUCCESS: ${stat.size} bytes`);
                    } else {
                        console.log(`  -> File too small (${stat.size}b), removing`);
                        fs.unlinkSync(destPath);
                    }
                } catch (e) {
                    console.log(`  -> Download failed: ${e.message}`);
                }
            }
        }
        results.push(...foundForCat);
    }

    fs.writeFileSync(path.join(__dirname, 'verified_downloads.json'), JSON.stringify(results, null, 2));
    console.log(`\nFinished! Total verified downloads: ${results.length}`);
}

run().catch(console.error);

