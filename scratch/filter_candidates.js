const fs = require('fs');
const path = require('path');

const vetted = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/vetted_candidates.json', 'utf8'));

const kept = [];
const rejected = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/rejected_candidates.json', 'utf8'));

for (const item of vetted) {
    const p = path.join('D:/DoodleStudio/scratch/downloaded_templates', item.localFile);
    const content = fs.readFileSync(p, 'utf8');

    // Check colored illustrations:
    // If it contains vibrant colors like red (#ff0000, #ed1c24), green (#00ff00, #8cc63f, #009445), blue, yellow
    // Or if it's misclassified (Soyuz spacecraft as sea)
    let rejectReason = null;

    if (item.id === '215780') {
        rejectReason = 'Sai chủ đề: Metadata và hình thực tế là tàu vũ trụ Soyuz (Soyuz spacecraft), không phải sinh vật biển (Sea life).';
    } else if (item.id === '349016') {
        rejectReason = 'Không phải line-art tô màu: Tranh vẽ hoàn chỉnh có màu đổ (#203000, gradient), không phải nét viền đen trắng rỗng.';
    } else if (item.id === '26146') {
        rejectReason = 'Không phải line-art tô màu: Tranh vector đã tô sẵn màu (vàng, xanh lá, tím), không phải nét viền rỗng để bé tô.';
    } else if (item.id === '352406' || item.id === '352405') {
        rejectReason = 'Trùng lặp & đã tô màu: Bản tô màu sẵn (Version 1 & 2) của cùng một bức tranh Butterfly Fairy, chỉ giữ lại bản nét rỗng Outline (#352404).';
    } else if (item.id === '26140') {
        rejectReason = 'Không phải line-art tô màu: Tranh quả táo đã tô sẵn màu đỏ tươi (#ed1c24) và lá xanh (#009445).';
    } else if (item.id === '224174') {
        rejectReason = 'Trùng lặp: Bản biến thể độ phân giải nặng (400KB) của cùng xe Citroen 2 CV 1960 (#224175).';
    } else if (item.id === '284795') {
        rejectReason = 'File quá nặng & phức tạp (355 KB) với nhiều mảng bóng đổ xám đặc, không phải nét viền rỗng cho trẻ em.';
    }

    if (rejectReason) {
        rejected.push({
            id: item.id,
            title: item.title,
            file: item.localFile,
            reason: rejectReason
        });
    } else {
        kept.push(item);
    }
}

console.log(`Kept: ${kept.length}, Total Rejected: ${rejected.length}`);
fs.writeFileSync('D:/DoodleStudio/scratch/final_vetted.json', JSON.stringify(kept, null, 2));
fs.writeFileSync('D:/DoodleStudio/scratch/all_rejected.json', JSON.stringify(rejected, null, 2));

