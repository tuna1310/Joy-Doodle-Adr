const fs = require('fs');
const path = require('path');
const https = require('https');

const outDir = path.join(__dirname, 'downloaded_templates');

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

const targetIds = [
    { id: '281169', slug: 'happy-dinosaur-outline', catId: 'dinosaurs', catVi: 'Khủng long', title: 'Happy Dinosaur Outline' },
    { id: '234370', slug: 'rawr-dinosaur-outline', catId: 'dinosaurs', catVi: 'Khủng long', title: 'Triceratops Dino Outline' },
    { id: '32701', slug: 'dinosaur---stegosaurus', catId: 'dinosaurs', catVi: 'Khủng long', title: 'Stegosaurus Outline' },
    { id: '234766', slug: 'rainbow-for-coloring', catId: 'scenery', catVi: 'Phong cảnh đơn giản', title: 'Rainbow for Coloring' },
    { id: '76790', slug: 'house-line-art', catId: 'scenery', catVi: 'Phong cảnh đơn giản', title: 'House Line Art' },
    { id: '235489', slug: 'mountain-outlines-simpler', catId: 'scenery', catVi: 'Phong cảnh đơn giản', title: 'Mountain Outlines' },
    { id: '179265', slug: 'fish-outline', catId: 'sea', catVi: 'Biển', title: 'Fish Outline' },
    { id: '178560', slug: 'turtle-side-outline', catId: 'sea', catVi: 'Biển', title: 'Turtle Side Outline' },
    { id: '215780', slug: 'baby-octopus-coloring-page', catId: 'sea', catVi: 'Biển', title: 'Baby Octopus Coloring' },
    { id: '178129', slug: 'simple-rose-outline', catId: 'flowers', catVi: 'Hoa lá', title: 'Simple Rose Outline' },
    { id: '21235', slug: 'tulip-outline', catId: 'flowers', catVi: 'Hoa lá', title: 'Tulip Outline' }
];

async function addTargets() {
    const vetted = JSON.parse(fs.readFileSync(path.join(__dirname, 'vetted_candidates.json'), 'utf8'));

    for (const t of targetIds) {
        console.log(`Checking ${t.id} (${t.slug})...`);
        const detailUrl = `https://openclipart.org/detail/${t.id}/${t.slug}`;
        try {
            const html = await fetchText(detailUrl);
            const jsonLdMatch = html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/i);
            if (!jsonLdMatch) {
                console.log(`  -> No JSON-LD for ${t.id}`);
                continue;
            }
            const ld = JSON.parse(jsonLdMatch[1].trim());
            let artist = 'Anonymous';
            if (ld.artist && ld.artist.name) artist = ld.artist.name;
            else if (ld.author && ld.author.name) artist = ld.author.name;

            let svgUrl = (ld.image && ld.image.url) ? ld.image.url : `https://openclipart.org/download/${t.id}`;
            const filename = `${t.catId}_${t.id}_${t.slug.substring(0, 20).replace(/[^a-zA-Z0-9]/g, '_')}.svg`;
            const destPath = path.join(outDir, filename);

            await downloadFile(svgUrl, destPath);
            const stat = fs.statSync(destPath);
            if (stat.size > 800) {
                console.log(`  -> OK: ${stat.size} bytes (${artist})`);
                vetted.push({
                    id: t.id,
                    slug: t.slug,
                    detailUrl,
                    title: ld.name || t.title,
                    artist,
                    license: 'CC0 1.0 Universal (Public Domain)',
                    svgUrl,
                    localFile: filename,
                    fileSizeBytes: stat.size,
                    category: t.catVi,
                    catId: t.catId,
                    visualDesc: 'Nét line-art đen trắng rõ ràng, các vùng kín'
                });
            }
        } catch (e) {
            console.log(`  -> Error for ${t.id}: ${e.message}`);
        }
    }

    fs.writeFileSync(path.join(__dirname, 'vetted_candidates.json'), JSON.stringify(vetted, null, 2));
    console.log(`Total vetted now: ${vetted.length}`);
}

addTargets().catch(console.error);

