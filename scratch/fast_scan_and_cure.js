const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';

const Q = new Int32Array(1024 * 1024);

function evaluateSvg(svgPath, closeRadius = 3, dilateRadius = 1) {
    try {
        const svgStr = fs.readFileSync(svgPath, 'utf8');
        const resvg = new Resvg(svgStr, { fitTo: { mode: 'width', value: 1024 } });
        const rawPng = PNG.sync.read(resvg.render().asPng());

        let minX = rawPng.width, maxX = 0, minY = rawPng.height, maxY = 0;
        for (let y = 0; y < rawPng.height; y++) {
            const rowOff = y * rawPng.width * 4;
            for (let x = 0; x < rawPng.width; x++) {
                const idx = rowOff + x * 4;
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
        const targetSize = 920; // scale up nicely
        const scale = Math.min(targetSize / cropW, targetSize / cropH);
        const dstW = Math.round(cropW * scale);
        const dstH = Math.round(cropH * scale);
        const offX = Math.round((1024 - dstW) / 2);
        const offY = Math.round((1024 - dstH) / 2);

        const mask = new Uint8Array(1024 * 1024);
        for (let y = 0; y < dstH; y++) {
            const srcY = minY + Math.floor(y / scale);
            const sRow = srcY * rawPng.width * 4;
            const dRow = (offY + y) * 1024 + offX;
            for (let x = 0; x < dstW; x++) {
                const srcX = minX + Math.floor(x / scale);
                const sIdx = sRow + srcX * 4;
                const a = rawPng.data[sIdx + 3];
                if (a > 50) {
                    const lum = 0.299 * rawPng.data[sIdx] + 0.587 * rawPng.data[sIdx+1] + 0.114 * rawPng.data[sIdx+2];
                    if (lum < 220) {
                        mask[dRow + x] = 1;
                    }
                }
            }
        }

        // Morphological closing: Dilation then Erosion
        // 1. Dilate
        let buf1 = new Uint8Array(1024 * 1024);
        const cr = closeRadius;
        for (let y = cr; y < 1024 - cr; y++) {
            const rY = y * 1024;
            for (let x = cr; x < 1024 - cr; x++) {
                if (mask[rY + x] === 1) {
                    for (let dy = -cr; dy <= cr; dy++) {
                        const oY = (y + dy) * 1024;
                        for (let dx = -cr; dx <= cr; dx++) {
                            if (dx*dx + dy*dy <= cr*cr) {
                                buf1[oY + x + dx] = 1;
                            }
                        }
                    }
                }
            }
        }

        // 2. Erode back by (cr - dilateRadius)
        const er = cr - dilateRadius;
        let processed;
        if (er > 0) {
            processed = new Uint8Array(1024 * 1024);
            for (let y = er; y < 1024 - er; y++) {
                const rY = y * 1024;
                for (let x = er; x < 1024 - er; x++) {
                    let allOnes = 1;
                    for (let dy = -er; dy <= er; dy++) {
                        const oY = (y + dy) * 1024;
                        for (let dx = -er; dx <= er; dx++) {
                            if (dx*dx + dy*dy <= er*er) {
                                if (buf1[oY + x + dx] === 0) {
                                    allOnes = 0;
                                    break;
                                }
                            }
                        }
                        if (!allOnes) break;
                    }
                    if (allOnes) processed[rY + x] = 1;
                }
            }
        } else {
            processed = buf1;
        }

        // Flood fill exterior
        const visited = new Uint8Array(1024 * 1024);
        let qHead = 0;
        let qTail = 0;

        for (let x = 0; x < 1024; x++) {
            if (processed[x] === 0) { visited[x] = 1; Q[qTail++] = x; }
            const b = 1023 * 1024 + x;
            if (processed[b] === 0 && visited[b] === 0) { visited[b] = 1; Q[qTail++] = b; }
        }
        for (let y = 0; y < 1024; y++) {
            const l = y * 1024;
            if (processed[l] === 0 && visited[l] === 0) { visited[l] = 1; Q[qTail++] = l; }
            const r = y * 1024 + 1023;
            if (processed[r] === 0 && visited[r] === 0) { visited[r] = 1; Q[qTail++] = r; }
        }

        while (qHead < qTail) {
            const cur = Q[qHead++];
            const cx = cur % 1024;
            const cy = (cur / 1024) | 0;

            if (cx > 0) {
                const n = cur - 1;
                if (processed[n] === 0 && visited[n] === 0) { visited[n] = 1; Q[qTail++] = n; }
            }
            if (cx < 1023) {
                const n = cur + 1;
                if (processed[n] === 0 && visited[n] === 0) { visited[n] = 1; Q[qTail++] = n; }
            }
            if (cy > 0) {
                const n = cur - 1024;
                if (processed[n] === 0 && visited[n] === 0) { visited[n] = 1; Q[qTail++] = n; }
            }
            if (cy < 1023) {
                const n = cur + 1024;
                if (processed[n] === 0 && visited[n] === 0) { visited[n] = 1; Q[qTail++] = n; }
            }
        }

        let totalInterior = 0;
        const allRegions = [];
        const sigRegions = [];

        for (let y = 0; y < 1024; y++) {
            const rY = y * 1024;
            for (let x = 0; x < 1024; x++) {
                const idx = rY + x;
                if (processed[idx] === 0 && visited[idx] === 0) {
                    visited[idx] = 2;
                    let rHead = 0;
                    let rTail = 0;
                    Q[rTail++] = idx;
                    let area = 0;

                    while (rHead < rTail) {
                        const c = Q[rHead++];
                        area++;
                        const rx = c % 1024;
                        const ry = (c / 1024) | 0;

                        if (rx > 0) {
                            const n = c - 1;
                            if (processed[n] === 0 && visited[n] === 0) { visited[n] = 2; Q[rTail++] = n; }
                        }
                        if (rx < 1023) {
                            const n = c + 1;
                            if (processed[n] === 0 && visited[n] === 0) { visited[n] = 2; Q[rTail++] = n; }
                        }
                        if (ry > 0) {
                            const n = c - 1024;
                            if (processed[n] === 0 && visited[n] === 0) { visited[n] = 2; Q[rTail++] = n; }
                        }
                        if (ry < 1023) {
                            const n = c + 1024;
                            if (processed[n] === 0 && visited[n] === 0) { visited[n] = 2; Q[rTail++] = n; }
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
        console.error('Err:', e.message);
        return null;
    }
}

// Test on suspect files first
const suspects = [
    'vehicles_181232_number_image_car.svg',
    'vehicles_323440_wagon_train.svg',
    'flowers_230020_alp_lily.svg',
    'dinosaurs_28420_cartoon_pterodactyl.svg',
    'dinosaurs_23508_cartoon_brontosaurus.svg',
    'letters_190575_v_for_violin_for_col.svg',
    'flowers_262028_trees.svg',
    'scenery_16504_castle.svg',
    'letters_190572_p_for_pencil_for_col.svg',
    'food_245073_lemon_lineart.svg',
    'flowers_333586_lilly_flower_abstrac.svg',
    'animals_353970_lutz_toy_puppy_outli.svg',
    'animals_354654_lutz_rabbit_3_outlin.svg'
];

console.log('Testing suspect files with closing and scaling...');
for (const f of suspects) {
    const full = path.join(downloadDir, f);
    if (!fs.existsSync(full)) {
        console.log(`Missing ${f}`);
        continue;
    }
    const res = evaluateSvg(full, 3, 1);
    if (res) {
        console.log(`${f} -> ${res.isPass ? 'PASS' : 'FAIL'} | Area: ${res.fillPct}% (${Math.round(res.totalInterior/1000)}k px) | Sig: ${res.sigCount} (Total: ${res.allRegionsCount})`);
    } else {
        console.log(`${f} -> ERROR`);
    }
}

