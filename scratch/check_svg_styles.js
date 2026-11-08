const fs = require('fs');
const path = require('path');

const vetted = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/vetted_candidates.json', 'utf8'));

console.log('Inspecting each SVG structure...');
for (let i = 0; i < vetted.length; i++) {
    const item = vetted[i];
    const p = path.join('D:/DoodleStudio/scratch/downloaded_templates', item.localFile);
    const content = fs.readFileSync(p, 'utf8');

    const fillMatches = content.match(/fill[:=]\s*["']?([^;"'>\s]+)/gi) || [];
    const strokeMatches = content.match(/stroke[:=]\s*["']?([^;"'>\s]+)/gi) || [];
    
    // Check if fill is mostly none/white or if it has heavy colors
    const uniqueFills = Array.from(new Set(fillMatches.map(m => m.toLowerCase())));
    const uniqueStrokes = Array.from(new Set(strokeMatches.map(m => m.toLowerCase())));

    console.log(`[#${i+1}] ${item.localFile}`);
    console.log(`   Title: ${item.title} (${item.category})`);
    console.log(`   Size: ${(item.fileSizeBytes/1024).toFixed(1)} KB`);
    console.log(`   Fills sample: ${uniqueFills.slice(0, 5).join(', ')}`);
    console.log(`   Strokes sample: ${uniqueStrokes.slice(0, 5).join(', ')}`);
}

