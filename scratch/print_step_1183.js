const fs = require('fs');

const content = fs.readFileSync('C:/Users/Admin/.gemini/antigravity/brain/fbf229bc-466c-4bfe-b871-a25dff2d39b4/.system_generated/logs/transcript.jsonl', 'utf8');
const lines = content.split('\n');

for (let i = 0; i < lines.length; i++) {
  try {
    const obj = JSON.parse(lines[i]);
    if (obj.step_index === 1183 || i === 1183) {
      console.log(obj.content);
    }
  } catch(e) {}
}

