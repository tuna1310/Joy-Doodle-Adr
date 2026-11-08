const fs = require('fs');
const path = require('path');

const downloadDir = 'D:/DoodleStudio/scratch/downloaded_templates';
const data = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/final_40_tested.json', 'utf8'));

let md = '| STT | Danh mục | Tên file đã lưu | Link trang xác nhận | Tác giả | Giấy phép | Kích thước | Kết quả thử đổ màu | Mô tả hình thật sự nhìn thấy |\n';
md += '|:---:|---|---|---|---|---|---|---|---|\n';

data.forEach((item, index) => {
    const p = path.join(downloadDir, item.file);
    const size = fs.existsSync(p) ? (fs.statSync(p).size / 1024).toFixed(1) + ' KB' : 'N/A';
    const link = `[#${item.id}](${item.url})`;
    md += `| ${index + 1} | ${item.cat} | \`${item.file}\` | ${link} | ${item.artist} | CC0 1.0 Universal | ${size} | ${item.fillResult} | ${item.desc} |\n`;
});

fs.writeFileSync('D:/DoodleStudio/scratch/table_final_40.md', md, 'utf8');
console.log('Fixed table_final_40.md with real file sizes!');
