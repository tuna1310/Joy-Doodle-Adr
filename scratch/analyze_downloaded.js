const fs = require('fs');
const path = require('path');

const dir = path.join(__dirname, 'downloaded_templates');
const jsonPath = path.join(__dirname, 'verified_downloads.json');
const items = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

console.log(`Total downloaded items to analyze: ${items.length}\n`);

const vetted = [];
const rejected = [];

for (const item of items) {
    const filePath = path.join(dir, item.localFile);
    if (!fs.existsSync(filePath)) {
        rejected.push({ ...item, reason: 'File does not exist on disk' });
        continue;
    }
    const content = fs.readFileSync(filePath, 'utf8');
    const size = fs.statSync(filePath).size;

    // Check size
    if (size > 800000) {
        rejected.push({ ...item, reason: `File too large / too complex for kids (${(size/1024).toFixed(0)} KB)` });
        continue;
    }

    // Check for inappropriate subjects
    const lowerTitle = item.title.toLowerCase();
    const lowerSlug = item.slug.toLowerCase();
    if (lowerTitle.includes('israel') || lowerTitle.includes('palestin') || lowerTitle.includes('building') || lowerTitle.includes('cube') || lowerTitle.includes('pi day')) {
        rejected.push({ ...item, reason: 'Wrong subject / not suitable for kids coloring' });
        continue;
    }

    // Check if SVG has path
    const pathMatches = content.match(/<path/g) || [];
    const circleMatches = content.match(/<circle/g) || [];
    const totalShapes = pathMatches.length + circleMatches.length;

    if (totalShapes === 0) {
        rejected.push({ ...item, reason: 'No vector paths found' });
        continue;
    }

    // Determine visual style
    let style = 'Nét line-art đen trắng rõ ràng, các vùng kín';
    if (content.includes('fill="#') || content.includes('fill: #') || content.includes('fill="rgb')) {
        // has color fill or black fill
        if (content.includes('stroke="#000000"') || content.includes('stroke: #000') || content.includes('stroke="black"')) {
            style = 'Nét viền đen rõ rệt, có mảng phân chia rõ';
        } else {
            style = 'Mảng hình khối vector đen trắng/màu phẳng';
        }
    }

    vetted.push({
        ...item,
        shapeCount: totalShapes,
        visualDesc: style
    });
}

console.log(`Vetted suitable candidates: ${vetted.length}`);
console.log(`Rejected: ${rejected.length}\n`);

fs.writeFileSync(path.join(__dirname, 'vetted_candidates.json'), JSON.stringify(vetted, null, 2));
fs.writeFileSync(path.join(__dirname, 'rejected_candidates.json'), JSON.stringify(rejected, null, 2));

