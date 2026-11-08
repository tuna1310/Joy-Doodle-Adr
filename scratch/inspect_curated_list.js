const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');

const candidateIds = [
    // 1. Động vật (Animals)
    { id: '354457', slug: 'dog', cat: 'Động vật', catId: 'animals', title: 'Dog' },
    { id: '353970', slug: 'lutz-toy-puppy-outline', cat: 'Động vật', catId: 'animals', title: 'Lutz - Toy Puppy' },
    { id: '330237', slug: 'cute-bear-sitting', cat: 'Động vật', catId: 'animals', title: 'Cute Bear Sitting' },
    { id: '354654', slug: 'lutz-rabbit-3-outline', cat: 'Động vật', catId: 'animals', title: 'Lutz - Rabbit' },
    { id: '354459', slug: 'bird', cat: 'Động vật', catId: 'animals', title: 'Bird' },
    { id: '352670', slug: 'simple-butterfly-outline', cat: 'Động vật', catId: 'animals', title: 'Simple Butterfly Outline' },
    { id: '284788', slug: 'horse', cat: 'Động vật', catId: 'animals', title: 'Horse' },
    { id: '312543', slug: 'kawaii-kitty-line-art', cat: 'Động vật', catId: 'animals', title: 'Kawaii Kitty Line Art' },
    { id: '328812', slug: 'feliform-face-coloring-book-version', cat: 'Động vật', catId: 'animals', title: 'Feliform Face' },

    // 2. Phương tiện (Vehicles)
    { id: '181232', slug: 'number-image-car', cat: 'Phương tiện', catId: 'vehicles', title: 'Car' },
    { id: '228075', slug: 'airplane-with-propeller-outline', cat: 'Phương tiện', catId: 'vehicles', title: 'Airplane with Propeller' },
    { id: '298680', slug: 'sail-ship-2-masts-outline', cat: 'Phương tiện', catId: 'vehicles', title: 'Sailboat' },
    { id: '231473', slug: 'boat', cat: 'Phương tiện', catId: 'vehicles', title: 'Simple Boat' },
    { id: '323440', slug: 'wagon-train', cat: 'Phương tiện', catId: 'vehicles', title: 'Wagon Train' },
    { id: '176375', slug: 'bus-remixed', cat: 'Phương tiện', catId: 'vehicles', title: 'City Bus' },
    { id: '313982', slug: 'biesagonetta', cat: 'Phương tiện', catId: 'vehicles', title: 'Bicycle' },

    // 3. Biển (Sea life)
    { id: '215988', slug: 'dolphin', cat: 'Biển', catId: 'sea', title: 'Dolphin' },
    { id: '74233', slug: 'green-sea-turtle-line-art', cat: 'Biển', catId: 'sea', title: 'Sea Turtle' },
    { id: '325114', slug: 'turtle-side-outline', cat: 'Biển', catId: 'sea', title: 'Turtle Side Outline' },
    { id: '290490', slug: 'crab-04', cat: 'Biển', catId: 'sea', title: 'Crab' },
    { id: '174579', slug: 'starfish', cat: 'Biển', catId: 'sea', title: 'Starfish' },
    { id: '245072', slug: 'baby-octopus-coloring-page', cat: 'Biển', catId: 'sea', title: 'Baby Octopus' },
    { id: '333629', slug: 'a-smiling-eel-version-without-fill-color-for-coloring', cat: 'Biển', catId: 'sea', title: 'Smiling Eel' },

    // 4. Hoa lá & Cây (Flowers & Nature)
    { id: '236029', slug: 'tulip-bloom', cat: 'Hoa lá', catId: 'flowers', title: 'Tulip Bloom' },
    { id: '352672', slug: 'simple-flower-head-lineart', cat: 'Hoa lá', catId: 'flowers', title: 'Sunflower / Daisy' },
    { id: '352740', slug: 'imaginary-flower-head', cat: 'Hoa lá', catId: 'flowers', title: 'Mandala Flower' },
    { id: '333586', slug: 'lilly-flower-abstract-art-coloring-page', cat: 'Hoa lá', catId: 'flowers', title: 'Lily Flower' },
    { id: '354124', slug: 'lutz-oak-leaf-outline', cat: 'Hoa lá', catId: 'flowers', title: 'Oak Leaf' },
    { id: '354067', slug: 'lutz-maple-leaf-outline', cat: 'Hoa lá', catId: 'flowers', title: 'Maple Leaf' },

    // 5. Khủng long (Dinosaurs)
    { id: '1384', slug: 'dragon-with-horn', cat: 'Khủng long', catId: 'dinosaurs', title: 'Dragon with Horn' },
    { id: '330252', slug: 'cute-dino-silhouette-t-rex', cat: 'Khủng long', catId: 'dinosaurs', title: 'Cute Dino T-Rex' },
    { id: '330251', slug: 'cute-dino-silhouette-velociraptor', cat: 'Khủng long', catId: 'dinosaurs', title: 'Cute Dino Raptor' },
    { id: '281169', slug: 'happy-dinosaur-outline', cat: 'Khủng long', catId: 'dinosaurs', title: 'Happy Dinosaur' },
    { id: '234370', slug: 'rawr-dinosaur-outline', cat: 'Khủng long', catId: 'dinosaurs', title: 'Rawr Triceratops' },

    // 6. Trái cây & Đồ ăn (Food & Fruits)
    { id: '84631', slug: 'a-is-for-apple', cat: 'Trái cây & Đồ ăn', catId: 'food', title: 'Apple' },
    { id: '8546', slug: 'simple-fruit-apple', cat: 'Trái cây & Đồ ăn', catId: 'food', title: 'Apple Slice' },
    { id: '177814', slug: 'single-line-art-banana', cat: 'Trái cây & Đồ ăn', catId: 'food', title: 'Single Banana' },
    { id: '177813', slug: 'banana-bunch-line-art', cat: 'Trái cây & Đồ ăn', catId: 'food', title: 'Banana Bunch' },

    // 7. Phong cảnh đơn giản (Scenery)
    { id: '132469', slug: 'family-house-raskorjaka-for-tshirt-printing', cat: 'Phong cảnh đơn giản', catId: 'scenery', title: 'Family House' },
    { id: '234766', slug: 'rainbow-for-coloring', cat: 'Phong cảnh đơn giản', catId: 'scenery', title: 'Rainbow' },
    { id: '235489', slug: 'mountain-outlines-simpler', cat: 'Phong cảnh đơn giản', catId: 'scenery', title: 'Mountain Outlines' },

    // 8. Chữ cái & Chữ số (Letters & Numbers)
    { id: '87049', slug: 'b-is-for-butterfly', cat: 'Chữ cái & chữ số', catId: 'letters', title: 'B is for Butterfly' },
    { id: '190567', slug: 'letter-c-for-cat-for-coloring', cat: 'Chữ cái & chữ số', catId: 'letters', title: 'Letter C for Cat' },
    { id: '87061', slug: 'd-is-for-dog', cat: 'Chữ cái & chữ số', catId: 'letters', title: 'D is for Dog' },
    { id: '190572', slug: 'p-for-pencil-for-coloring', cat: 'Chữ cái & chữ số', catId: 'letters', title: 'P for Pencil' },
    { id: '190575', slug: 'v-for-violin-for-coloring', cat: 'Chữ cái & chữ số', catId: 'letters', title: 'V for Violin' }
];

