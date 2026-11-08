const fs = require('fs');
const path = require('path');
const { Resvg } = require('D:/DoodleStudio/scratch/node_modules/@resvg/resvg-js');
const { PNG } = require('D:/DoodleStudio/scratch/node_modules/pngjs');

const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';
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
            isPass: totalInterior >= 150000 && sigRegions.length >= 3 && sigRegions.length <= 80,
            mask
        };
    } catch (e) {
        return null;
    }
}

// 40 selected items: exactly 5 per category
const selectedTemplates = [
    // 1. ĐỘNG VẬT (Animals) - 5
    {
        id: '330237',
        file: 'animals_330237_cute_bear_sitting.svg',
        title: 'Cute Bear Sitting',
        titleVi: 'Gấu con ngồi chơi',
        cat: 'Động vật',
        catId: 'animals',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/330237/cute-bear-sitting',
        visualDesc: 'Chú gấu bông tròn trĩnh đang ngồi với hai tai tròn, chiếc mũi to và hai bàn chân xòe ra dễ thương.'
    },
    {
        id: '284788',
        file: 'animals_284788_horse.svg',
        title: 'Standing Horse',
        titleVi: 'Chú ngựa đứng',
        cat: 'Động vật',
        catId: 'animals',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/284788/horse',
        visualDesc: 'Chú ngựa với bờm dài, đuôi cong mềm mại và bốn chân vững chãi trong tư thế đứng tự tin.'
    },
    {
        id: '312543',
        file: 'animals_312543_kawaii_kitty_line_ar.svg',
        title: 'Kawaii Kitty',
        titleVi: 'Mèo con Kawaii',
        cat: 'Động vật',
        catId: 'animals',
        artist: 'teck5',
        url: 'https://openclipart.org/detail/312543/kawaii-kitty-line-art',
        visualDesc: 'Mèo con phong cách chibi Nhật Bản với chiếc đầu to, mắt lấp lánh và đôi tai vểnh đáng yêu.'
    },
    {
        id: '328812',
        file: 'animals_328812_feliform_face_colori.svg',
        title: 'Feliform Face',
        titleVi: 'Mặt mèo ngộ nghĩnh',
        cat: 'Động vật',
        catId: 'animals',
        artist: 'AdamStanislav',
        url: 'https://openclipart.org/detail/328812/feliform-face-coloring-book-version',
        visualDesc: 'Khuôn mặt chú mèo hoạt hình với đường nét đơn giản, đôi mắt tròn xoe, mũi tam giác và 6 sợi râu hai bên má.'
    },
    {
        id: '352670',
        file: 'animals_352670_simple_butterfly_out.svg',
        title: 'Simple Butterfly',
        titleVi: 'Bướm xuân xòe cánh',
        cat: 'Động vật',
        catId: 'animals',
        artist: 'teck5',
        url: 'https://openclipart.org/detail/352670/simple-butterfly-outline',
        visualDesc: 'Cánh bướm đối xứng tuyệt đẹp xòe rộng với các ô hoa văn lớn bên trong cánh rất dễ tô màu neon.'
    },

    // 2. PHƯƠNG TIỆN (Vehicles) - 5
    {
        id: '176375',
        file: 'vehicles_176375_bus_remixed.svg',
        title: 'City Bus',
        titleVi: 'Xe buýt thành phố',
        cat: 'Phương tiện',
        catId: 'vehicles',
        artist: 'j4p4n',
        url: 'https://openclipart.org/detail/176375/bus-remixed',
        visualDesc: 'Chiếc xe buýt lớn với các ô cửa kính sổ chữ nhật, bánh xe tròn to và thân xe vuông vức ngộ nghĩnh.'
    },
    {
        id: '228075',
        file: 'vehicles_228075_airplane_with_propel.svg',
        title: 'Airplane with Propeller',
        titleVi: 'Máy bay cánh quạt',
        cat: 'Phương tiện',
        catId: 'vehicles',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/228075/airplane-with-propeller',
        visualDesc: 'Máy bay cánh đơn cổ điển với chong chóng cánh quạt phía mũi, thân tròn và cánh ngang sải rộng.'
    },
    {
        id: '298680',
        file: 'vehicles_298680_sail_ship_2_masts_ou.svg',
        title: 'Sailboat 2 Masts',
        titleVi: 'Thuyền buồm ra khơi',
        cat: 'Phương tiện',
        catId: 'vehicles',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/298680/sail-ship-2-masts-outline',
        visualDesc: 'Con thuyền lướt sóng với thân thuyền cong vút, hai cột buồm căng gió với các mảng buồm tam giác lớn.'
    },
    {
        id: '312018',
        file: 'vehicles_312018_car.svg',
        title: 'City Car',
        titleVi: 'Xe ô tô con',
        cat: 'Phương tiện',
        catId: 'vehicles',
        artist: 'gustavorezende',
        url: 'https://openclipart.org/detail/312018/car',
        visualDesc: 'Chiếc ô tô gia đình nhìn nghiêng với hai bánh xe lớn, đèn pha tròn, kính chắn gió và cửa xe viền dày rõ ràng.'
    },
    {
        id: '272531',
        file: 'vehicles_272531_rocket_coloring_page.svg',
        title: 'Rocket in Space',
        titleVi: 'Tên lửa không gian',
        cat: 'Phương tiện',
        catId: 'vehicles',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/272531/rocket-coloring-page',
        visualDesc: 'Tên lửa vũ trụ hình nón vút lên trời cao với cửa sổ tròn nhìn ra các vì sao và vây đuôi cân đối.'
    },

    // 3. BIỂN (Sea) - 5
    {
        id: '74233',
        file: 'sea_74233_green_sea_turtle_lin.svg',
        title: 'Green Sea Turtle',
        titleVi: 'Rùa biển xanh',
        cat: 'Biển',
        catId: 'sea',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/74233/green-sea-turtle-line-art',
        visualDesc: 'Chú rùa biển đang bơi nhìn từ trên xuống với mai rùa phân chia thành các ô vảy lớn và bốn mái chèo xòe rộng.'
    },
    {
        id: '325114',
        file: 'sea_325114_turtle_side_outline.svg',
        title: 'Turtle Swimming Side',
        titleVi: 'Rùa bơi nhìn nghiêng',
        cat: 'Biển',
        catId: 'sea',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/325114/turtle-side-outline',
        visualDesc: 'Chú rùa bơi nghiêng với chiếc đầu ngước lên thở bọt khí, mai vòm cao và hai chân trước quạt nước điệu nghệ.'
    },
    {
        id: '325250',
        file: 'sea_325250_cartoon_octopus.svg',
        title: 'Cartoon Octopus',
        titleVi: 'Bạch tuộc tinh nghịch',
        cat: 'Biển',
        catId: 'sea',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/325250/cartoon-octopus',
        visualDesc: 'Chú bạch tuộc vui nhộn với phần đầu tròn to, đôi mắt tinh nghịch và các xúc tu uốn lượn mềm mại.'
    },
    {
        id: '333629',
        file: 'sea_333629_a_smiling_eel_versio.svg',
        title: 'Smiling Sea Eel',
        titleVi: 'Cá chình mỉm cười',
        cat: 'Biển',
        catId: 'sea',
        artist: 'AdamStanislav',
        url: 'https://openclipart.org/detail/333629/a-smiling-eel-version-2',
        visualDesc: 'Chú cá chình biển thân uốn lượn hình chữ S nhấp nhô, miệng cười tươi để lộ niềm vui tung tăng dưới đáy biển.'
    },
    {
        id: '170171',
        file: 'sea_170171_sea_fantasy.svg',
        title: 'Sea Fantasy Shell',
        titleVi: 'Vỏ sò biển kỳ diệu',
        cat: 'Biển',
        catId: 'sea',
        artist: 'j4p4n',
        url: 'https://openclipart.org/detail/170171/sea-fantasy',
        visualDesc: 'Vỏ sò biển xếp tầng tự nhiên với các đường rãnh tỏa đều từ tâm như chiếc quạt xòe duyên dáng.'
    },

    // 4. HOA LÁ (Flowers) - 5
    {
        id: '352672',
        file: 'flowers_352672_simple_flower_head_l.svg',
        title: 'Sunflower Daisy',
        titleVi: 'Hoa hướng dương tỏa rạng',
        cat: 'Hoa lá',
        catId: 'flowers',
        artist: 'teck5',
        url: 'https://openclipart.org/detail/352672/simple-flower-head-line-art',
        visualDesc: 'Bông hoa cúc/hướng dương với nhụy tròn ở giữa và vòng cánh hoa hình giọt nước nở đều xung quanh.'
    },
    {
        id: '352740',
        file: 'flowers_352740_imaginary_flower_hea.svg',
        title: 'Mandala Flower Head',
        titleVi: 'Hoa Mandala rực rỡ',
        cat: 'Hoa lá',
        catId: 'flowers',
        artist: 'teck5',
        url: 'https://openclipart.org/detail/352740/imaginary-flower-head-line-art',
        visualDesc: 'Bông hoa nghệ thuật xếp nhiều lớp cánh hoa đối xứng hình học tỏa tròn từ nhụy hoa ra ngoài rất cuốn hút.'
    },
    {
        id: '298288',
        file: 'flowers_298288_rose_4.svg',
        title: 'Rose Blossom',
        titleVi: 'Đóa hoa hồng ngát hương',
        cat: 'Hoa lá',
        catId: 'flowers',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/298288/rose-4',
        visualDesc: 'Đóa hoa hồng đang hé nở với các cánh hoa cuộn xoắn ốc uyển chuyển cùng phiến lá răng cưa bên dưới.'
    },
    {
        id: '312540',
        file: 'flowers_312540_poinsettia_in_a_pot_.svg',
        title: 'Poinsettia in Pot',
        titleVi: 'Chậu hoa trạng nguyên',
        cat: 'Hoa lá',
        catId: 'flowers',
        artist: 'teck5',
        url: 'https://openclipart.org/detail/312540/poinsettia-in-a-pot-line-art',
        visualDesc: 'Chậu hoa trạng nguyên xinh xắn với những chiếc lá nhọn hình ngôi sao xòe trên miệng chậu gốm.'
    },
    {
        id: '26146',
        file: 'flowers_26146_bee_and_flowers.svg',
        title: 'Bee & Spring Blossom',
        titleVi: 'Ong chăm và hoa xuân',
        cat: 'Hoa lá',
        catId: 'flowers',
        artist: 'Gerald_G',
        url: 'https://openclipart.org/detail/26146/bee-and-flowers',
        visualDesc: 'Chú ong mật dễ thương đang bay lượn bên cành hoa xuân với những cánh hoa nở rộ chào ngày mới.'
    },

    // 5. KHỦNG LONG (Dinosaurs) - 5
    {
        id: '1384',
        file: 'dinosaurs_1384_dragon_with_horn.svg',
        title: 'Dragon Dino with Horn',
        titleVi: 'Khủng long sừng dũng mãnh',
        cat: 'Khủng long',
        catId: 'dinosaurs',
        artist: 'ludvig',
        url: 'https://openclipart.org/detail/1384/dragon-with-horn',
        visualDesc: 'Khủng long bạo chúa hoạt hình đứng bằng hai chân sau to khỏe, miệng há to và chiếc sừng nhọn trên đầu.'
    },
    {
        id: '281169',
        file: 'dinosaurs_281169_happy_dinosaur_outli.svg',
        title: 'Happy Dinosaur',
        titleVi: 'Khủng long cổ dài vui vẻ',
        cat: 'Khủng long',
        catId: 'dinosaurs',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/281169/happy-dinosaur-outline',
        visualDesc: 'Chú khủng long ăn cỏ thân thiện với chiếc cổ vươn cao, thân hình mập mạp và đôi mắt to tròn thân ái.'
    },
    {
        id: '234370',
        file: 'dinosaurs_234370_rawr_dinosaur_outlin.svg',
        title: 'Rawr Triceratops',
        titleVi: 'Khủng long ba sừng',
        cat: 'Khủng long',
        catId: 'dinosaurs',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/234370/rawr-dinosaur-outline',
        visualDesc: 'Khủng long ba sừng với chiếc khiên cổ xòe rộng bảo vệ gáy, ba chiếc sừng nhọn và bốn chân trụ chắc nịch.'
    },
    {
        id: '316177',
        file: 'dinosaurs_316177_white_cartoon_tricer.svg',
        title: 'Cartoon Triceratops Baby',
        titleVi: 'Khủng long con đáng yêu',
        cat: 'Khủng long',
        catId: 'dinosaurs',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/316177/white-cartoon-triceratops',
        visualDesc: 'Bé khủng long con mũm mĩm bước đi vui nhộn với chiếc yếm cổ viền lượn sóng và nụ cười ngây thơ.'
    },
    {
        id: '281168',
        file: 'dino_brontosaurus_01.svg',
        title: 'Gentle Brontosaurus',
        titleVi: 'Khủng long Brontosaurus',
        cat: 'Khủng long',
        catId: 'dinosaurs',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/281168/brontosaurus',
        visualDesc: 'Khủng long khổng lồ hiền lành với chiếc đuôi dài quét đất và chiếc cổ cao với tới những ngọn cây.'
    },

    // 6. TRÁI CÂY & ĐỒ ĂN (Food) - 5
    {
        id: '8546',
        file: 'food_8546_simple_fruit_apple.svg',
        title: 'Sliced Apple Half',
        titleVi: 'Quả táo cắt đôi',
        cat: 'Trái cây & Đồ ăn',
        catId: 'food',
        artist: 'Gerald_G',
        url: 'https://openclipart.org/detail/8546/simple-fruit-apple',
        visualDesc: 'Quả táo bổ đôi để lộ lõi hạt bên trong cùng cuống lá xanh tươi phía trên rất ngộ nghĩnh.'
    },
    {
        id: '8541',
        file: 'food_8541_watermelon.svg',
        title: 'Juicy Watermelon Slice',
        titleVi: 'Miếng dưa hấu mát lạnh',
        cat: 'Trái cây & Đồ ăn',
        catId: 'food',
        artist: 'Gerald_G',
        url: 'https://openclipart.org/detail/8541/simple-fruit-watermelon',
        visualDesc: 'Miếng dưa hấu hình trăng khuyết với lớp vỏ xanh sọc, cùi dưa và những hạt dưa đen bóng nằm trên nền ruột.'
    },
    {
        id: '8545',
        file: 'food_8545_orange.svg',
        title: 'Fresh Orange Slice',
        titleVi: 'Quả cam mọng nước',
        cat: 'Trái cây & Đồ ăn',
        catId: 'food',
        artist: 'Gerald_G',
        url: 'https://openclipart.org/detail/8545/simple-fruit-orange',
        visualDesc: 'Quả cam tròn căng mọng với cuống lá nhỏ xinh xắn trên đỉnh quả toát lên vẻ thơm ngọt.'
    },
    {
        id: '8539',
        file: 'food_8539_grapes.svg',
        title: 'Grape Bunch',
        titleVi: 'Chùm nho trĩu quả',
        cat: 'Trái cây & Đồ ăn',
        catId: 'food',
        artist: 'Gerald_G',
        url: 'https://openclipart.org/detail/8539/simple-fruit-grapes',
        visualDesc: 'Chùm nho sai trĩu quả với từng quả nho tròn đính liền nhau và chiếc lá nho hình bàn tay xanh mướt.'
    },
    {
        id: '177813',
        file: 'food_177813_banana_bunch_line_ar.svg',
        title: 'Banana Bunch',
        titleVi: 'Nải chuối tiêu vàng',
        cat: 'Trái cây & Đồ ăn',
        catId: 'food',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/177813/banana-bunch-line-art',
        visualDesc: 'Ba quả chuối chín uốn cong ghép thành chùm với núm cuống chung rất quen thuộc và dễ thương.'
    },

    // 7. PHONG CẢNH (Scenery) - 5
    {
        id: '132469',
        file: 'scenery_132469_family_house_raskorj.svg',
        title: 'Country Cottage House',
        titleVi: 'Ngôi nhà nhỏ nông thôn',
        cat: 'Phong cảnh',
        catId: 'scenery',
        artist: 'j4p4n',
        url: 'https://openclipart.org/detail/132469/family-house-raskorj',
        visualDesc: 'Ngôi nhà xinh xắn với mái ngói tam giác có ống khói, cửa ra vào vòm gỗ và hai khung cửa sổ ô vuông ấm cúng.'
    },
    {
        id: '234766',
        file: 'scenery_234766_rainbow_for_coloring.svg',
        title: 'Rainbow with Clouds',
        titleVi: 'Cầu vồng và mây trắng',
        cat: 'Phong cảnh',
        catId: 'scenery',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/234766/rainbow-for-coloring',
        visualDesc: 'Dải cầu vồng 7 màu uốn cong duyên dáng bắc ngang trời nối liền hai đám mây bồng bềnh xốp nhẹ.'
    },
    {
        id: '229963',
        file: 'scenery_229963_color.svg',
        title: 'Windmill in Meadow',
        titleVi: 'Cối xay gió trên đồng cỏ',
        cat: 'Phong cảnh',
        catId: 'scenery',
        artist: 'j4p4n',
        url: 'https://openclipart.org/detail/229963/color',
        visualDesc: 'Cối xay gió phong cách Hà Lan với 4 cánh quạt chữ thập lớn, thân tháp đá tròn và mái chóp nhọn.'
    },
    {
        id: '181418',
        file: 'scenery_181418_l_shaped_building.svg',
        title: 'Modern Villa House',
        titleVi: 'Biệt thự sân vườn',
        cat: 'Phong cảnh',
        catId: 'scenery',
        artist: 'rejon',
        url: 'https://openclipart.org/detail/181418/l-shaped-building',
        visualDesc: 'Tòa nhà kiến trúc chữ L hiện đại với nhiều mảng tường hình học, ban công thoáng đãng và lối đi lát gạch.'
    },
    {
        id: '181419',
        file: 'scenery_181419_rectangle_shaped_bui.svg',
        title: 'City School Building',
        titleVi: 'Trường học em yêu',
        cat: 'Phong cảnh',
        catId: 'scenery',
        artist: 'rejon',
        url: 'https://openclipart.org/detail/181419/rectangle-shaped-building',
        visualDesc: 'Khu nhà hai tầng ngay ngắn với các dãy cửa sổ đón ánh ban mai và hàng hiên rộng chào đón học sinh.'
    },

    // 8. CHỮ CÁI & BIỂU TƯỢNG (Letters & Symbols) - 5
    {
        id: '87055',
        file: 'letters_87055_c_is_for_car.svg',
        title: 'Letter C & Car',
        titleVi: 'Chữ C và Ô tô nhỏ',
        cat: 'Chữ cái',
        catId: 'letters',
        artist: 'Gerald_G',
        url: 'https://openclipart.org/detail/87055/c-is-for-car',
        visualDesc: 'Chữ cái C in hoa nét dày bản lớn đứng cạnh chiếc xe hơi con tinh nghịch với bánh xe tròn xoe.'
    },
    {
        id: '88651',
        file: 'letters_88651_m_is_for_mug.svg',
        title: 'Letter M & Mug',
        titleVi: 'Chữ M và Chiếc cốc',
        cat: 'Chữ cái',
        catId: 'letters',
        artist: 'Gerald_G',
        url: 'https://openclipart.org/detail/88651/m-is-for-mug',
        visualDesc: 'Chữ cái M in hoa vững chãi với đường nét uốn lượn cạnh chiếc cốc uống nước có quai tròn trĩnh.'
    },
    {
        id: '88813',
        file: 'letters_88813_r_is_for_rainbow.svg',
        title: 'Letter R & Rainbow',
        titleVi: 'Chữ R và Cầu vồng',
        cat: 'Chữ cái',
        catId: 'letters',
        artist: 'Gerald_G',
        url: 'https://openclipart.org/detail/88813/r-is-for-rainbow',
        visualDesc: 'Chữ cái R in hoa duyên dáng đi kèm hình cung cầu vồng cong vút tỏa sáng trên nền trời.'
    },
    {
        id: '312381',
        file: 'letters_312381_euclid_initial_lette.svg',
        title: 'Decorated Letter A',
        titleVi: 'Chữ A hoa văn cổ tích',
        cat: 'Chữ cái',
        catId: 'letters',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/312381/euclid-initial-letter-a',
        visualDesc: 'Chữ cái A phong cách cổ tích hoàng gia với những dải dây leo quấn quanh thanh chữ rất trang nhã.'
    },
    {
        id: '312382',
        file: 'letters_312382_euclid_initial_lette.svg',
        title: 'Decorated Letter B',
        titleVi: 'Chữ B hoa văn quý phái',
        cat: 'Chữ cái',
        catId: 'letters',
        artist: 'GDJ',
        url: 'https://openclipart.org/detail/312382/euclid-initial-letter-b',
        visualDesc: 'Chữ cái B với hai bụng chữ uốn tròn mềm mại kết hợp hoa lá đối xứng hai bên tạo nét quý phái.'
    }
];

