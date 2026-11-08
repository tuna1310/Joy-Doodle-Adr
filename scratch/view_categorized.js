const fs = require('fs');
const passed = JSON.parse(fs.readFileSync('scratch/passed_candidates_clean.json', 'utf8'));

const categories = {
  animals: [],
  vehicles: [],
  sea: [],
  flowers: [],
  dinosaurs: [],
  food: [],
  scenery: [],
  letters: []
};

for (const p of passed) {
  let f = p.file;
  let cat = 'other';
  if (f.startsWith('animals_')) cat = 'animals';
  else if (f.startsWith('vehicles_')) cat = 'vehicles';
  else if (f.startsWith('sea_')) cat = 'sea';
  else if (f.startsWith('flowers_')) cat = 'flowers';
  else if (f.startsWith('dinosaurs_') || f.startsWith('dino_')) cat = 'dinosaurs';
  else if (f.startsWith('food_')) cat = 'food';
  else if (f.startsWith('scenery_')) cat = 'scenery';
  else if (f.startsWith('letters_')) cat = 'letters';
  
  if (categories[cat]) categories[cat].push(p);
  else console.log('Unknown cat:', f);
}

for (const k in categories) {
  console.log('=== ' + k.toUpperCase() + ' (' + categories[k].length + ') ===');
  categories[k].forEach(x => {
    console.log('  ' + x.file + ' | Area: ' + x.fillPct + '% (' + Math.round(x.totalInterior/1000) + 'k) | Sig: ' + x.sigCount);
  });
}

