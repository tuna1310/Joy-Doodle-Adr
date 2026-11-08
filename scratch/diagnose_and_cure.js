const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';

// Accurate flood fill & region analyzer meeting user's exact criteria:
// - totalFillableArea >= 150,000 px
// - significantRegions (each >= 1000 px) between 3 and 80
function analyzeBinaryMask(mask, width, height) {
    // mask: 1 = stroke (line), 0 = empty space
    const visited = new Uint8Array(width * height);
    const queue = [];

    // 1. Flood fill exterior from all 4 borders
    for (let x = 0; x < width; x++) {
        if (mask[x] === 0) { visited[x] = 1; queue.push(x); }
        const b = (height - 1) * width + x;
        if (mask[b] === 0 && visited[b] === 0) { visited[b] = 1; queue.push(b); }
    }
    for (let y = 0; y < height; y++) {
        const l = y * width;
        if (mask[l] === 0 && visited[l] === 0) { visited[l] = 1; queue.push(l); }
        const r = y * width + (width - 1);
        if (mask[r] === 0 && visited[r] === 0) { visited[r] = 1; queue.push(r); }
    }

    let head = 0;
    while (head < queue.length) {
        const cur = queue[head++];
        const cx = cur % width;
        const cy = Math.floor(cur / width);
        const nb = [
            cx > 0 ? cur - 1 : -1,
            cx < width - 1 ? cur + 1 : -1,
            cy > 0 ? cur - width : -1,
            cy < height - 1 ? cur + width : -1
        ];
        for (const n of nb) {
            if (n >= 0 && mask[n] === 0 && visited[n] === 0) {
                visited[n] = 1;
                queue.push(n);
            }
        }
    }

    // 2. Interior regions
    let totalFillablePixels = 0;
    const allRegions = [];
    const significantRegions = []; // >= 1000 px
    const minorRegions = []; // < 1000 px

    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            const idx = y * width + x;
            if (mask[idx] === 0 && visited[idx] === 0) {
                // Found interior region
                visited[idx] = 2;
                const rQueue = [idx];
                let rHead = 0;
                let area = 0;

                while (rHead < rQueue.length) {
                    const rCur = rQueue[rHead++];
                    area++;
                    const rx = rCur % width;
                    const ry = Math.floor(rCur / width);
                    const rNb = [
                        rx > 0 ? rCur - 1 : -1,
                        rx < width - 1 ? rCur + 1 : -1,
                        ry > 0 ? rCur - width : -1,
                        ry < height - 1 ? rCur + width : -1
                    ];
                    for (const rn of rNb) {
                        if (rn >= 0 && mask[rn] === 0 && visited[rn] === 0) {
                            visited[rn] = 2;
                            rQueue.push(rn);
                        }
                    }
                }

                allRegions.push(area);
                totalFillablePixels += area;
                if (area >= 1000) {
                    significantRegions.push(area);
                } else {
                    minorRegions.push(area);
                }
            }
        }
    }

    const pct = (totalFillablePixels / (width * height) * 100).toFixed(1);
    const isPassArea = totalFillablePixels >= 150000;
    const isPassRegions = significantRegions.length >= 3 && significantRegions.length <= 80;
    const passed = isPassArea && isPassRegions;

    return {
        totalFillablePixels,
        fillablePct: pct + '%',
        totalRegionsCount: allRegions.length,
        significantCount: significantRegions.length,
        minorCount: minorRegions.length,
        passed,
        reasons: [
            !isPassArea ? `Diện tích quá nhỏ (${Math.round(totalFillablePixels/1000)}k < 150k px)` : null,
            significantRegions.length < 3 ? `Quá ít vùng lớn (${significantRegions.length} < 3)` : null,
            significantRegions.length > 80 ? `Quá nhiều vùng (${significantRegions.length} > 80)` : null
        ].filter(Boolean)
    };
}

// Test currently rendered templates in scratch/rendered_1024
const renderedFiles = fs.readdirSync('D:/DoodleStudio/scratch/rendered_1024').filter(f => f.endsWith('.png'));
console.log(`Analyzing ${renderedFiles.length} rendered templates against strict criteria...`);

const report = [];
for (const f of renderedFiles) {
    const pngPath = path.join('D:/DoodleStudio/scratch/rendered_1024', f);
    const png = PNG.sync.read(fs.readFileSync(pngPath));
    const mask = new Uint8Array(png.width * png.height);
    for (let i = 0; i < mask.length; i++) {
        if (png.data[i * 4] > 128) mask[i] = 1;
    }

    const res = analyzeBinaryMask(mask, png.width, png.height);
    report.push({
        file: f,
        ...res
    });
}

const passed = report.filter(r => r.passed);
const failed = report.filter(r => !r.passed);

console.log(`\n=== STRICT RESULTS ===`);
console.log(`PASSED: ${passed.length}`);
console.log(`FAILED: ${failed.length}`);

console.log('\nFAILED DETAILS:');
failed.forEach(f => {
    console.log(`- ${f.file}: ${f.reasons.join(', ')} [Area: ${f.fillablePct}, Sig: ${f.significantCount}, Total: ${f.totalRegionsCount}]`);
});

fs.writeFileSync('D:/DoodleStudio/scratch/strict_analysis_initial.json', JSON.stringify({ passed, failed }, null, 2));

