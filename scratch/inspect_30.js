const fs = require('fs');
const path = require('path');

const kept = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/final_vetted.json', 'utf8'));

console.log('=== REMAINING 30 ITEMS ===');
kept.forEach((item, i) => {
    const p = path.join('D:/DoodleStudio/scratch/downloaded_templates', item.localFile);
    const sz = fs.statSync(p).size;
    console.log(`[${i+1}] ID:${item.id} | Cat:${item.category} | ${item.localFile} (${(sz/1024).toFixed(1)}KB)`);
    console.log(`     Title: ${item.title}`);
    console.log(`     Artist: ${item.artist}`);
    console.log(`     URL: ${item.detailUrl}`);
});

