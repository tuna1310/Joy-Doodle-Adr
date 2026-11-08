const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const Q = new Int32Array(1024 * 1024);

function evaluateSvg(svgPath) {
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
        const targetSize = 900;
        const scale = Math.min(targetSize / cropW, targetSize / cropH);
        const dstW = Math.round(cropW * scale);
        const dstH = Math.round(cropH * scale);
        const offX = Math.round((1024 - dstW) / 2);
        const offY = Math.round((1024 - dstH) / 2);

        const rad = 2;
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
                        const py = offY + y;
                        const px = offX + x;
                        for (let dy = -rad; dy <= rad; dy++) {
                            const oY = (py + dy) * 1024;
                            for (let dx = -rad; dx <= rad; dx++) {
                                mask[oY + px + dx] = 1;
                            }
                        }
                    }
                }
            }
        }

        const visited = new Uint8Array(1024 * 1024);
        let qHead = 0;
        let qTail = 0;

        for (let x = 0; x < 1024; x++) {
            if (mask[x] === 0) { visited[x] = 1; Q[qTail++] = x; }
            const b = 1023 * 1024 + x;
            if (mask[b] === 0 && visited[b] === 0) { visited[b] = 1; Q[qTail++] = b; }
        }
        for (let y = 0; y < 1024; y++) {
            const l = y * 1024;
            if (mask[l] === 0 && visited[l] === 0) { visited[l] = 1; Q[qTail++] = l; }
            const r = y * 1024 + 1023;
            if (mask[r] === 0 && visited[r] === 0) { visited[r] = 1; Q[qTail++] = r; }
        }

        while (qHead < qTail) {
            const cur = Q[qHead++];
            const cx = cur % 1024;
            const cy = (cur / 1024) | 0;

            if (cx > 0) {
                const n = cur - 1;
                if (mask[n] === 0 && visited[n] === 0) { visited[n] = 1; Q[qTail++] = n; }
            }
            if (cx < 1023) {
                const n = cur + 1;
                if (mask[n] === 0 && visited[n] === 0) { visited[n] = 1; Q[qTail++] = n; }
            }
            if (cy > 0) {
                const n = cur - 1024;
                if (mask[n] === 0 && visited[n] === 0) { visited[n] = 1; Q[qTail++] = n; }
            }
            if (cy < 1023) {
                const n = cur + 1024;
                if (mask[n] === 0 && visited[n] === 0) { visited[n] = 1; Q[qTail++] = n; }
            }
        }

        let totalInterior = 0;
        const allRegions = [];
        const sigRegions = [];

        for (let y = 0; y < 1024; y++) {
            const rY = y * 1024;
            for (let x = 0; x < 1024; x++) {
                const idx = rY + x;
                if (mask[idx] === 0 && visited[idx] === 0) {
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
                            if (mask[n] === 0 && visited[n] === 0) { visited[n] = 2; Q[rTail++] = n; }
                        }
                        if (rx < 1023) {
                            const n = c + 1;
                            if (mask[n] === 0 && visited[n] === 0) { visited[n] = 2; Q[rTail++] = n; }
                        }
                        if (ry > 0) {
                            const n = c - 1024;
                            if (mask[n] === 0 && visited[n] === 0) { visited[n] = 2; Q[rTail++] = n; }
                        }
                        if (ry < 1023) {
                            const n = c + 1024;
                            if (mask[n] === 0 && visited[n] === 0) { visited[n] = 2; Q[rTail++] = n; }
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

const list = [
    'letters_87055_c_is_for_car.svg',
    'letters_87067_e_is_for_egg.svg',
    'letters_87073_f_is_for_flower.svg',
    'letters_88633_j_is_for_jar.svg',
    'letters_88651_m_is_for_mug.svg',
    'letters_88813_r_is_for_rainbow.svg',
    'letters_88999_t_is_for_teapot.svg',
    'letters_89005_u_is_for_umbrella.svg',
    'letters_89029_y_is_for_yarn.svg'
];

for (const f of list) {
    const p = path.join('D:/DoodleStudio/scratch/downloaded_templates', f);
    if (fs.existsSync(p)) {
        const r = evaluateSvg(p);
        console.log(f + ' -> ' + (r ? (r.isPass ? 'PASS' : 'FAIL') : 'null') + ' | ' + (r ? r.fillPct + '% (' + Math.round(r.totalInterior/1000) + 'k)' : '') + ' | ' + (r ? 'Sig: ' + r.sigCount : ''));
    }
}

