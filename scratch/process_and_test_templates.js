const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const candidates = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/curated_verified.json', 'utf8'));
const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';
const outputPngDir = 'D:/DoodleStudio/scratch/rendered_1024';
const contactSheetDir = 'D:/DoodleStudio/scratch/contact_sheets';

if (!fs.existsSync(outputPngDir)) fs.mkdirSync(outputPngDir, { recursive: true });
if (!fs.existsSync(contactSheetDir)) fs.mkdirSync(contactSheetDir, { recursive: true });

// Flood fill tester and line analyzer
function analyzeAndTestFill(png) {
    const width = png.width;
    const height = png.height;
    const data = png.data;

    // Create a 2D binary grid: 1 = stroke/line, 0 = empty space
    const grid = new Uint8Array(width * height);
    let strokeCount = 0;

    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            const idx = (y * width + x) * 4;
            const r = data[idx];
            const g = data[idx + 1];
            const b = data[idx + 2];
            const a = data[idx + 3];

            // If opaque and has color/stroke
            // On transparent or white background:
            // A stroke is either dark on light, or non-transparent on transparent
            let isStroke = false;
            if (a > 100) {
                // If it's dark on light:
                const lum = 0.299 * r + 0.587 * g + 0.114 * b;
                if (lum < 200) {
                    isStroke = true;
                }
            }
            if (isStroke) {
                grid[y * width + x] = 1;
                strokeCount++;
            }
        }
    }

    // Check stroke thickness by testing line spans
    let sampleThickness = [];
    for (let y = 100; y < height - 100; y += 40) {
        let span = 0;
        for (let x = 0; x < width; x++) {
            if (grid[y * width + x] === 1) {
                span++;
            } else {
                if (span > 0) {
                    if (span <= 30) sampleThickness.push(span);
                    span = 0;
                }
            }
        }
    }
    const avgThickness = sampleThickness.length > 0 
        ? (sampleThickness.reduce((a, b) => a + b, 0) / sampleThickness.length).toFixed(1)
        : '0';

    // Now test Flood Fill from borders (exterior background)
    // BFS queue for exterior
    const visited = new Uint8Array(width * height);
    const queue = [];

    // Seed borders
    for (let x = 0; x < width; x++) {
        if (grid[x] === 0) { visited[x] = 1; queue.push(x); }
        const bottom = (height - 1) * width + x;
        if (grid[bottom] === 0 && visited[bottom] === 0) { visited[bottom] = 1; queue.push(bottom); }
    }
    for (let y = 0; y < height; y++) {
        const left = y * width;
        if (grid[left] === 0 && visited[left] === 0) { visited[left] = 1; queue.push(left); }
        const right = y * width + (width - 1);
        if (grid[right] === 0 && visited[right] === 0) { visited[right] = 1; queue.push(right); }
    }

    let head = 0;
    while (head < queue.length) {
        const cur = queue[head++];
        const cx = cur % width;
        const cy = Math.floor(cur / width);

        // 4-way neighbors
        const neighbors = [
            cx > 0 ? cur - 1 : -1,
            cx < width - 1 ? cur + 1 : -1,
            cy > 0 ? cur - width : -1,
            cy < height - 1 ? cur + width : -1
        ];

        for (const n of neighbors) {
            if (n >= 0 && grid[n] === 0 && visited[n] === 0) {
                visited[n] = 1;
                queue.push(n);
            }
        }
    }

    // Now find interior enclosed regions (where grid == 0 and visited == 0)
    let regionCount = 0;
    let totalEnclosedPixels = 0;
    const regionSizes = [];

    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            const idx = y * width + x;
            if (grid[idx] === 0 && visited[idx] === 0) {
                // Found a new enclosed region
                regionCount++;
                let regionSize = 0;
                const rQueue = [idx];
                visited[idx] = 2; // mark interior visited

                let rHead = 0;
                while (rHead < rQueue.length) {
                    const rCur = rQueue[rHead++];
                    regionSize++;
                    const rx = rCur % width;
                    const ry = Math.floor(rCur / width);

                    const rNeighbors = [
                        rx > 0 ? rCur - 1 : -1,
                        rx < width - 1 ? rCur + 1 : -1,
                        ry > 0 ? rCur - width : -1,
                        ry < height - 1 ? rCur + width : -1
                    ];

                    for (const rn of rNeighbors) {
                        if (rn >= 0 && grid[rn] === 0 && visited[rn] === 0) {
                            visited[rn] = 2;
                            rQueue.push(rn);
                        }
                    }
                }

                totalEnclosedPixels += regionSize;
                // Only consider meaningful regions (greater than 150px)
                if (regionSize > 150) {
                    regionSizes.push(regionSize);
                }
            }
        }
    }

    return {
        strokeCount,
        avgThickness: parseFloat(avgThickness),
        exteriorPixels: queue.length,
        enclosedRegionsTotal: regionCount,
        significantRegions: regionSizes.length,
        totalEnclosedPixels,
        isClosedAndFillable: regionSizes.length >= 3 && totalEnclosedPixels > 20000
    };
}

