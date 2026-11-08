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

async function main() {
    const targets = [
        { query: 'pear outline', catId: 'food', catVi: 'Trái cây & Đồ ăn' },
        { query: 'lemon outline', catId: 'food', catVi: 'Trái cây & Đồ ăn' },
        { query: 'dinosaur cartoon outline', catId: 'dinosaurs', catVi: 'Khủng long' }
    ];

    for (const t of targets) {
        console.log(`Searching: ${t.query}`);
        const hits = await searchCandidates(t.query);
        for (const h of hits.slice(0, 3)) {
            const detail = await getClipDetail(h.id, h.slug);
            if (!detail) continue;
            const filename = `${t.catId}_${h.id}_${h.slug.substring(0, 20).replace(/[^a-zA-Z0-9]/g, '_')}.svg`;
            const dest = path.join(outDir, filename);
            if (!fs.existsSync(dest)) {
                await downloadFile(detail.svgUrl, dest);
            }
            const stat = fs.statSync(dest);
            if (stat.size > 800 && stat.size < 80000) {
                console.log(`  + Downloaded ${h.id} (${detail.title}): ${stat.size} bytes`);
            }
        }
    }
}

main().catch(console.error);

