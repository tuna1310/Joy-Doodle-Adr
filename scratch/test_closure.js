const fs = require('fs');
const path = require('path');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

// Test if closing gaps on a neon outline creates closed regions
function testClosure(pngPath) {
    const png = PNG.sync.read(fs.readFileSync(pngPath));
    const w = png.width;
    const h = png.height;
    
    // Find min/max coordinates of white pixels
    let minX = w, maxX = 0, minY = h, maxY = 0;
    for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
            const idx = (y * w + x) * 4;
            if (png.data[idx] > 128) {
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }
    }
    return { minX, maxX, minY, maxY, spanW: maxX - minX, spanH: maxY - minY };
}

const checkFiles = [
    'template_animals_354457.png',
    'template_animals_354459.png',
    'template_sea_215988.png',
    'template_sea_245072.png',
    'template_flowers_236029.png',
    'template_flowers_354124.png',
    'template_dinosaurs_330252.png'
];

for (const f of checkFiles) {
    const p = path.join('D:/DoodleStudio/scratch/rendered_1024', f);
    if (fs.existsSync(p)) {
        console.log(f, testClosure(p));
    }
}

