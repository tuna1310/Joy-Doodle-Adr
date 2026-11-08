const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';

function evaluateWithScale(svgPath) {
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
}

const svgs = [
    // Animals
    'animals_330237_cute_bear_sitting.svg',
    'animals_284788_horse.svg',
    'animals_312543_kawaii_kitty_line_ar.svg',
    'animals_328812_feliform_face_colori.svg',
    'animals_352670_simple_butterfly_out.svg',
    'animals_353970_lutz_toy_puppy_outli.svg',
    'animals_354654_lutz_rabbit_3_outlin.svg',

    // Vehicles
    'vehicles_176375_bus_remixed.svg',
    'vehicles_228075_airplane_with_propel.svg',
    'vehicles_298680_sail_ship_2_masts_ou.svg',
    'vehicles_313982_biesagonetta.svg',
    'vehicles_312018_car.svg',
    'vehicles_324037_big_helicopter___out.svg',
    'vehicles_272531_rocket_coloring_page.svg',

    // Sea
    'sea_74233_green_sea_turtle_lin.svg',
    'sea_325114_turtle_side_outline.svg',
    'sea_290490_crab_04.svg',
    'sea_174579_starfish.svg',
    'sea_333629_a_smiling_eel_versio.svg',

    // Flowers
    'flowers_352672_simple_flower_head_l.svg',
    'flowers_352740_imaginary_flower_hea.svg',
    'flowers_333586_lilly_flower_abstrac.svg',
    'flowers_298288_rose___4_.svg',

    // Dinosaurs
    'dinosaurs_1384_dragon_with_horn.svg',
    'dinosaurs_281169_happy_dinosaur_outli.svg',
    'dinosaurs_234370_rawr_dinosaur_outlin.svg',
    'dinosaurs_316177_white_cartoon_tricer.svg',

    // Food
    'food_84631_a_is_for_apple.svg',
    'food_8546_simple_fruit_apple.svg',
    'food_177813_banana_bunch_line_ar.svg',

    // Scenery
    'scenery_132469_family_house_raskorj.svg',
    'scenery_234766_rainbow_for_coloring.svg',
    'scenery_229963_color.svg',

    // Letters
    'letters_87049_b_is_for_butterfly.svg',
    'letters_190567_letter_c_for_cat_for.svg',
    'letters_87061_d_is_for_dog.svg'
];

console.log('Testing scaled candidates...');
for (const s of svgs) {
    const p = path.join(downloadDir, s);
    if (!fs.existsSync(p)) {
        console.log(`Missing: ${s}`);
        continue;
    }
    const res = evaluateWithScale(p);
    console.log(`${s} -> ${res.isPass ? 'PASS' : 'FAIL'} | Area: ${res.fillPct}% (${Math.round(res.totalInterior/1000)}k px) | Sig: ${res.sigCount} (Total: ${res.allRegionsCount})`);
}

