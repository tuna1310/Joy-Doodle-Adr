const fs = require('fs');
const path = require('path');

const vetted = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/vetted_candidates.json', 'utf8'));

console.log(`Loaded ${vetted.length} vetted candidates.`);

vetted.forEach((item, index) => {
    const filePath = path.join('D:/DoodleStudio/scratch/downloaded_templates', item.localFile);
    const stat = fs.statSync(filePath);
    const content = fs.readFileSync(filePath, 'utf8');

    // Parse SVG title/desc or look at paths
    console.log(`[#${index + 1}]`);
    console.log(`  File: ${item.localFile}`);
    console.log(`  Title: ${item.title}`);
    console.log(`  Category: ${item.category}`);
    console.log(`  Artist: ${item.artist}`);
    console.log(`  License: ${item.license}`);
    console.log(`  URL: ${item.detailUrl}`);
    console.log(`  Size: ${stat.size} bytes (${(stat.size / 1024).toFixed(1)} KB)`);
    console.log(`  Preview snippet: ${content.substring(0, 150).replace(/\r?\n/g, ' ')}`);
});

