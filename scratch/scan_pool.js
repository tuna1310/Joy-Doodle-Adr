const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';

function analyzeSvg(svgPath, closeRadius = 3) {
    try {
        const svgStr = fs.readFileSync(svgPath, 'utf8');
        const resvg = new Resvg(svgStr, { fitTo: { mode: 'width', value: 1024 } });
        const rawPng = PNG.sync.read(resvg.render().asPng());

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
        const targetSize = 900;
        const scale = Math.min(targetSize / cropW, targetSize / cropH);
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

        // Morphological dilation (stroke thickening / gap closing)
        const rad = closeRadius;
        const dilated = new Uint8Array(1024 * 1024);
        for (let y = rad; y < 1024 - rad; y++) {
            for (let x = rad; x < 1024 - rad; x++) {
                if (mask[y * 1024 + x] === 1) {
                    for (let dy = -rad; dy <= rad; dy++) {
                        for (let dx = -rad; dx <= rad; dx++) {
                            if (dx*dx + dy*dy <= rad*rad) {
                                dilated[(y + dy) * 1024 + (x + dx)] = 1;
                            }
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
        const allRegions = [];
        const sigRegions = []; // >= 1000 px

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
                    allRegions.push(area);
                    totalInterior += area;
                    if (area >= 1000) sigRegions.push(area);
                }
            }
        }

        return {
            totalInterior,
            fillPct: (totalInterior / 1048576 * 100).toFixed(1),
            allRegionsCount: allRegions.length,
            sigCount: sigRegions.length,
            isPass: totalInterior >= 150000 && sigRegions.length >= 3 && sigRegions.length <= 80
        };
    } catch (e) {
        return null;
    }
}

const allFiles = fs.readdirSync(downloadDir).filter(f => f.endsWith('.svg'));
console.log(`Scanning ${allFiles.length} files...`);

const results = [];
for (const file of allFiles) {
    const fullPath = path.join(downloadDir, file);
    const r = analyzeSvg(fullPath, 2); // test with rad 2
    if (r) {
        results.push({ file, ...r });
    }
}

const passed = results.filter(r => r.isPass);
console.log(`Finished scan: ${passed.length} passed out of ${results.length}`);

fs.writeFileSync('scratch/pool_scan_results.json', JSON.stringify(results, null, 2));
fs.writeFileSync('scratch/pool_passed.json', JSON.stringify(passed, null, 2));

console.log('Sample passed:');
console.log(passed.slice(0, 10).map(p => `${p.file} -> ${p.fillPct}% (${Math.round(p.totalInterior/1000)}k), sig: ${p.sigCount}`));

