import fs from 'fs';
import path from 'path';
import readline from 'readline';
import { exec } from 'child_process';

const pendingFile = path.resolve(process.cwd(), 'Chat_Bot_AI/data/pendingReview.json');
const trainingFile = path.resolve(process.cwd(), 'Chat_Bot_AI/data/trainingData.js');

if (!fs.existsSync(pendingFile)) {
  console.log("Không có file pendingReview.json. Không có gì để duyệt.");
  process.exit(0);
}

let pendingData = [];
try {
  pendingData = JSON.parse(fs.readFileSync(pendingFile, 'utf8'));
} catch (e) {
  console.log("Lỗi đọc pendingReview.json:", e.message);
  process.exit(1);
}

if (pendingData.length === 0) {
  console.log("Hàng chờ rỗng. Không có gì để duyệt.");
  process.exit(0);
}

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

const VALID_INTENTS = [
  'ticket_price_query', 'schedule_query', 'booking_guidance', 'mood_query',
  'bot_identity', 'promotion_query', 'movie_info_query', 'movie_ordinal_query',
  'security_violation', 'greeting', 'recommendation_prompt', 'top_rated_query',
  'movie_comparison_query', 'director_actor_query', 'upcoming_showtime_query',
  'contextual_pronoun_resolution'
];

let currentIndex = 0;
let modifiedTraining = false;

const processNext = () => {
  if (currentIndex >= pendingData.length) {
    console.log("\n==================================");
    console.log("Đã duyệt xong toàn bộ hàng chờ.");
    fs.writeFileSync(pendingFile, JSON.stringify(pendingData, null, 2));
    rl.close();
    
    if (modifiedTraining) {
      console.log("\n🚀 Phát hiện dữ liệu trainingData.js thay đổi. Đang tự động chạy npm run train...");
      const child = exec("npm run train", { cwd: process.cwd() });
      child.stdout.pipe(process.stdout);
      child.stderr.pipe(process.stderr);
      child.on('exit', (code) => {
        console.log(`\n✅ Quá trình train hoàn tất với mã thoát ${code}.`);
      });
    }
    return;
  }

  const item = pendingData[currentIndex];
  console.log(`\n[${currentIndex + 1}/${pendingData.length}] Câu hỏi: "${item.text}"`);
  console.log(`  Kỳ vọng ban đầu (Suggested): [${item.suggestedIntent}]`);
  console.log(`  Thực tế bot đoán (Actual):   [${item.actualIntent}]`);
  console.log(`  Số lần lặp lại lỗi (Occurrences): ${item.occurrences}`);
  
  rl.question("👉 Chọn hành động: [1] Dùng Kỳ vọng ban đầu, [2] Dùng Thực tế bot đoán, [3] Xóa (Delete), [Enter] Skip\n   Hoặc gõ tay tên intent khác: ", (answer) => {
    answer = answer.trim();
    if (answer === '1') {
      answer = item.suggestedIntent;
    } else if (answer === '2') {
      answer = item.actualIntent;
    }

    if (answer.toLowerCase() === 'skip' || answer === '') {
      console.log("⏩ Đã bỏ qua (giữ lại trong hàng chờ).");
      currentIndex++;
      processNext();
    } else if (answer.toLowerCase() === 'delete' || answer === '3') {
      console.log("🗑️ Đã xóa khỏi hàng chờ.");
      pendingData.splice(currentIndex, 1);
      // Do not increment currentIndex because we just removed the current item
      processNext();
    } else {
      if (!VALID_INTENTS.includes(answer)) {
        console.log(`⚠️ LỖI: Nhãn "${answer}" không tồn tại trong hệ thống!`);
        console.log(`💡 Các nhãn hợp lệ: ${VALID_INTENTS.join(', ')}`);
        console.log(`Vui lòng nhập lại nhãn đúng, hoặc gõ 'skip' / 'delete'.`);
        processNext();
        return;
      }

      // Append to trainingData.js
      const lines = fs.readFileSync(trainingFile, 'utf8').split('\n');
      const lastIndex = lines.findLastIndex(l => l.trim() === '];');
      if (lastIndex !== -1) {
        // Safe escape single quotes
        const safeText = item.text.replace(/'/g, "\\'");
        lines.splice(lastIndex, 0, `  { text: '${safeText}', intent: '${answer}' },`);
        fs.writeFileSync(trainingFile, lines.join('\n'));
        console.log(`✅ Đã thêm câu "${item.text}" với nhãn [${answer}] vào trainingData.js!`);
        modifiedTraining = true;
      } else {
        console.log("❌ Lỗi: Không tìm thấy dấu `];` kết thúc mảng rawTrainingSamples trong trainingData.js.");
      }
      
      // Remove from pending
      pendingData.splice(currentIndex, 1);
      processNext();
    }
  });
};

console.log(`Bắt đầu duyệt ${pendingData.length} câu nghi ngờ sai...`);
processNext();
