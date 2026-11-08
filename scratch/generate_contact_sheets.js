const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';
const outputPngDir = 'D:/DoodleStudio/scratch/rendered_1024';
const contactSheetDir = 'D:/DoodleStudio/scratch/contact_sheets';
const artifactDir = 'C:/Users/Admin/.gemini/antigravity/brain/fbf229bc-466c-4bfe-b871-a25dff2d39b4';

if (!fs.existsSync(outputPngDir)) fs.mkdirSync(outputPngDir, { recursive: true });
if (!fs.existsSync(contactSheetDir)) fs.mkdirSync(contactSheetDir, { recursive: true });

// The 40 selected templates (100% verified, line art, closed regions)
const templates40 = [
    // 1. Động vật (Animals) - 6
    { id: '353970', file: 'animals_353970_lutz_toy_puppy_outli.svg', title: 'Toy Puppy', cat: 'Động vật', catId: 'animals', artist: 'teck5', url: 'https://openclipart.org/detail/353970/lutz-toy-puppy-outline',
      desc: 'Chú cún con đồ chơi ngồi ngoan ngoãn với đôi tai rủ, vòng cổ có chuông nhỏ, đôi mắt to tròn và nét viền rất dày dễ tô.' },
    { id: '330237', file: 'animals_330237_cute_bear_sitting.svg', title: 'Cute Bear Sitting', cat: 'Động vật', catId: 'animals', artist: 'hund', url: 'https://openclipart.org/detail/330237/cute-bear-sitting',
      desc: 'Chú gấu bông ngồi mỉm cười, hai tai tròn xoe, bụng tròn to và bàn chân có đệm móng bo tròn ngộ nghĩnh.' },
    { id: '354654', file: 'animals_354654_lutz_rabbit_3_outlin.svg', title: 'Rabbit with Carrot', cat: 'Động vật', catId: 'animals', artist: 'teck5', url: 'https://openclipart.org/detail/354654/lutz-rabbit-3-outline',
      desc: 'Chú thỏ trắng đứng hai chân ôm củ cà rốt to, đôi tai dài vểnh cao, má phúng phính và đuôi tròn bông xù.' },
    { id: '352670', file: 'animals_352670_simple_butterfly_out.svg', title: 'Simple Butterfly', cat: 'Động vật', catId: 'animals', artist: 'teck5', url: 'https://openclipart.org/detail/352670/simple-butterfly-outline',
      desc: 'Bướm xòe cánh đối xứng hai bên, thân thon có hai râu cong, các khoang cánh to rõ nét rất thích hợp tô nhiều màu neon.' },
    { id: '284788', file: 'animals_284788_horse.svg', title: 'Horse Standing', cat: 'Động vật', catId: 'animals', artist: 'Superolli', url: 'https://openclipart.org/detail/284788/horse',
      desc: 'Chú ngựa đứng hiên ngang nhìn nghiêng, bờm ngựa uốn lượn, đuôi dài buông lơi và 4 chân khỏe khoắn móng guốc rõ ràng.' },
    { id: '312543', file: 'animals_312543_kawaii_kitty_line_ar.svg', title: 'Kawaii Kitty', cat: 'Động vật', catId: 'animals', artist: 'GDJ', url: 'https://openclipart.org/detail/312543/kawaii-kitty-line-art',
      desc: 'Chú mèo con phong cách Kawaii chibi đầu to mình nhỏ, đôi mắt to lấp lánh, tai nhọn và râu mèo dễ thương.' },

    // 2. Phương tiện (Vehicles) - 6
    { id: '181232', file: 'vehicles_181232_number_image_car.svg', title: 'Sport Car', cat: 'Phương tiện', catId: 'vehicles', artist: 'frankes', url: 'https://openclipart.org/detail/181232/number-image-car',
      desc: 'Chiếc xe hơi hoạt hình mui trần với bánh xe tròn to, kính chắn gió uốn cong và thân xe bo tròn kiểu coupe năng động.' },
    { id: '228075', file: 'vehicles_228075_airplane_with_propel.svg', title: 'Airplane with Propeller', cat: 'Phương tiện', catId: 'vehicles', artist: 'Firkin', url: 'https://openclipart.org/detail/228075/airplane-with-propeller-outline',
      desc: 'Máy bay cánh quạt một tầng cánh cổ điển, mũi có cánh quạt 2 cánh, cánh máy bay dài và các cửa sổ cabin tròn.' },
    { id: '298680', file: 'vehicles_298680_sail_ship_2_masts_ou.svg', title: 'Sailboat 2 Masts', cat: 'Phương tiện', catId: 'vehicles', artist: 'GDJ', url: 'https://openclipart.org/detail/298680/sail-ship-2-masts-outline',
      desc: 'Thuyền buồm hai cột buồm lướt sóng, các cánh buồm căng gió chia thành từng mảng tam giác lớn dễ đổ màu.' },
    { id: '323440', file: 'vehicles_323440_wagon_train.svg', title: 'Wagon Train', cat: 'Phương tiện', catId: 'vehicles', artist: 'GDJ', url: 'https://openclipart.org/detail/323440/wagon-train',
      desc: 'Đoàn tàu hỏa hơi nước với đầu kéo có ống khói nhả khói tròn, các bánh xe sắt liên kết thanh truyền và toa chở hàng.' },
    { id: '176375', file: 'vehicles_176375_bus_remixed.svg', title: 'City Bus', cat: 'Phương tiện', catId: 'vehicles', artist: 'tivus', url: 'https://openclipart.org/detail/176375/bus-remixed',
      desc: 'Xe buýt thành phố hình hộp chữ nhật bo góc, hàng cửa sổ lớn chia ô rõ ràng, hai bánh xe to và đèn pha tròn.' },
    { id: '313982', file: 'vehicles_313982_biesagonetta.svg', title: 'Bicycle Outline', cat: 'Phương tiện', catId: 'vehicles', artist: 'richardsdrawings', url: 'https://openclipart.org/detail/313982/biesagonetta',
      desc: 'Chiếc xe đạp cổ điển hai bánh tròn to có căm xe, khung sườn tam giác đơn giản, yên xe và ghi-đông uốn cong.' },

    // 3. Biển (Sea life) - 5
    { id: '74233', file: 'sea_74233_green_sea_turtle_lin.svg', title: 'Green Sea Turtle', cat: 'Biển', catId: 'sea', artist: 'netalloy', url: 'https://openclipart.org/detail/74233/green-sea-turtle-line-art',
      desc: 'Chú rùa biển nhìn từ trên xuống, mai rùa chia các ô lục giác đều đặn, hai chi trước dạng mái chèo xòe rộng bơi lội.' },
    { id: '325114', file: 'sea_325114_turtle_side_outline.svg', title: 'Turtle Side Swimming', cat: 'Biển', catId: 'sea', artist: 'teck5', url: 'https://openclipart.org/detail/325114/turtle-side-outline',
      desc: 'Rùa biển bơi góc nghiêng, đầu vươn về phía trước, mai vòm cong và chân bơi uốn lượn giữa dòng nước.' },
    { id: '290490', file: 'sea_290490_crab_04.svg', title: 'Ocean Crab', cat: 'Biển', catId: 'sea', artist: 'GDJ', url: 'https://openclipart.org/detail/290490/crab-04',
      desc: 'Chú cua biển với hai chiếc càng to khỏe giương cao hai bên, mai cua tròn có gai nhỏ và 6 chân bò hai bên.' },
    { id: '174579', file: 'sea_174579_starfish.svg', title: 'Starfish 5 Arms', cat: 'Biển', catId: 'sea', artist: 'jpenici', url: 'https://openclipart.org/detail/174579/starfish',
      desc: 'Sao biển 5 cánh đối xứng tỏa tròn, bề mặt có các chấm giác bám nhỏ li ti, nét viền dày dặn rất dễ tô.' },
    { id: '333629', file: 'sea_333629_a_smiling_eel_versio.svg', title: 'Smiling Sea Eel', cat: 'Biển', catId: 'sea', artist: 'zapyon', url: 'https://openclipart.org/detail/333629/a-smiling-eel-version-without-fill-color-for-coloring',
      desc: 'Chú lươn biển mỉm cười thân thiện, thân hình dài uốn lượn hình chữ S, dải vây lưng mềm mại chạy dài.' },

    // 4. Hoa lá & Cây (Flowers & Nature) - 5
    { id: '352672', file: 'flowers_352672_simple_flower_head_l.svg', title: 'Sunflower Daisy', cat: 'Hoa lá', catId: 'flowers', artist: 'teck5', url: 'https://openclipart.org/detail/352672/simple-flower-head-lineart',
      desc: 'Bông hoa cúc/hướng dương với nhụy hoa tròn xoe ở giữa và 12 cánh hoa xòe tròn đều đặn quanh tâm.' },
    { id: '352740', file: 'flowers_352740_imaginary_flower_hea.svg', title: 'Mandala Flower', cat: 'Hoa lá', catId: 'flowers', artist: 'teck5', url: 'https://openclipart.org/detail/352740/imaginary-flower-head',
      desc: 'Bông hoa nghệ thuật kiểu mạn đà la với nhiều lớp cánh hoa lồng ghép đối xứng tỏa tròn tinh tế.' },
    { id: '333586', file: 'flowers_333586_lilly_flower_abstrac.svg', title: 'Lily Flower', cat: 'Hoa lá', catId: 'flowers', artist: 'elizavella', url: 'https://openclipart.org/detail/333586/lilly-flower-abstract-art-coloring-page',
      desc: 'Hoa ly thanh nhã với các cánh hoa thon dài vươn ra, nhụy hoa dài nhô cao và cuống hoa cong mềm mại.' },
    { id: '262028', file: 'flowers_262028_trees.svg', title: 'Woodland Trees', cat: 'Hoa lá', catId: 'flowers', artist: 'studio_twenty_eight', url: 'https://openclipart.org/detail/262028/trees',
      desc: 'Ba cây xanh đứng cạnh nhau với tán lá hình mây bồng bềnh, thân cây thẳng có vân gỗ và rễ bám đất.' },
    { id: '230020', file: 'flowers_230020_alp_lily.svg', title: 'Alp Lily Blossom', cat: 'Hoa lá', catId: 'flowers', artist: 'Moini', url: 'https://openclipart.org/detail/230020/alp-lily',
      desc: 'Bông hoa loa kèn núi bung nở rực rỡ, cánh hoa uốn cong nhẹ ra ngoài, cuống lá thon thả.' },

    // 5. Khủng long (Dinosaurs) - 5
    { id: '1384', file: 'dinosaurs_1384_dragon_with_horn.svg', title: 'Dragon Dino with Horn', cat: 'Khủng long', catId: 'dinosaurs', artist: 'Platypuscove', url: 'https://openclipart.org/detail/1384/dragon-with-horn',
      desc: 'Chú rồng khủng long tí hon có đôi cánh nhỏ sau lưng, sừng nhọn trên đầu, đuôi gai cong vút ngộ nghĩnh.' },
    { id: '281169', file: 'dinosaurs_281169_happy_dinosaur_outli.svg', title: 'Happy Brachiosaurus', cat: 'Khủng long', catId: 'dinosaurs', artist: 'Firkin', url: 'https://openclipart.org/detail/281169/happy-dinosaur-outline',
      desc: 'Khủng long cổ dài Brachiosaurus mỉm cười toe toét, cổ cao vút, thân hình tròn mập và bốn chân to như cột nhà.' },
    { id: '234370', file: 'dinosaurs_234370_rawr_dinosaur_outlin.svg', title: 'Rawr Triceratops', cat: 'Khủng long', catId: 'dinosaurs', artist: 'Tavin', url: 'https://openclipart.org/detail/234370/rawr-dinosaur-outline',
      desc: 'Khủng long 3 sừng Triceratops với khiên cổ viền lượn sóng, hai sừng dài trên trán và một sừng mũi dễ thương.' },
    { id: '28420', file: 'dinosaurs_28420_cartoon_pterodactyl.svg', title: 'Cartoon Pterodactyl', cat: 'Khủng long', catId: 'dinosaurs', artist: 'StudioFibonacci', url: 'https://openclipart.org/detail/28420/cartoon-pterodactyl',
      desc: 'Khủng long bay Pterodactyl sải rộng đôi cánh màng da, mỏ dài nhọn, mào sau đầu đặc trưng đang bay lượn.' },
    { id: '23508', file: 'dinosaurs_23508_cartoon_brontosaurus.svg', title: 'Baby Brontosaurus', cat: 'Khủng long', catId: 'dinosaurs', artist: 'StudioFibonacci', url: 'https://openclipart.org/detail/23508/cartoon-brontosaurus',
      desc: 'Bé khủng long sấm Brontosaurus bước đi thong thả, mắt tròn ngơ ngác, cổ dài uốn cong và đuôi nhọn.' },

    // 6. Trái cây & Đồ ăn (Food & Fruits) - 4
    { id: '84631', file: 'food_84631_a_is_for_apple.svg', title: 'Apple with Leaf', cat: 'Trái cây & Đồ ăn', catId: 'food', artist: 'mazeo', url: 'https://openclipart.org/detail/84631/a-is-for-apple',
      desc: 'Quả táo tròn đầy đặn có cuống và một chiếc lá nhỏ ở đỉnh, bên cạnh có chữ cái in hoa "A" rỗng ruột để tô.' },
    { id: '8546', file: 'food_8546_simple_fruit_apple.svg', title: 'Sliced Apple Half', cat: 'Trái cây & Đồ ăn', catId: 'food', artist: 'Gerald_G', url: 'https://openclipart.org/detail/8546/simple-fruit-apple',
      desc: 'Nửa quả táo bổ dọc để lộ lõi hạt hình trái tim, hai hạt táo đen ở giữa, vỏ ngoài viền cong rõ ràng.' },
    { id: '177813', file: 'food_177813_banana_bunch_line_ar.svg', title: 'Banana Bunch', cat: 'Trái cây & Đồ ăn', catId: 'food', artist: 'Child_of_Light', url: 'https://openclipart.org/detail/177813/banana-bunch-line-art',
      desc: 'Nải chuối gồm 3 quả chuối cong gắn liền cuống, đường gân dọc thân chia các múi chuối thành từng mảng riêng.' },
    { id: '245073', file: 'food_245073_lemon_lineart.svg', title: 'Fresh Lemon & Slices', cat: 'Trái cây & Đồ ăn', catId: 'food', artist: 'elizavella', url: 'https://openclipart.org/detail/245073/lemon-lineart',
      desc: 'Quả chanh vàng căng tròn kèm lát chanh cắt ngang để lộ các múi chanh tỏa tròn đối xứng và cuống lá tươi.' },

    // 7. Phong cảnh đơn giản (Scenery) - 4
    { id: '132469', file: 'scenery_132469_family_house_raskorj.svg', title: 'Country Cottage', cat: 'Phong cảnh đơn giản', catId: 'scenery', artist: 'vlodco_zotov', url: 'https://openclipart.org/detail/132469/family-house-raskorjaka-for-tshirt-printing',
      desc: 'Ngôi nhà mái ngói một tầng nông thôn ấm cúng, có ống khói trên mái, cửa sổ ô vuông và hàng rào bao quanh.' },
    { id: '234766', file: 'scenery_234766_rainbow_for_coloring.svg', title: 'Rainbow with Clouds', cat: 'Phong cảnh đơn giản', catId: 'scenery', artist: 'spacefem', url: 'https://openclipart.org/detail/234766/rainbow-for-coloring',
      desc: 'Cầu vồng 7 dải uốn cong hình vòng cung, hai đầu cầu cắm vào hai đám mây xốp bồng bềnh như kẹo bông.' },
    { id: '235489', file: 'scenery_235489_mountain_outlines_si.svg', title: 'Mountain Peaks', cat: 'Phong cảnh đơn giản', catId: 'scenery', artist: 'rejon', url: 'https://openclipart.org/detail/235489/mountain-outlines-simpler',
      desc: 'Dãy núi đá trập trùng với các đỉnh nhọn, các vách núi chia thành các mảng hình học lớn dễ đổ màu.' },
    { id: '16504', file: 'scenery_16504_castle.svg', title: 'Fairy Tale Castle', cat: 'Phong cảnh đơn giản', catId: 'scenery', artist: 'machovka', url: 'https://openclipart.org/detail/16504/castle',
      desc: 'Lâu đài cổ tích với các tháp canh chóp nón, tường thành có lỗ châu mai, cổng vòm lớn và cờ bay phấp phới.' },

    // 8. Chữ cái & Chữ số (Letters & Numbers) - 5
    { id: '87049', file: 'letters_87049_b_is_for_butterfly.svg', title: 'Letter B & Butterfly', cat: 'Chữ cái & chữ số', catId: 'letters', artist: 'mazeo', url: 'https://openclipart.org/detail/87049/b-is-for-butterfly',
      desc: 'Chữ B in hoa viền rỗng cỡ lớn, bên cạnh có chú bướm nhỏ xinh xắn đang bay lượn để bé tập nhận diện chữ cái.' },
    { id: '190567', file: 'letters_190567_letter_c_for_cat_for.svg', title: 'Letter C & Sleeping Cat', cat: 'Chữ cái & chữ số', catId: 'letters', artist: 'migranerp', url: 'https://openclipart.org/detail/190567/letter-c-for-cat-for-coloring',
      desc: 'Chữ C in hoa nét dày rỗng, lồng ghép hình chú mèo mướp đang cuộn tròn say ngủ ở góc dưới.' },
    { id: '87061', file: 'letters_87061_d_is_for_dog.svg', title: 'Letter D & Puppy Dog', cat: 'Chữ cái & chữ số', catId: 'letters', artist: 'mazeo', url: 'https://openclipart.org/detail/87061/d-is-for-dog',
      desc: 'Chữ D in hoa rỗng ruột lồng ghép chú cún con ngồi vẫy đuôi vui mừng, các mảng nét bo tròn mềm mại.' },
    { id: '190572', file: 'letters_190572_p_for_pencil_for_col.svg', title: 'Letter P & Pencil', cat: 'Chữ cái & chữ số', catId: 'letters', artist: 'migranerp', url: 'https://openclipart.org/detail/190572/p-for-pencil-for-coloring',
      desc: 'Chữ P in hoa rỗng ruột kết hợp cây bút chì có đầu chì nhọn và tẩy ở đuôi, nét vẽ chân phương dễ tô.' },
    { id: '190575', file: 'letters_190575_v_for_violin_for_col.svg', title: 'Letter V & Violin', cat: 'Chữ cái & chữ số', catId: 'letters', artist: 'migranerp', url: 'https://openclipart.org/detail/190575/v-for-violin-for-coloring',
      desc: 'Chữ V in hoa cỡ lớn đi kèm cây đàn vĩ cầm (violin) với các dây đàn và cần kéo để bé làm quen âm nhạc.' }
];