console.log(`Evaluating all ${selectedTemplates.length} templates with strict criteria...`);

let failCount = 0;
const results = [];

for (let i = 0; i < selectedTemplates.length; i++) {
    const item = selectedTemplates[i];
    const full = path.join(downloadDir, item.file);
    if (!fs.existsSync(full)) {
        console.error(`MISSING FILE: ${item.file}`);
        failCount++;
        continue;
    }
    const sz = fs.statSync(full).size;
    const r = evaluateSvg(full);
    if (!r || !r.isPass) {
        console.error(`FAILED: ${item.file} -> Area: ${r ? r.fillPct : 0}% (${r ? Math.round(r.totalInterior/1000) : 0}k), Sig: ${r ? r.sigCount : 0}`);
        failCount++;
    } else {
        console.log(`[PASS ${i+1}/40] [${item.cat}] ${item.title} -> Area: ${r.fillPct}% (${Math.round(r.totalInterior/1000)}k px), Sig: ${r.sigCount}`);
        results.push({
            stt: i + 1,
            ...item,
            fileSizeKb: (sz / 1024).toFixed(1),
            totalInterior: r.totalInterior,
            fillPct: r.fillPct,
            sigCount: r.sigCount,
            totalRegions: r.allRegionsCount,
            result: 'Đạt'
        });
    }
}

console.log(`\nFinal Verdict: ${results.length} PASSED out of ${selectedTemplates.length}. Fail count: ${failCount}`);

if (failCount === 0) {
    fs.writeFileSync('scratch/perfect_40_catalog.json', JSON.stringify(results, null, 2));
    console.log('Saved scratch/perfect_40_catalog.json successfully!');
}

