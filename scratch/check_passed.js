const fs = require('fs');

const results = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/fill_test_results.json', 'utf8'));
const passed = results.filter(r => r.fillStatus === 'Đạt');

console.log(`=== TOTAL PASSED: ${passed.length} ===`);
const cats = {};
passed.forEach((p, idx) => {
    cats[p.cat] = (cats[p.cat] || 0) + 1;
    console.log(`[#${idx+1}] ID:${p.id} | Cat:${p.cat} | Title:${p.title} | Stroke:${p.avgThickness}px | Regions:${p.significantRegions}`);
});

console.log('\nDistribution by Category:');
console.log(cats);
fs.writeFileSync('D:/DoodleStudio/scratch/passed_candidates.json', JSON.stringify(passed, null, 2));

