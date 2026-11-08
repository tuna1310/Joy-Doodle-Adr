const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const dir = 'D:/DoodleStudio/scratch/downloaded_templates';
const files = fs.readdirSync(dir).filter(f => f.endsWith('.svg'));

console.log(`Found ${files.length} SVG files on disk.`);

// Flood fill test function:
// Rasterize SVG onto a 1024x1024 canvas with black background and white lines (or transparent)
// In a coloring template:
// Lines are solid (e.g. black on white, or white on black).
// A closed region has boundary pixels that block the flood fill from reaching the canvas borders.
function testFloodFill(pngBuffer) {
    const png = PNG.sync.read(pngBuffer);
    const width = png.width;
    const height = png.height;
    const data = png.data;

    // Check center pixel or a few sample points
    // Let's do a simple connected-component count of background (empty) regions
    // A point (x,y) is border if it has high luminance/alpha.
    // Let's sample a grid 64x64 to count enclosed regions.
    let borderPixels = 0;
    for (let i = 0; i < data.length; i += 4) {
        const a = data[i + 3];
        // If opaque and dark or light
        if (a > 128) borderPixels++;
    }

    const borderRatio = borderPixels / (width * height);
    return {
        width,
        height,
        borderRatio,
        isReasonableDensity: borderRatio > 0.01 && borderRatio < 0.45
    };
}

const analysis = [];

for (const f of files) {
    const p = path.join(dir, f);
    const svgStr = fs.readFileSync(p, 'utf8');
    const size = fs.statSync(p).size;

    try {
        const resvg = new Resvg(svgStr, {
            fitTo: { mode: 'width', value: 1024 }
        });
        const pngData = resvg.render();
        const pngBuf = pngData.asPng();
        const fillResult = testFloodFill(pngBuf);

        analysis.push({
            file: f,
            size,
            validRender: true,
            borderRatio: (fillResult.borderRatio * 100).toFixed(1) + '%',
            isGoodDensity: fillResult.isReasonableDensity
        });
    } catch (e) {
        analysis.push({
            file: f,
            size,
            validRender: false,
            error: e.message
        });
    }
}

fs.writeFileSync('D:/DoodleStudio/scratch/render_analysis.json', JSON.stringify(analysis, null, 2));
console.log('Analysis completed for', analysis.length, 'files.');
const good = analysis.filter(x => x.validRender && x.isGoodDensity);
console.log('Good candidates:', good.length);

