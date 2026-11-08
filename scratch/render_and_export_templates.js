const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';
const catalog = JSON.parse(fs.readFileSync('scratch/perfect_40_catalog.json', 'utf8'));

const renderedDir = 'D:/DoodleStudio/scratch/rendered_1024';
if (!fs.existsSync(renderedDir)) fs.mkdirSync(renderedDir, { recursive: true });

const targetAssetsDir = 'D:/DoodleStudio/app/src/main/assets/templates/images';
if (!fs.existsSync(targetAssetsDir)) fs.mkdirSync(targetAssetsDir, { recursive: true });

console.log(`Rendering 40 templates into 1024x1024 outline PNGs...`);

function renderTemplateToPng(svgPath, outPngPath) {
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

    // Output transparent PNG with pure white outlines (RGB: 255,255,255; Alpha: 255 where mask=1)
    const outPng = new PNG({ width: 1024, height: 1024 });
    for (let i = 0; i < 1024 * 1024; i++) {
        const idx = i * 4;
        if (mask[i] === 1) {
            outPng.data[idx] = 255;
            outPng.data[idx + 1] = 255;
            outPng.data[idx + 2] = 255;
            outPng.data[idx + 3] = 255;
        } else {
            outPng.data[idx] = 0;
            outPng.data[idx + 1] = 0;
            outPng.data[idx + 2] = 0;
            outPng.data[idx + 3] = 0;
        }
    }

    fs.writeFileSync(outPngPath, PNG.sync.write(outPng));
    return outPng;
}

const renderedPngs = [];

for (let i = 0; i < catalog.length; i++) {
    const item = catalog[i];
    const srcSvg = path.join(downloadDir, item.file);
    const pngName = `template_${String(i + 1).padStart(2, '0')}_${item.id}.png`;
    const scratchPng = path.join(renderedDir, pngName);
    const assetPng = path.join(targetAssetsDir, pngName);

    console.log(`[${i + 1}/40] Rendering ${item.titleVi} -> ${pngName}`);
    const pngObj = renderTemplateToPng(srcSvg, scratchPng);
    fs.copyFileSync(scratchPng, assetPng);

    renderedPngs.push({ pngObj, item, pngName });
}

console.log('Generating 2 Contact Sheets...');

function buildContactSheet(items, sheetPath) {
    // 5 columns x 4 rows = 20 images
    // Each cell: 256x256 image + 40px text label = 256 x 296
    const cols = 5;
    const rows = 4;
    const cellW = 256;
    const cellH = 270;
    const sheetW = cols * cellW;
    const sheetH = rows * cellH;

    const sheet = new PNG({ width: sheetW, height: sheetH });
    // Fill with dark navy background (#0f172a)
    for (let i = 0; i < sheetW * sheetH; i++) {
        const idx = i * 4;
        sheet.data[idx] = 15;
        sheet.data[idx + 1] = 23;
        sheet.data[idx + 2] = 42;
        sheet.data[idx + 3] = 255;
    }

    for (let idx = 0; idx < items.length; idx++) {
        const c = idx % cols;
        const r = Math.floor(idx / cols);
        const { pngObj, item } = items[idx];

        const startX = c * cellW + 8;
        const startY = r * cellH + 8;
        const innerSize = 240;

        // Downsample 1024x1024 to 240x240
        const scale = 1024 / innerSize;
        for (let y = 0; y < innerSize; y++) {
            const sY = Math.floor(y * scale);
            const dRow = (startY + y) * sheetW;
            const sRow = sY * 1024;
            for (let x = 0; x < innerSize; x++) {
                const sX = Math.floor(x * scale);
                const sIdx = (sRow + sX) * 4;
                const dIdx = (dRow + startX + x) * 4;

                const a = pngObj.data[sIdx + 3];
                if (a > 50) {
                    // White outline with neon cyan tint
                    sheet.data[dIdx] = 240;
                    sheet.data[dIdx + 1] = 248;
                    sheet.data[dIdx + 2] = 255;
                } else {
                    // Card background (#1e293b)
                    sheet.data[dIdx] = 30;
                    sheet.data[dIdx + 1] = 41;
                    sheet.data[dIdx + 2] = 59;
                }
            }
        }
    }

    fs.writeFileSync(sheetPath, PNG.sync.write(sheet));
}

const artifactDir = 'C:/Users/Admin/.gemini/antigravity/brain/fbf229bc-466c-4bfe-b871-a25dff2d39b4';
const sheet1Path = path.join(artifactDir, 'contact_sheet_1.png');
const sheet2Path = path.join(artifactDir, 'contact_sheet_2.png');

buildContactSheet(renderedPngs.slice(0, 20), sheet1Path);
buildContactSheet(renderedPngs.slice(20, 40), sheet2Path);

console.log('Contact sheets generated and copied to artifact directory!');

// Generate templates index.json for Android
const androidIndex = catalog.map((item, idx) => ({
    id: `template_${String(idx + 1).padStart(2, '0')}_${item.id}`,
    stt: idx + 1,
    openclipartId: item.id,
    titleEn: item.title,
    titleVi: item.titleVi,
    category: item.cat,
    categoryId: item.catId,
    artist: item.artist,
    license: "CC0 1.0 Universal",
    imagePath: `templates/images/${renderedPngs[idx].pngName}`,
    fillableAreaPct: item.fillPct,
    significantRegions: item.sigCount,
    description: item.visualDesc
}));

const indexJsonPath = 'D:/DoodleStudio/app/src/main/assets/templates/index.json';
fs.writeFileSync(indexJsonPath, JSON.stringify(androidIndex, null, 2));
console.log(`Saved ${indexJsonPath}!`);

// Generate CREDITS.md
let creditsMd = `# Attribution & Credits

All coloring template artwork used in DoodleStudio is 100% sourced from [Openclipart.org](https://openclipart.org) under the **Creative Commons CC0 1.0 Universal Public Domain Dedication**.

| STT | Thể loại | Tên tranh | Tác giả | Giấy phép | Kích thước | Vùng tô kín | Openclipart URL |
|---|---|---|---|---|---|---|---|
`;

for (let i = 0; i < catalog.length; i++) {
    const item = catalog[i];
    creditsMd += `| ${i + 1} | ${item.cat} | ${item.titleVi} (${item.title}) | ${item.artist} | CC0 Public Domain | ${item.fileSizeKb} KB | ${item.sigCount} vùng (${item.fillPct}%) | [Openclipart #${item.id}](${item.url}) |\n`;
}

creditsMd += `\n*Generated with 100% CC0 public domain vector artwork.*\n`;
fs.writeFileSync('D:/DoodleStudio/CREDITS.md', creditsMd);
console.log('Saved CREDITS.md!');

