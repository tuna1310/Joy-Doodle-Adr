const fs = require('fs');
const path = require('path');
const https = require('https');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

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

// Evaluate SVG with scale to fill and strict region counting
function evaluateSvg(svgStr) {
    const resvg = new Resvg(svgStr, { fitTo: { mode: 'width', value: 1024 } });
    const rawPng = PNG.sync.read(resvg.render().asPng());

    // Find bounding box
    let minX = rawPng.width, maxX = 0, minY = rawPng.height, maxY = 0;
    for (let y = 0; y < rawPng.height; y++) {
        for (let x = 0; x < rawPng.width; x++) {
            const idx = (y * rawPng.width + x) * 4;
            const a = rawPng.data[idx + 3];
            if (a > 50) {
                const lum = 0.299 * rawPng.data[idx] + 0.587 * rawPng.data[idx+1] + 0.114 * rawPng.data[idx+2];
                if (lum < 220) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
    }

    if (minX >= maxX || minY >= maxY) return null;

    const cropW = maxX - minX + 1;
    const cropH = maxY - minY + 1;
    // Scale crop to fill 880x880 inside 1024x1024
    const scale = Math.min(880 / cropW, 880 / cropH);
    const dstW = Math.round(cropW * scale);
    const dstH = Math.round(cropH * scale);
    const offX = Math.round((1024 - dstW) / 2);
    const offY = Math.round((1024 - dstH) / 2);

    const mask = new Uint8Array(1024 * 1024);
    for (let y = 0; y < dstH; y++) {
        for (let x = 0; x < dstW; x++) {
            const srcX = minX + Math.floor(x / scale);
            const srcY = minY + Math.floor(y / scale);
            const sIdx = (srcY * rawPng.width + srcX) * 4;
            const a = rawPng.data[sIdx + 3];
            if (a > 50) {
                const lum = 0.299 * rawPng.data[sIdx] + 0.587 * rawPng.data[sIdx+1] + 0.114 * rawPng.data[sIdx+2];
                if (lum < 220) {
                    mask[(offY + y) * 1024 + (offX + x)] = 1;
                }
            }
        }
    }

    // Dilate 2px
    const dilated = new Uint8Array(1024 * 1024);
    for (let y = 2; y < 1022; y++) {
        for (let x = 2; x < 1022; x++) {
            if (mask[y * 1024 + x] === 1) {
                for (let dy = -2; dy <= 2; dy++) {
                    for (let dx = -2; dx <= 2; dx++) {
                        dilated[(y + dy) * 1024 + (x + dx)] = 1;
                    }
                }
            }
        }
    }

    // Flood fill exterior
    const visited = new Uint8Array(1024 * 1024);
    const queue = [];
    for (let x = 0; x < 1024; x++) {
        if (dilated[x] === 0) { visited[x] = 1; queue.push(x); }
        const b = 1023 * 1024 + x;
        if (dilated[b] === 0 && visited[b] === 0) { visited[b] = 1; queue.push(b); }
    }
    for (let y = 0; y < 1024; y++) {
        const l = y * 1024;
        if (dilated[l] === 0 && visited[l] === 0) { visited[l] = 1; queue.push(l); }
        const r = y * 1024 + 1023;
        if (dilated[r] === 0 && visited[r] === 0) { visited[r] = 1; queue.push(r); }
    }

    let head = 0;
    while (head < queue.length) {
        const cur = queue[head++];
        const cx = cur % 1024;
        const cy = Math.floor(cur / 1024);
        const nb = [
            cx > 0 ? cur - 1 : -1,
            cx < 1023 ? cur + 1 : -1,
            cy > 0 ? cur - 1024 : -1,
            cy < 1023 ? cur + 1024 : -1
        ];
        for (const n of nb) {
            if (n >= 0 && dilated[n] === 0 && visited[n] === 0) {
                visited[n] = 1;
                queue.push(n);
            }
        }
    }

    let totalInterior = 0;
    const sigRegions = [];
    for (let y = 0; y < 1024; y++) {
        for (let x = 0; x < 1024; x++) {
            const idx = y * 1024 + x;
            if (dilated[idx] === 0 && visited[idx] === 0) {
                visited[idx] = 2;
                const rQ = [idx];
                let rH = 0;
                let area = 0;
                while (rH < rQ.length) {
                    const c = rQ[rH++];
                    area++;
                    const rx = c % 1024;
                    const ry = Math.floor(c / 1024);
                    const rnb = [
                        rx > 0 ? c - 1 : -1,
                        rx < 1023 ? c + 1 : -1,
                        ry > 0 ? c - 1024 : -1,
                        ry < 1023 ? c + 1024 : -1
                    ];
                    for (const rn of rnb) {
                        if (rn >= 0 && dilated[rn] === 0 && visited[rn] === 0) {
                            visited[rn] = 2;
                            rQ.push(rn);
                        }
                    }
                }
                totalInterior += area;
                if (area >= 1000) sigRegions.push(area);
            }
        }
    }

    return {
        totalInterior,
        fillPct: (totalInterior / 1048576 * 100).toFixed(1),
        sigCount: sigRegions.length,
        isPass: totalInterior >= 150000 && sigRegions.length >= 3 && sigRegions.length <= 80
    };
}

async function findCandidatesForQuery(query, catId, catVi) {
    console.log(`Searching for "${query}"...`);
    const hits = await searchCandidates(query);
    for (const h of hits.slice(0, 6)) {
        try {
            const detail = await getClipDetail(h.id, h.slug);
            if (!detail) continue;
            const filename = `${catId}_${h.id}_${h.slug.substring(0, 20).replace(/[^a-zA-Z0-9]/g, '_')}.svg`;
            const dest = path.join(outDir, filename);
            if (!fs.existsSync(dest)) {
                await downloadFile(detail.svgUrl, dest);
            }
            const stat = fs.statSync(dest);
            if (stat.size > 800 && stat.size < 90000) {
                const svgStr = fs.readFileSync(dest, 'utf8');
                const evalRes = evaluateSvg(svgStr);
                if (evalRes && evalRes.isPass) {
                    console.log(`  >>> PERFECT MATCH: ID ${h.id} (${detail.title}) | Area: ${evalRes.fillPct}% (${Math.round(evalRes.totalInterior/1000)}k) | Sig: ${evalRes.sigCount}`);
                    return {
                        id: h.id,
                        file: filename,
                        title: detail.title,
                        cat: catVi,
                        catId,
                        artist: detail.artist,
                        url: detail.detailUrl,
                        statBytes: stat.size,
                        ...evalRes
                    };
                } else if (evalRes) {
                    console.log(`  - ID ${h.id} (${detail.title}): Area ${evalRes.fillPct}%, Sig ${evalRes.sigCount} (Not pass)`);
                }
            }
        } catch (e) {
            console.log(`  Error on ${h.id}: ${e.message}`);
        }
    }
    return null;
}

async function main() {
    const searchTasks = [
        // Vehicles to replace car #181232 and train #323440
        { query: 'car cartoon outline', catId: 'vehicles', catVi: 'Phương tiện' },
        { query: 'car outline', catId: 'vehicles', catVi: 'Phương tiện' },
        { query: 'locomotive outline', catId: 'vehicles', catVi: 'Phương tiện' },
        { query: 'train cartoon outline', catId: 'vehicles', catVi: 'Phương tiện' },
        { query: 'helicopter outline', catId: 'vehicles', catVi: 'Phương tiện' },
        { query: 'rocket outline', catId: 'vehicles', catVi: 'Phương tiện' },

        // Dinosaurs to replace pterodactyl #28420 and brontosaurus #23508
        { query: 'stegosaurus outline', catId: 'dinosaurs', catVi: 'Khủng long' },
        { query: 'stegosaurus cartoon', catId: 'dinosaurs', catVi: 'Khủng long' },
        { query: 'dinosaur cartoon', catId: 'dinosaurs', catVi: 'Khủng long' },
        { query: 't-rex outline', catId: 'dinosaurs', catVi: 'Khủng long' },

        // Scenery to replace castle #16504 and mountains #235489
        { query: 'windmill outline', catId: 'scenery', catVi: 'Phong cảnh đơn giản' },
        { query: 'lighthouse outline', catId: 'scenery', catVi: 'Phong cảnh đơn giản' },
        { query: 'camping tent outline', catId: 'scenery', catVi: 'Phong cảnh đơn giản' },

        // Flowers to replace #230020 and #262028
        { query: 'rose flower outline', catId: 'flowers', catVi: 'Hoa lá' },
        { query: 'tulip flower outline', catId: 'flowers', catVi: 'Hoa lá' },
        { query: 'sunflower outline', catId: 'flowers', catVi: 'Hoa lá' },

        // Fruits to replace #245073 and #256766
        { query: 'orange fruit outline', catId: 'food', catVi: 'Trái cây & Đồ ăn' },
        { query: 'watermelon fruit outline', catId: 'food', catVi: 'Trái cây & Đồ ăn' },
        { query: 'strawberry fruit outline', catId: 'food', catVi: 'Trái cây & Đồ ăn' },
        { query: 'carrot outline', catId: 'food', catVi: 'Trái cây & Đồ ăn' },

        // Numbers 1 to 5
        { query: 'bubble number 1', catId: 'letters', catVi: 'Chữ cái & chữ số' },
        { query: 'number 1 cartoon', catId: 'letters', catVi: 'Chữ cái & chữ số' },
        { query: 'number 2 cartoon', catId: 'letters', catVi: 'Chữ cái & chữ số' },
        { query: 'number 3 cartoon', catId: 'letters', catVi: 'Chữ cái & chữ số' },
        { query: 'number 4 cartoon', catId: 'letters', catVi: 'Chữ cái & chữ số' },
        { query: 'number 5 cartoon', catId: 'letters', catVi: 'Chữ cái & chữ số' }
    ];

    const perfects = [];
    for (const t of searchTasks) {
        const p = await findCandidatesForQuery(t.query, t.catId, t.catVi);
        if (p) perfects.push(p);
    }

    fs.writeFileSync('D:/DoodleStudio/scratch/found_perfects.json', JSON.stringify(perfects, null, 2));
    console.log(`\nFound ${perfects.length} perfect replacement templates!`);
}

main().catch(console.error);

