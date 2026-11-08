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
    const dinos = [
        { id: '28420', slug: 'cartoon-pterodactyl' },
        { id: '23508', slug: 'cartoon-brontosaurus' },
        { id: '339881', slug: 'stegosaurus-cartoon' }
    ];

    for (const d of dinos) {
        const detail = await getClipDetail(d.id, d.slug);
        if (!detail) continue;
        const filename = `dinosaurs_${d.id}_${d.slug.substring(0, 20).replace(/[^a-zA-Z0-9]/g, '_')}.svg`;
        const dest = path.join(outDir, filename);
        await downloadFile(detail.svgUrl, dest);
        const stat = fs.statSync(dest);
        console.log(`Downloaded ${filename} (${stat.size} bytes) by ${detail.artist}`);
    }
}

main().catch(console.error);

