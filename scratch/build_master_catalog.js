const fs = require('fs');
const path = require('path');

const outDir = path.join(__dirname, 'downloaded_templates');
const existingVetted = JSON.parse(fs.readFileSync(path.join(__dirname, 'final_vetted.json'), 'utf8'));
const allRejected = JSON.parse(fs.readFileSync(path.join(__dirname, 'all_rejected.json'), 'utf8'));

// Add the 3 verified flowers
const additionalFlowers = [
    {
        id: '352740',
        slug: 'imaginary-flower-head',
        detailUrl: 'https://openclipart.org/detail/352740/imaginary-flower-head',
        title: 'Imaginary Flower Head',
        artist: 'teck5',
        license: 'CC0 1.0 Universal (Public Domain)',
        localFile: 'flowers_352740_imaginary_flower_hea.svg',
        category: 'Hoa lá',
        catId: 'flowers'
    },
    {
        id: '352672',
        slug: 'simple-flower-head-lineart',
        detailUrl: 'https://openclipart.org/detail/352672/simple-flower-head-lineart',
        title: 'Simple Flower Head Lineart',
        artist: 'teck5',
        license: 'CC0 1.0 Universal (Public Domain)',
        localFile: 'flowers_352672_simple_flower_head_l.svg',
        category: 'Hoa lá',
        catId: 'flowers'
    },
    {
        id: '333586',
        slug: 'lilly-flower-abstract-art-coloring-page',
        detailUrl: 'https://openclipart.org/detail/333586/lilly-flower-abstract-art-coloring-page',
        title: 'Lilly Flower Abstract art coloring page',
        artist: 'elizavella',
        license: 'CC0 1.0 Universal (Public Domain)',
        localFile: 'flowers_333586_lilly_flower_abstrac.svg',
        category: 'Hoa lá',
        catId: 'flowers'
    }
];

for (const f of additionalFlowers) {
    if (!existingVetted.some(x => x.id === f.id)) {
        existingVetted.push(f);
    }
}

// Check every file on disk
const finalVerified = [];