console.log(`Checking ${candidateIds.length} candidate templates...`);
const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';
const verifiedList = [];

for (const c of candidateIds) {
    // Find matching file in downloadDir
    const files = fs.readdirSync(downloadDir);
    const matched = files.find(f => f.includes(`_${c.id}_`));
    if (!matched) {
        console.log(`[MISSING ON DISK]: ID ${c.id} (${c.title})`);
        continue;
    }

    const p = path.join(downloadDir, matched);
    const size = fs.statSync(p).size;
    try {
        const svgStr = fs.readFileSync(p, 'utf8');
        const resvg = new Resvg(svgStr, { fitTo: { mode: 'width', value: 1024 } });
        const pngBuf = resvg.render().asPng();
        console.log(`[OK] ID ${c.id} | ${c.cat} | ${matched} (${(size/1024).toFixed(1)}KB) -> Rendered PNG ${pngBuf.length} bytes`);
        verifiedList.push({
            ...c,
            localFile: matched,
            sizeBytes: size,
            sizeKb: (size / 1024).toFixed(1) + ' KB'
        });
    } catch (e) {
        console.log(`[RENDER ERROR] ID ${c.id}: ${e.message}`);
    }
}

console.log(`\nSuccessfully verified on disk & rendered: ${verifiedList.length} of ${candidateIds.length}`);
fs.writeFileSync('D:/DoodleStudio/scratch/curated_verified.json', JSON.stringify(verifiedList, null, 2));

