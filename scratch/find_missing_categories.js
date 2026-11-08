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
    console.log(`Searching: ${url}`);
    try {
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
    } catch (e) {
        console.error(`Search failed for ${query}:`, e.message);
        return [];
    }
}

async function getCandidateMeta(id, slug) {
    const detailUrl = `https://openclipart.org/detail/${id}/${slug}`;
    try {
        const html = await fetchText(detailUrl);
        const jsonLdMatch = html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/);
        let title = slug.replace(/-/g, ' ');
        let artist = 'Unknown';
        let license = 'CC0 1.0 Universal (Public Domain)';
        let svgUrl = `https://openclipart.org/download/${id}/${slug}.svg`;

        if (jsonLdMatch) {
            try {
                const ld = JSON.parse(jsonLdMatch[1]);
                if (ld.name) title = ld.name;
                if (ld.author && ld.author.name) artist = ld.author.name;
                if (ld.license) license = ld.license;
            } catch (e) {}
        }

        const dlMatch = html.match(/href="([^"]*\/download\/[0-9]+\/[^"]*\.svg)"/i);
        if (dlMatch) {
            svgUrl = dlMatch[1];
            if (!svgUrl.startsWith('http')) {
                svgUrl = 'https://openclipart.org' + svgUrl;
            }
        }

        return { id, slug, detailUrl, title, artist, license, svgUrl };
    } catch (e) {
        return null;
    }
}

async function main() {
    const queries = [
        { q: 'pizza coloring', cat: 'food' },
        { q: 'cupcake coloring', cat: 'food' },
        { q: 'watermelon coloring', cat: 'food' },
        { q: 'ice cream coloring', cat: 'food' },
        { q: 'number coloring', cat: 'letters' },
        { q: 'number 1 coloring', cat: 'letters' },
        { q: 'number 2 coloring', cat: 'letters' },
        { q: 'number 3 coloring', cat: 'letters' },
        { q: 'alphabet coloring', cat: 'letters' }
    ];

    const results = [];
    for (const item of queries) {
        const cands = await searchCandidates(item.q);
        console.log(`Query "${item.q}" found ${cands.length} items`);
        for (const c of cands.slice(0, 4)) {
            const meta = await getCandidateMeta(c.id, c.slug);
            if (meta) {
                const safeSlug = meta.slug.replace(/[^a-zA-Z0-9_]/g, '_').substring(0, 20);
                const localName = `${item.cat}_${meta.id}_${safeSlug}.svg`;
                const dest = path.join(outDir, localName);
                if (!fs.existsSync(dest)) {
                    console.log(`Downloading ${localName} from ${meta.svgUrl}...`);
                    try {
                        await downloadFile(meta.svgUrl, dest);
                        results.push({ ...meta, localFile: localName, category: item.cat });
                    } catch (e) {
                        console.error(`Download failed: ${e.message}`);
                    }
                } else {
                    results.push({ ...meta, localFile: localName, category: item.cat });
                }
            }
        }
    }
    fs.writeFileSync('scratch/missing_downloaded.json', JSON.stringify(results, null, 2));
    console.log(`Downloaded ${results.length} new items.`);
}

main();