// Curated visual descriptions based on actual SVG contents:
const visualDescriptions = {
    '328812': 'Khuôn mặt chú mèo hoạt hình với đường nét đơn giản, đôi mắt tròn xoe, mũi tam giác và 6 sợi râu hai bên má.',
    '312543': 'Chú mèo con phong cách Kawaii chibi ngồi tròn trịa, mắt to long lanh, tai nhọn và nét viền đen dày rõ ràng.',
    '300166': 'Đầu chú báo/mèo hoang dã cách điệu đường nét hình học sắc nét, các mảng đối xứng dễ tô từng mảng màu.',
    '245307': 'Tranh các loài động vật ngày thứ 6 (sư tử, hươu cao cổ, voi) tụ họp vui vẻ dưới tán cây rừng.',
    '352404': 'Nàng tiên bướm với đôi cánh họa tiết uốn lượn tinh xảo, tóc dài bồng bềnh và chiếc váy hoa xòe rộng.',
    '352740': 'Bông hoa cách điệu với nhụy hoa tròn lớn ở giữa và các lớp cánh hoa uốn lượn đối xứng như mạn đà la đơn giản.',
    '352672': 'Bông hoa hướng dương/cúc nét vẽ tay mộc mạc với 12 cánh hoa xòe tròn quanh tâm lớn, rất dễ tô màu.',
    '333586': 'Bông hoa ly duyên dáng với các cánh hoa vươn dài, gân hoa rõ rệt và các nụ hoa nhỏ vươn xung quanh.',
    '84631': 'Quả táo tròn đầy đặn kèm cuống và một chiếc lá xinh, bên cạnh có chữ cái in hoa "A" to lớn viền rỗng.',
    '8546': 'Quả táo bổ đôi đơn giản để lộ phần thịt quả và hai hạt táo đen ở chính giữa, viền nét to dễ thương.',
    '177814': 'Một quả chuối cong cong vẽ dạng line-art tối giản, có cuống và đầu núm phân tách rõ ràng.',
    '177813': 'Nải chuối gồm 3 quả chuối gắn liền phần cuống trên, các đường gân nổi chia múi chuối thành từng mảng riêng biệt.',
    '224175': 'Chiếc xe cổ Citroen 2CV kinh điển nhìn từ góc nghiêng, có đèn pha tròn lồi, gương chiếu hậu và bánh xe căm.',
    '181232': 'Chiếc xe ô tô thể thao hoạt hình vui nhộn mang biển số và bánh xe to tròn, thân xe cong vút dễ tô.',
    '164545': 'Chiếc xe con bọ Volkswagen Beetle huyền thoại với dáng tròn cổ điển, cản trước cong và hai đèn pha lồi.',
    '1384': 'Chú khủng long bạo chúa/rồng nhỏ có cánh và sừng ngộ nghĩnh, đuôi dài có gai nhọn đứng hiên ngang.',
    '330252': 'Khủng long T-Rex tí hon phong cách chibi đầu to thân nhỏ, hai tay ngắn cũn cỡn và đuôi vểnh tinh nghịch.',
    '330251': 'Khủng long Velociraptor con chạy nhảy với móng vuốt chân cong, đuôi giữ thăng bằng và miệng há tươi cười.',
    '281169': 'Chú khủng long cổ dài Brachiosaurus đang cười toe toét, thân hình mập mạp đáng yêu, chân như cột nhà.',
    '234370': 'Khủng long 3 sừng Triceratops với chiếc khiên cổ lớn bo tròn và 3 chiếc sừng ngộ nghĩnh trên trán.',
    '32701': 'Khủng long Stegosaurus lưng mang hàng gai hình phiến lá chạy dọc từ cổ xuống đuôi gai sắc nhọn.',
    '333629': 'Chú lươn biển/cá chình uốn lượn lượn sóng đang mỉm cười thân thiện dưới đáy biển, vây lưng chạy dài.',
    '318322': 'Khung cảnh đáy biển sinh động với các chú cá con bơi lội, rạn san hô, rong biển bồng bềnh và bọt khí.',
    '314003': 'Nàng tiên cá đang bơi lượn duyên dáng bên cạnh hai chú cá heo nhảy múa giữa làn sóng nước.',
    '312542': 'Chân dung nàng tiên cá với đôi mắt to tròn long lanh, mái tóc bồng bềnh cài sao biển và đuôi vảy cá.',
    '313993': 'Ngôi nhà gỗ ấm cúng giữa mùa đông phủ tuyết, có ống khói nhả khói, hàng rào gỗ và cây thông bao quanh.',
    '132469': 'Ngôi nhà mái ngói một tầng đơn giản với cửa sổ ô vuông, cửa ra vào có bậc thềm và ống khói trên mái.',
    '234766': 'Cầu vồng rực rỡ với 7 dải cung tròn uốn cong xuất phát từ hai đám mây xốp bồng bềnh ở hai đầu chân cầu.',
    '235489': 'Dãy núi trập trùng với các đỉnh núi nhọn tuyết phủ, tiền cảnh có đồi thoai thoải và vài bụi cây.',
    '190575': 'Chữ V in hoa cỡ lớn rỗng ruột đi kèm cây đàn vĩ cầm (violin) với dây đàn và cần kéo để bé tập tô chữ.',
    '190572': 'Chữ P in hoa rỗng ruột lồng ghép cây bút chì (pencil) có đầu gọt nhọn và tẩy ở đuôi ngộ nghĩnh.',
    '190567': 'Chữ C in hoa đi kèm chú mèo con (cat) cuộn tròn dễ thương, kết hợp học chữ cái tiếng Anh và con vật.',
    '190564': 'Chữ N in hoa kết hợp hình chiếc mũi (nariz) hoạt hình vui nhộn với hai lỗ mũi tròn xoe cho bé tô.'
};

for (const item of existingVetted) {
    const p = path.join(outDir, item.localFile);
    if (!fs.existsSync(p)) {
        console.error(`File missing: ${p}`);
        continue;
    }
    const stat = fs.statSync(p);
    item.fileSizeBytes = stat.size;
    item.visualDesc = visualDescriptions[item.id] || 'Đường nét line-art trắng đen sạch sẽ, các đường viền khép kín phù hợp tô màu.';
    finalVerified.push(item);
}

// Clean up duplicate entries in allRejected
const uniqueRejectedMap = new Map();
for (const r of allRejected) {
    if (!uniqueRejectedMap.has(r.id)) {
        uniqueRejectedMap.set(r.id, r);
    }
}
const finalRejected = Array.from(uniqueRejectedMap.values());

console.log(`Final Verified Count: ${finalVerified.length}`);
console.log(`Final Rejected Count: ${finalRejected.length}`);

fs.writeFileSync(path.join(__dirname, 'master_verified.json'), JSON.stringify(finalVerified, null, 2));
fs.writeFileSync(path.join(__dirname, 'master_rejected.json'), JSON.stringify(finalRejected, null, 2));

