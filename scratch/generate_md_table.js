const fs = require('fs');

const verified = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/master_verified.json', 'utf8'));

let md = '| STT | Danh mục | Tên file đã lưu | Link trang xác nhận | Tác giả | Giấy phép | Kích thước | Mô tả hình thật sự nhìn thấy |\n';
md += '|---|---|---|---|---|---|---|---|\n';

verified.forEach((item, index) => {
    const sizeKb = (item.fileSizeBytes / 1024).toFixed(1) + ' KB';
    const link = `[#${item.id}](${item.detailUrl})`;
    md += `| ${index + 1} | ${item.category} | \`${item.localFile}\` | ${link} | ${item.artist} | ${item.license} | ${sizeKb} | ${item.visualDesc} |\n`;
});

fs.writeFileSync('D:/DoodleStudio/scratch/table.md', md, 'utf8');
console.log('Markdown table generated. Length:', md.length);

