const fs = require('fs');
const path = require('path');

const vetted = JSON.parse(fs.readFileSync('D:/DoodleStudio/scratch/vetted_candidates.json', 'utf8'));

for (let i = 0; i < vetted.length; i++) {
    const item = vetted[i];
    const p = path.join('D:/DoodleStudio/scratch/downloaded_templates', item.localFile);
    const content = fs.readFileSync(p, 'utf8');
    
    // Look for clues in SVG
    const sodipodiDocname = content.match(/sodipodi:docname="([^"]+)"/i);
    const inkscapeLabel = content.match(/inkscape:label="([^"]+)"/i);
    const dcTitle = content.match(/<dc:title>([^<]+)<\/dc:title>/i);
    const dcDesc = content.match(/<dc:description>([^<]+)<\/dc:description>/i);
    
    console.log(`[${i+1}] ${item.localFile} | ID:${item.id} | Cat:${item.category} | Title:${item.title} | Artist:${item.artist}`);
    if (sodipodiDocname) console.log(`    docname: ${sodipodiDocname[1]}`);
    if (inkscapeLabel) console.log(`    label: ${inkscapeLabel[1]}`);
    if (dcTitle) console.log(`    dc:title: ${dcTitle[1]}`);
    if (dcDesc) console.log(`    dc:desc: ${dcDesc[1]}`);
}

