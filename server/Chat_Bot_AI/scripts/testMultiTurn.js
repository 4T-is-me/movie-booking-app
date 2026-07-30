import fetch from 'node-fetch';

const API_URL = 'http://localhost:5000/api/chat';

async function sendChat(message, sessionId) {
  try {
    const res = await fetch(API_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message, sessionId })
    });
    const data = await res.json();
    return data;
  } catch (error) {
    console.error('Lỗi khi gọi API:', error.message);
    return null;
  }
}

async function runScenario(scenarioName, messages) {
  console.log(`\n======================================================`);
  console.log(`🎭 BẮT ĐẦU KỊCH BẢN: ${scenarioName}`);
  console.log(`======================================================`);
  
  const sessionId = `test-scenario-${Date.now()}-${Math.random().toString(36).substring(7)}`;
  
  for (let i = 0; i < messages.length; i++) {
    const msg = messages[i];
    console.log(`\n👤 Lượt ${i + 1} - User: "${msg}"`);
    
    const response = await sendChat(msg, sessionId);
    
    if (response) {
      console.log(`🤖 Bot Intent: [${response.matchedIntent}]`);
      console.log(`🤖 Bot Trả lời:\n${response.reply}`);
    }
    
    // Đợi 1 giây giữa các lượt chat cho thực tế
    await new Promise(r => setTimeout(r, 1000));
  }
}

async function runTests() {
  // Kịch bản A: Hỏi phim hôm nay -> Hỏi phim thứ 1
  await runScenario("Kịch bản A (Context: movie_ordinal_query)", [
    "hôm nay rạp chiếu phim gì",
    "phim thứ 1 đó chiếu mấy giờ"
  ]);

  // Kịch bản B: Hỏi tư vấn -> Hỏi giá vé phim đó
  await runScenario("Kịch bản B (Context: contextual_pronoun_resolution - 'phim đó')", [
    "đang buồn muốn xem phim gì",
    "vậy phim đó giá vé bao nhiêu"
  ]);

  // Kịch bản C: Đổi chủ đề đột ngột (Ngữ nghĩa độc lập)
  await runScenario("Kịch bản C (Đổi chủ đề đột ngột, không dính context)", [
    "cho tui coi giá vé",
    "còn khuyến mãi gì không"
  ]);

  console.log(`\n✅ Đã hoàn tất các kịch bản test!`);
}

runTests();