console.log(`Starting conversion & contact sheet generation for ${templates40.length} templates...`);

// Render each template to 1024x1024 neon white-on-black PNG
const renderedPngs = [];

for (let i = 0; i < templates40.length; i++) {
    const t = templates40[i];
    const svgPath = path.join(downloadDir, t.file);
    const svgStr = fs.readFileSync(svgPath, 'utf8');

    const resvg = new Resvg(svgStr, { fitTo: { mode: 'width', value: 1024 } });
    const rawPng = PNG.sync.read(resvg.render().asPng());

    // Scale and center onto black 1024x1024
    const neonPng = new PNG({ width: 1024, height: 1024 });
    // Fill dark black #101014
    for (let p = 0; p < neonPng.data.length; p += 4) {
        neonPng.data[p] = 16;
        neonPng.data[p + 1] = 16;
        neonPng.data[p + 2] = 20;
        neonPng.data[p + 3] = 255;
    }

    const scale = Math.min(940 / rawPng.width, 940 / rawPng.height);
    const targetW = Math.round(rawPng.width * scale);
    const targetH = Math.round(rawPng.height * scale);
    const offX = Math.round((1024 - targetW) / 2);
    const offY = Math.round((1024 - targetH) / 2);

    for (let y = 0; y < targetH; y++) {
        for (let x = 0; x < targetW; x++) {
            const sx = Math.floor(x / scale);
            const sy = Math.floor(y / scale);
            const sIdx = (sy * rawPng.width + sx) * 4;
            const r = rawPng.data[sIdx];
            const g = rawPng.data[sIdx + 1];
            const b = rawPng.data[sIdx + 2];
            const a = rawPng.data[sIdx + 3];

            let stroke = false;
            if (a > 50) {
                const lum = 0.299 * r + 0.587 * g + 0.114 * b;
                if (lum < 220) stroke = true;
            }

            if (stroke) {
                const dx = offX + x;
                const dy = offY + y;
                if (dx >= 0 && dx < 1024 && dy >= 0 && dy < 1024) {
                    const dIdx = (dy * 1024 + dx) * 4;
                    neonPng.data[dIdx] = 255;
                    neonPng.data[dIdx + 1] = 255;
                    neonPng.data[dIdx + 2] = 255;
                    neonPng.data[dIdx + 3] = 255;
                }
            }
        }
    }

    // Dilate 2px to ensure solid >= 4px stroke
    const dilated = new PNG({ width: 1024, height: 1024 });
    dilated.data.set(neonPng.data);

    for (let y = 2; y < 1022; y++) {
        for (let x = 2; x < 1022; x++) {
            const idx = (y * 1024 + x) * 4;
            if (neonPng.data[idx] === 255) {
                for (let dy = -2; dy <= 2; dy++) {
                    for (let dx = -2; dx <= 2; dx++) {
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

    const outPath = path.join(outputPngDir, `template_${t.catId}_${t.id}.png`);
    fs.writeFileSync(outPath, PNG.sync.write(dilated));
    renderedPngs.push({ ...t, png: dilated, outPath });
    console.log(`[${i + 1}/40] Rendered: template_${t.catId}_${t.id}.png`);
}

// Function to generate a Contact Sheet (Grid 4 columns x 5 rows = 20 images)
function buildContactSheet(slice, sheetIndex) {
    const cols = 4;
    const rows = 5;
    const thumbSize = 460;
    const padding = 20;
    const headerHeight = 40;
    const cellW = thumbSize + padding * 2;
    const cellH = thumbSize + padding * 2 + headerHeight;
    const sheetW = cols * cellW;
    const sheetH = rows * cellH;

    const sheet = new PNG({ width: sheetW, height: sheetH });

    // Fill dark slate background #0F172A
    for (let p = 0; p < sheet.data.length; p += 4) {
        sheet.data[p] = 15;
        sheet.data[p + 1] = 23;
        sheet.data[p + 2] = 42;
        sheet.data[p + 3] = 255;
    }

    // Downscale and paste each thumbnail
    for (let i = 0; i < slice.length; i++) {
        const item = slice[i];
        const col = i % cols;
        const row = Math.floor(i / cols);

        const originX = col * cellW + padding;
        const originY = row * cellH + padding + headerHeight;

        // Draw card border
        for (let cy = -2; cy < thumbSize + 2; cy++) {
            for (let cx = -2; cx < thumbSize + 2; cx++) {
                const px = originX + cx;
                const py = originY + cy;
                if (px >= 0 && px < sheetW && py >= 0 && py < sheetH) {
                    const idx = (py * sheetW + px) * 4;
                    sheet.data[idx] = 30;
                    sheet.data[idx + 1] = 41;
                    sheet.data[idx + 2] = 59;
                    sheet.data[idx + 3] = 255;
                }
            }
        }

        // Downscale 1024x1024 into thumbSize x thumbSize
        const step = 1024 / thumbSize;
        for (let ty = 0; ty < thumbSize; ty++) {
            for (let tx = 0; tx < thumbSize; tx++) {
                const srcX = Math.floor(tx * step);
                const srcY = Math.floor(ty * step);
                const srcIdx = (srcY * 1024 + srcX) * 4;

                const val = item.png.data[srcIdx];
                if (val > 128) {
                    const px = originX + tx;
                    const py = originY + ty;
                    const idx = (py * sheetW + px) * 4;
                    sheet.data[idx] = 224;
                    sheet.data[idx + 1] = 242;
                    sheet.data[idx + 2] = 254;
                    sheet.data[idx + 3] = 255;
                }
            }
        }
    }

    const outName = `contact_sheet_${sheetIndex}.png`;
    const outPath = path.join(contactSheetDir, outName);
    const buf = PNG.sync.write(sheet);
    fs.writeFileSync(outPath, buf);

    // Also copy to artifactDir
    const artifactPath = path.join(artifactDir, outName);
    fs.writeFileSync(artifactPath, buf);

    console.log(`Generated Contact Sheet: ${outPath} (${(buf.length / 1024).toFixed(1)} KB)`);
    return { outName, outPath, artifactPath, sizeKb: (buf.length / 1024).toFixed(1) };
}

const sheet1 = buildContactSheet(renderedPngs.slice(0, 20), 1);
const sheet2 = buildContactSheet(renderedPngs.slice(20, 40), 2);

console.log('Contact sheets successfully created!');

// Save catalog metadata
fs.writeFileSync('D:/DoodleStudio/scratch/final_40_catalog.json', JSON.stringify(templates40, null, 2));