// Convert SVG to White-on-Black 1024x1024 Neon Template PNG
function convertToNeonOutline(svgStr, outPngPath) {
    const resvg = new Resvg(svgStr, {
        fitTo: { mode: 'width', value: 1024 }
    });
    const rendered = resvg.render();
    const rawPng = PNG.sync.read(rendered.asPng());

    // Create a new 1024x1024 PNG with Pure Black Background and Crisp White / Bright Line Art
    const neonPng = new PNG({ width: 1024, height: 1024 });

    // Initialize black background
    for (let i = 0; i < neonPng.data.length; i += 4) {
        neonPng.data[i] = 0;       // R
        neonPng.data[i + 1] = 0;   // G
        neonPng.data[i + 2] = 0;   // B
        neonPng.data[i + 3] = 255; // A (opaque black)
    }

    // Determine scale and offset to center within 1024x1024 with 32px padding
    const scale = Math.min(960 / rawPng.width, 960 / rawPng.height);
    const targetW = Math.round(rawPng.width * scale);
    const targetH = Math.round(rawPng.height * scale);
    const offsetX = Math.round((1024 - targetW) / 2);
    const offsetY = Math.round((1024 - targetH) / 2);

    for (let y = 0; y < targetH; y++) {
        for (let x = 0; x < targetW; x++) {
            const srcX = Math.floor(x / scale);
            const srcY = Math.floor(y / scale);
            const srcIdx = (srcY * rawPng.width + srcX) * 4;

            const r = rawPng.data[srcIdx];
            const g = rawPng.data[srcIdx + 1];
            const b = rawPng.data[srcIdx + 2];
            const a = rawPng.data[srcIdx + 3];

            // Detect stroke: dark on light, or opaque on transparent
            let strokeIntensity = 0;
            if (a > 50) {
                const lum = 0.299 * r + 0.587 * g + 0.114 * b;
                if (lum < 220) {
                    strokeIntensity = Math.round((1 - lum / 255) * (a / 255) * 255);
                }
            }

            if (strokeIntensity > 40) {
                const dstX = offsetX + x;
                const dstY = offsetY + y;
                if (dstX >= 0 && dstX < 1024 && dstY >= 0 && dstY < 1024) {
                    const dstIdx = (dstY * 1024 + dstX) * 4;
                    // White line
                    neonPng.data[dstIdx] = 255;
                    neonPng.data[dstIdx + 1] = 255;
                    neonPng.data[dstIdx + 2] = 255;
                    neonPng.data[dstIdx + 3] = 255;
                }
            }
        }
    }

    // Morphological dilation (1-2px) to ensure stroke is solid and >= 4px
    const dilated = new PNG({ width: 1024, height: 1024 });
    dilated.data.set(neonPng.data);

    for (let y = 1; y < 1023; y++) {
        for (let x = 1; x < 1023; x++) {
            const idx = (y * 1024 + x) * 4;
            if (neonPng.data[idx] === 255) {
                // Set 3x3 neighbor to white
                for (let dy = -1; dy <= 1; dy++) {
                    for (let dx = -1; dx <= 1; dx++) {
                        const nIdx = ((y + dy) * 1024 + (x + dx)) * 4;
                        dilated.data[nIdx] = 255;
                        dilated.data[nIdx + 1] = 255;
                        dilated.data[nIdx + 2] = 255;
                        dilated.data[nIdx + 3] = 255;
                    }
                }
            }
        }
    }

    const buf = PNG.sync.write(dilated);
    fs.writeFileSync(outPngPath, buf);
    return { dilated, buf, rawPng };
}

async function run() {
    console.log(`Processing ${candidates.length} templates...`);
    const results = [];

    for (let i = 0; i < candidates.length; i++) {
        const c = candidates[i];
        const svgPath = path.join(downloadDir, c.localFile);
        const svgStr = fs.readFileSync(svgPath, 'utf8');
        const outPngName = `template_${c.catId}_${c.id}.png`;
        const outPngPath = path.join(outputPngDir, outPngName);

        const { dilated, buf, rawPng } = convertToNeonOutline(svgStr, outPngPath);
        const test = analyzeAndTestFill(rawPng);

        let fillStatus = 'Đạt';
        let detailNote = `${test.significantRegions} vùng kín lớn, nét ~${test.avgThickness}px`;
        if (test.significantRegions < 2) {
            fillStatus = 'Cần gia cố nét';
            detailNote = `Ít vùng kín (${test.significantRegions} vùng)`;
        } else if (test.totalEnclosedPixels < 15000) {
            fillStatus = 'Vùng tô nhỏ';
            detailNote = `Diện tích vùng kín thấp (${Math.round(test.totalEnclosedPixels/1000)}k px)`;
        }

        console.log(`[${i+1}/${candidates.length}] ${c.id}: ${c.title} -> ${fillStatus} (${detailNote})`);
        results.push({
            ...c,
            outPngName,
            outPngPath,
            fillStatus,
            detailNote,
            significantRegions: test.significantRegions,
            avgThickness: test.avgThickness,
            totalEnclosedPixels: test.totalEnclosedPixels
        });
    }

    fs.writeFileSync('D:/DoodleStudio/scratch/fill_test_results.json', JSON.stringify(results, null, 2));
    console.log('\nAll templates processed and tested!');
}

run().catch(console.error);

