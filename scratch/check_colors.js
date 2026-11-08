const fs = require('fs');

const content = fs.readFileSync('C:/Users/Admin/.gemini/antigravity/brain/fbf229bc-466c-4bfe-b871-a25dff2d39b4/.system_generated/logs/transcript.jsonl', 'utf8');
const lines = content.split('\n');

for (let i = 0; i < lines.length; i++) {
  const line = lines[i];
  if (line.includes('bảng màu') || line.includes('palette') || line.includes('color picker') || line.includes('màu')) {
    try {
      const obj = JSON.parse(line);
      const text = obj.content || (obj.tool_calls ? JSON.stringify(obj.tool_calls) : '');
      if (text.includes('bảng màu') || text.includes('palette')) {
        console.log(`[Step ${obj.step_index || i}] ${text.substring(0, 200)}...\n`);
      }
    } catch(e) {}
  }
}

