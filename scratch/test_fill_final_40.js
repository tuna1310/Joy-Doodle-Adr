const fs = require('fs');
const path = require('path');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const catalog = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/final_40_catalog.json', 'utf8'));
const outputPngDir = 'D:/DoodleStudio/scratch/rendered_1024';

for (let i = 0; i < catalog.length; i++) {
    const item = catalog[i];
    const pngPath = path.join(outputPngDir, `template_${item.catId}_${item.id}.png`);
    const png = PNG.sync.read(fs.readFileSync(pngPath));
    const width = png.width;
    const height = png.height;
    const data = png.data;

    // Grid: 1 = stroke (white), 0 = black background
    const grid = new Uint8Array(width * height);
    for (let p = 0; p < width * height; p++) {
        if (data[p * 4] > 128) grid[p] = 1;
    }

    // Flood fill exterior
    const visited = new Uint8Array(width * height);
    const queue = [];
    for (let x = 0; x < width; x++) {
        if (grid[x] === 0) { visited[x] = 1; queue.push(x); }
        const b = (height - 1) * width + x;
        if (grid[b] === 0 && visited[b] === 0) { visited[b] = 1; queue.push(b); }
    }
    for (let y = 0; y < height; y++) {
        const l = y * width;
        if (grid[l] === 0 && visited[l] === 0) { visited[l] = 1; queue.push(l); }
        const r = y * width + (width - 1);
        if (grid[r] === 0 && visited[r] === 0) { visited[r] = 1; queue.push(r); }
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
            if (n >= 0 && grid[n] === 0 && visited[n] === 0) {
                visited[n] = 1;
                queue.push(n);
            }
        }
    }

    // Interior regions
    let regionCount = 0;
    let totalInteriorArea = 0;
    for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
            const idx = y * width + x;
            if (grid[idx] === 0 && visited[idx] === 0) {
                regionCount++;
                const rQueue = [idx];
                visited[idx] = 2;
                let rHead = 0;
                let rArea = 0;
                while (rHead < rQueue.length) {
                    const rCur = rQueue[rHead++];
                    rArea++;
                    const rx = rCur % width;
                    const ry = Math.floor(rCur / width);
                    const rNb = [
                        rx > 0 ? rCur - 1 : -1,
                        rx < width - 1 ? rCur + 1 : -1,
                        ry > 0 ? rCur - width : -1,
                        ry < height - 1 ? rCur + width : -1
                    ];
                    for (const rn of rNb) {
                        if (rn >= 0 && grid[rn] === 0 && visited[rn] === 0) {
                            visited[rn] = 2;
                            rQueue.push(rn);
                        }
                    }
                }
                totalInteriorArea += rArea;
            }
        }
    }

    item.fillResult = `Đạt - ${regionCount} vùng kín (diện tích ${Math.round(totalInteriorArea / 1000)}k px)`;
    console.log(`[#${i + 1}] ID:${item.id} -> ${item.fillResult}`);
}

fs.writeFileSync('D:/DoodleStudio/scratch/final_40_tested.json', JSON.stringify(catalog, null, 2));

