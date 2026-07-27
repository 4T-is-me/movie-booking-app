/**
 * Service phân loại ý định (Intent Classifier) cho chatbot rule-based.
 * Đã tích hợp Time Extractor & Typo-Tolerance (So khớp mờ Levenshtein + Subsequence + Rút gọn chữ lặp).
 */

import { moodDictionary } from '../config/moodDictionary.js';
import { detectSecurityViolation } from '../config/securityGuardrails.js';

// Các từ khóa chào hỏi
const greetingKeywords = [
  'xin chao',
  'chao bot',
  'chao ban',
  'halo',
  'helo',
  'alô',
  'alo',
  'hi bot',
  'hello'
];

// Các từ khóa nhận diện danh tính Bot AI
const botIdentityKeywords = [
  'ban co phai la ai khong',
  'ban la ai',
  'bot la ai',
  'phai la ai khong',
  'ban la ai vay',
  'ban la robot'
];

// Các từ khóa hướng dẫn đặt vé
const bookingGuidanceKeywords = [
  'dat ve',
  'huong dan dat ve',
  'cach dat ve',
  'mua ve',
  'dat ghe',
  'chon ghe',
  'chon rap',
  'cach mua ve',
  'mua ve xem phim'
];

// Các từ khóa hỏi giá vé
const ticketPriceKeywords = [
  'gia ve',
  'bao nhieu tien',
  'tien ve',
  'gia bao nhieu',
  've bao nhieu',
  'bang gia ve',
  'gia 1 ve'
];

// Các từ khóa hỏi khuyến mãi / ưu đãi
const promotionKeywords = [
  'khuyen mai',
  'voucher',
  'giam gia',
  'uu dai',
  'khuyen mai hom nay',
  'co km gi',
  'ma giam gia'
];

// Các từ khóa "sắp chiếu" — hỏi suất chiếu tính từ BÂY GIỜ trở đi theo rạp cụ thể
// Phải khai báo TRƯỚC scheduleKeywords để được ưu tiên nhận diện trước
const upcomingShowtimeKeywords = [
  // Hỏi trực tiếp về suất sắp tới
  'sap chieu',            // "sắp chiếu" — keyword cốt lõi, bắt mọi câu có "sắp chiếu"
  'gio con suat nao',
  'con suat nao khong',
  'con phim gi khong',
  'con phim sap chieu',
  'phim sap chieu',
  'suat chieu sap toi',
  'sap co phim gi',
  // Hỏi theo thời gian hiện tại
  'bay gio co phim gi',
  'bay gio co suat nao',
  'gio nay co phim gi',
  'luc nay co phim gi',
  'hien tai con phim gi',
  'hien co suat nao',
  // Hỏi ngắn dạng "còn suất nào"
  'con suat chieu nao',
  'con suat gi khong',
  'con phim nao khong'
];

// Các từ khóa tường minh yêu cầu suất chiếu/lịch chiếu
const explicitScheduleKeywords = [
  'lich chieu',
  'suat chieu',
  'suat chieu phim',
  'co suat chieu',
  'lich chieu hom nay',
  'suat chieu hom nay',
  'phim dang chieu',
  'phim dang hien',
  'lich chieu ngay mai',
  'suat chieu ngay mai',
  'lich chieu cuoi tuan'
];

// Các từ khóa linh hoạt cho schedule_query
const scheduleKeywords = [
  ...explicitScheduleKeywords,
  'co phim gi',
  'phim gi',
  'hom nay co gi',
  'hom nay co gi khong',
  'hom nay co gi hay',
  'co gi khong',
  'xem phim gi hom nay',
  'phim hot hom nay'
];

// Từ khóa liên quan đến tra cứu thông tin phim
const movieInfoKeywords = [
  'thong tin phim',
  'chi tiet phim',
  'noi dung phim',
  'dien vien phim',
  'dao dien phim',
  'thoi luong phim',
  'xem thong tin',
  'review phim',
  'danh gia phim'
];

// Các từ khóa ngoài chủ đề (off-topic) phổ biến
const offTopicKeywords = [
  'thoi tiet',
  'tin tuc',
  'the thao',
  'gia vang',
  'ti gia',
  'an gi',
  'choi gi',
  'may gio',
  'hom nay thu may',
  '1 voi 1',
  '1+1',
  'bang may',
  'bang bao nhieu',
  'ke chuyen',
  'chuyen cuoi',
  'hat cho',
  'hat bai'
];

/**
 * Tính khoảng cách Levenshtein giữa 2 chuỗi
 */
export const getLevenshteinDistance = (a = '', b = '') => {
  const m = a.length;
  const n = b.length;
  if (m === 0) return n;
  if (n === 0) return m;

  const dp = Array.from({ length: m + 1 }, () => new Array(n + 1).fill(0));

  for (let i = 0; i <= m; i++) dp[i][0] = i;
  for (let j = 0; j <= n; j++) dp[0][j] = j;

  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      dp[i][j] = Math.min(
        dp[i - 1][j] + 1,
        dp[i][j - 1] + 1,
        dp[i - 1][j - 1] + cost
      );
    }
  }
  return dp[m][n];
};

/**
 * So khớp mờ 1 token đầu vào với 1 token từ khóa chuẩn (Fuzzy & Prefix Matching)
 */
export const isFuzzyTokenMatch = (inputToken = '', targetToken = '') => {
  if (!inputToken || !targetToken) return false;
  if (inputToken === targetToken) return true;

  // Rút gọn ký tự lặp để so sánh (vd: "hoomm" -> "hom", "phimmm" -> "phim")
  const collapsedInput = inputToken.replace(/(.)\1+/g, '$1');
  const collapsedTarget = targetToken.replace(/(.)\1+/g, '$1');
  if (collapsedInput === collapsedTarget) return true;

  // Lọc hư từ tiếng Việt phổ biến để tránh false-positives khi fuzzy match với từ khác
  const stopWords = ['co', 'la', 'vi', 'do', 'ma', 'ra', 'ba', 'di', 'de', 'se', 'cho', 'va', 'gi', 'ten', 'bo', 'mat', 'vay', 'nhieu', 'bao', 'hay', 'luon', 'muon', 'chay', 'chua', 'roi', 've'];
  if ((stopWords.includes(inputToken) || stopWords.includes(targetToken)) && inputToken !== targetToken) {
    return false;
  }

  // 1. Khoảng cách Levenshtein (cho phép sai lệch tối đa 25% độ dài từ khóa)
  const dist = getLevenshteinDistance(inputToken, targetToken);
  const maxAllowedDist = Math.max(1, Math.floor(targetToken.length * 0.25));
  if (dist <= maxAllowedDist && Math.abs(inputToken.length - targetToken.length) <= 2) {
    return true;
  }

  // 2. Prefix matching cho từ gõ rớt ký tự
  const minPrefixLen = targetToken.length >= 4 ? 2 : 1;
  if (inputToken.length >= minPrefixLen && targetToken.startsWith(inputToken)) {
    return true;
  }

  return false;
};

/**
 * So khớp mờ n-gram cửa sổ cụm từ khóa (Phrase Window Matching)
 */
export const isPhraseFuzzyMatch = (inputWords = [], kwTokens = []) => {
  if (!inputWords.length || !kwTokens.length) return false;
  const n = kwTokens.length;
  for (let i = 0; i <= inputWords.length - n; i++) {
    let windowMatch = true;
    for (let j = 0; j < n; j++) {
      if (!isFuzzyTokenMatch(inputWords[i + j], kwTokens[j])) {
        windowMatch = false;
        break;
      }
    }
    if (windowMatch) return true;
  }
  return false;
};

/**
 * Trích xuất ngày đối chiếu từ các từ mốc thời gian tương đối
 * @param {string} normalizedMsg - Tin nhắn không dấu
 * @returns {{targetDate: Date, dateTitle: string}}
 */
export const extractRelativeDate = (normalizedMsg = '') => {
  const now = new Date();
  let targetDate = new Date(now);
  let dateTitle = 'hôm nay';

  if (normalizedMsg.includes('ngay mai') || normalizedMsg.includes('mai')) {
    targetDate.setDate(now.getDate() + 1);
    dateTitle = 'ngày mai';
  } else if (normalizedMsg.includes('ngay kia')) {
    targetDate.setDate(now.getDate() + 2);
    dateTitle = 'ngày kia';
  } else if (normalizedMsg.includes('cuoi tuan')) {
    const currentDay = now.getDay();
    const daysUntilSaturday = (6 - currentDay + 7) % 7;
    targetDate.setDate(now.getDate() + daysUntilSaturday);
    dateTitle = 'cuối tuần';
  } else if (normalizedMsg.includes('thu 2') || normalizedMsg.includes('thu hai')) {
    const days = (1 - now.getDay() + 7) % 7;
    targetDate.setDate(now.getDate() + (days === 0 ? 7 : days));
    dateTitle = 'Thứ 2';
  } else if (normalizedMsg.includes('thu 3') || normalizedMsg.includes('thu ba')) {
    const days = (2 - now.getDay() + 7) % 7;
    targetDate.setDate(now.getDate() + (days === 0 ? 7 : days));
    dateTitle = 'Thứ 3';
  } else if (normalizedMsg.includes('thu 4') || normalizedMsg.includes('thu tu')) {
    const days = (3 - now.getDay() + 7) % 7;
    targetDate.setDate(now.getDate() + (days === 0 ? 7 : days));
    dateTitle = 'Thứ 4';
  } else if (normalizedMsg.includes('thu 5') || normalizedMsg.includes('thu nam')) {
    const days = (4 - now.getDay() + 7) % 7;
    targetDate.setDate(now.getDate() + (days === 0 ? 7 : days));
    dateTitle = 'Thứ 5';
  } else if (normalizedMsg.includes('thu 6') || normalizedMsg.includes('thu sau')) {
    const days = (5 - now.getDay() + 7) % 7;
    targetDate.setDate(now.getDate() + (days === 0 ? 7 : days));
    dateTitle = 'Thứ 6';
  } else if (normalizedMsg.includes('thu 7') || normalizedMsg.includes('thu bay')) {
    const days = (6 - now.getDay() + 7) % 7;
    targetDate.setDate(now.getDate() + (days === 0 ? 7 : days));
    dateTitle = 'Thứ 7';
  } else if (normalizedMsg.includes('chu nhat')) {
    const days = (0 - now.getDay() + 7) % 7;
    targetDate.setDate(now.getDate() + (days === 0 ? 7 : days));
    dateTitle = 'Chủ Nhật';
  }

  return { targetDate, dateTitle };
};

/**
 * Kiểm tra xem câu hỏi có phải là câu hỏi lịch chiếu hay không (Hỗ trợ So khớp mờ & Typo)
 * @param {string} normalizedMsg - Tin nhắn đã chuẩn hóa không dấu
 * @returns {boolean}
 */
const isScheduleQuery = (normalizedMsg) => {
  // 1. So khớp trực tiếp chuỗi từ khóa schedule
  for (const kw of scheduleKeywords) {
    if (normalizedMsg.includes(kw)) {
      return true;
    }
  }

  const inputWords = normalizedMsg.split(/\s+/).filter(Boolean);

  // 2. So khớp mờ cửa sổ n-gram theo từng cụm từ khóa schedule
  for (const kw of scheduleKeywords) {
    const kwTokens = kw.split(/\s+/);
    if (isPhraseFuzzyMatch(inputWords, kwTokens)) {
      return true;
    }
  }

  // 3. Tách từ và so khớp đồng thời Time Token + Movie/Schedule Token
  const timeTokens = ['hom', 'nay', 'mai', 'kia', 'tuan', 'hnay', 'hday'];
  const scheduleMovieTokens = ['phim', 'phmi', 'phg', 'lich', 'suat', 'chieu'];

  let hasTimeMatch = false;
  let hasScheduleMovieMatch = false;

  for (const w of inputWords) {
    if (!hasTimeMatch) {
      for (const t of timeTokens) {
        if (isFuzzyTokenMatch(w, t)) {
          hasTimeMatch = true;
          break;
        }
      }
    }
    if (!hasScheduleMovieMatch) {
      for (const m of scheduleMovieTokens) {
        if (isFuzzyTokenMatch(w, m)) {
          hasScheduleMovieMatch = true;
          break;
        }
      }
    }
  }

  if (hasTimeMatch && hasScheduleMovieMatch) {
    return true;
  }

  return false;
};

// Các từ khóa đề xuất tư vấn chung khi người dùng phân vân
const recommendationPromptKeywords = [
  'khong biet xem gi',
  'chua biet xem gi',
  'tu van giup',
  'goi y phim hay',
  'nen xem phim gi',
  'xem phim gi hay',
  'tu van phim',
  'goi y gi khong',
  'chua biet xem phim gi',
  'khong biet muon xem gi'
];

// Các từ khóa hỏi top rating / hay nhất
const topRatedKeywords = [
  'rating cao nhat',
  'danh gia cao nhat',
  'phim hay nhat',
  'diem cao nhat',
  'top phim',
  'hay nhat rap',
  'phim nao hay nhat',
  'phim nao rating cao nhat'
];

// Các từ khóa so sánh phim
const movieComparisonKeywords = [
  'hay hon',
  'so sanh',
  'nen xem phim nao',
  'giua phim',
  'phim nao hay hon',
  'phim nao tot hon'
];

// Các từ khóa tìm kiếm theo Đạo diễn / Diễn viên
const directorActorKeywords = [
  'dao dien',
  'dien vien',
  'dien vien dong',
  'phim cua dao dien',
  'phim co dien vien'
];

/**
 * Trích xuất thứ tự số phim từ câu hỏi ordinal (vd: "phim thứ 2", "phim số 1", "phim đầu tiên")
 * @param {string} normalizedMsg - Tin nhắn đã chuẩn hóa
 * @returns {number|null} 1-based index (vd: 1, 2, 3...) hoặc null
 */
export const extractMovieOrdinalIndex = (normalizedMsg = '') => {
  const norm = normalizedMsg.trim().toLowerCase();

  // Pattern dạng: "phim so 2", "phim thu 2", "so 2", "phim 2"
  const numMatch = norm.match(/(?:phim|so|thu|movie)\s*(?:so|thu)?\s*(\d+)/i) || norm.match(/^(\d+)$/);
  if (numMatch) {
    const val = parseInt(numMatch[1], 10);
    if (val >= 1 && val <= 10) return val;
  }

  if (norm.includes('dau tien') || norm.includes('thu nhat') || norm.includes('so 1')) return 1;
  if (norm.includes('thu hai') || norm.includes('so 2')) return 2;
  if (norm.includes('thu ba') || norm.includes('so 3')) return 3;
  if (norm.includes('thu tu') || norm.includes('so 4')) return 4;
  if (norm.includes('thu nam') || norm.includes('so 5')) return 5;

  return null;
};

/**
 * Kiểm tra xem câu hỏi có chứa từ khóa gợi ý theo tâm trạng hay không (Hỗ trợ Fuzzy Matching & Negation Phủ Định)
 * @param {string} normalizedMsg - Tin nhắn đã chuẩn hóa không dấu
 * @returns {{matchedMoods: string[], negatedMoods: string[]}}
 */
const findMatchedMoods = (normalizedMsg) => {
  const matchedMoods = [];
  const negatedMoods = [];
  const inputWords = normalizedMsg.split(/\s+/).filter(Boolean);

  const negationWords = ['khong', 'dung', 'tranh', 'khong thich', 'khong muon', 'khong xem', 'ghet', 'chua thich'];

  const hasNegationBefore = (kw) => {
    const idx = normalizedMsg.indexOf(kw);
    if (idx <= 0) return false;
    const prefixText = normalizedMsg.substring(Math.max(0, idx - 35), idx).trim();
    return negationWords.some(neg => prefixText.endsWith(neg) || prefixText.includes(neg));
  };

  for (const [moodKey, moodData] of Object.entries(moodDictionary)) {
    for (const kw of moodData.keywords) {
      let isMatched = false;

      // 1. Exact boundary match
      const regex = new RegExp(`(?:^|\\s)${kw}(?:$|\\s)`, 'i');
      if (regex.test(normalizedMsg)) {
        isMatched = true;
      } else {
        // 2. Phrase fuzzy match
        const kwTokens = kw.split(/\s+/);
        if (kw.length >= 4 && isPhraseFuzzyMatch(inputWords, kwTokens)) {
          isMatched = true;
        }
      }

      if (isMatched) {
        if (hasNegationBefore(kw)) {
          if (!negatedMoods.includes(moodKey)) negatedMoods.push(moodKey);
        } else {
          if (!matchedMoods.includes(moodKey)) matchedMoods.push(moodKey);
        }
        break;
      }
    }
  }

  return { matchedMoods, negatedMoods };
};

/**
 * Trích xuất tên phim từ tin nhắn movie_info_query
 * @param {string} rawMsg - Tin nhắn gốc
 * @param {string} normalizedMsg - Tin nhắn đã chuẩn hóa
 * @returns {string} - Tên/từ khóa phim trích xuất được
 */
const extractMovieSearchQuery = (rawMsg, normalizedMsg) => {
  let extracted = normalizedMsg;

  for (const kw of movieInfoKeywords) {
    if (extracted.includes(kw)) {
      extracted = extracted.replace(kw, '');
    }
  }

  extracted = extracted
    .replace(/^(cho (toi|minh) (xem|hoi|biet)|muon (xem|biet)|cho hoi|xem|tin|phim)\s+/i, '')
    .replace(/\s+(the nao|nhu the nao|nhu nao|la gi|ra sao)$/i, '')
    .trim();

  return extracted || normalizedMsg;
};

/**
 * Phân loại ý định của tin nhắn
 * @param {string} normalizedMsg - Tin nhắn đã chuẩn hóa không dấu
 * @param {string} rawMsg - Tin nhắn gốc của người dùng
 * @returns {Object} - Object chứa intent, matchedMoods (nếu có), extractedQuery (nếu có), targetDate (nếu có)
 */
export const classifyIntent = (normalizedMsg, rawMsg = '') => {
  if (!normalizedMsg && !rawMsg) {
    return { intent: 'fallback', confidence: 0 };
  }

  // 0. BẢO VỆ AN TOÀN CAO NHẤT (SECURITY GUARDRAIL)
  if (detectSecurityViolation(normalizedMsg, rawMsg)) {
    return {
      intent: 'security_violation',
      confidence: 1.0
    };
  }

  // 0.5. TỪ KHÓA NGOÀI CHỦ ĐỀ (OFF-TOPIC) -> Ngắt sớm để không bị nhầm intent
  const isOffTopic = offTopicKeywords.some(kw => normalizedMsg.includes(kw));
  if (isOffTopic) {
    return {
      intent: 'fallback',
      confidence: 0.1
    };
  }

  // 0.6. Tra cứu theo Ordinal Index ("phim thứ 2", "phim số 1", "phim đầu tiên")
  const ordinalIndex = extractMovieOrdinalIndex(normalizedMsg);
  // Chỉ nhận diện ordinal khi câu chứa từ khóa "phim", "xem", "suat", "cho" hoặc chỉ có số đơn lẻ
  if (ordinalIndex !== null && (normalizedMsg.includes('phim') || normalizedMsg.includes('xem') || normalizedMsg.includes('suat') || normalizedMsg.includes('thu') || normalizedMsg.includes('so') || /^\d+$/.test(normalizedMsg.trim()))) {
    return {
      intent: 'movie_ordinal_query',
      ordinalIndex,
      confidence: 0.95
    };
  }

  // 0.7. Tư vấn khi người dùng không biết xem gì (Recommendation Prompt)
  const isRecommendationPrompt = recommendationPromptKeywords.some(kw => normalizedMsg.includes(kw));
  if (isRecommendationPrompt) {
    return {
      intent: 'recommendation_prompt',
      confidence: 0.95
    };
  }

  // 0.8. Tra cứu Rating cao nhất (Top Rated Query)
  const isTopRated = topRatedKeywords.some(kw => normalizedMsg.includes(kw));
  if (isTopRated) {
    return {
      intent: 'top_rated_query',
      confidence: 0.95
    };
  }

  // 0.9. So sánh hai bộ phim (Movie Comparison Query)
  const isMovieComparison = movieComparisonKeywords.some(kw => normalizedMsg.includes(kw));
  if (isMovieComparison) {
    return {
      intent: 'movie_comparison_query',
      confidence: 0.9
    };
  }

  // 1. Chào hỏi (Greeting - Chỉ khớp khi câu ngắn <= 25 ký tự)
  const isGreeting = greetingKeywords.some(kw => normalizedMsg === kw || (normalizedMsg.startsWith(kw + ' ') && normalizedMsg.length <= 25));
  if (isGreeting) {
    return {
      intent: 'greeting',
      confidence: 0.95
    };
  }

  // 1.2. Nhận diện danh tính Bot (Bot Identity)
  const isBotIdentity = botIdentityKeywords.some(kw => normalizedMsg.includes(kw));
  if (isBotIdentity) {
    return {
      intent: 'bot_identity',
      confidence: 0.95
    };
  }

  // 1.5. Hướng dẫn đặt vé (Booking Guidance)
  const isBookingGuidance = bookingGuidanceKeywords.some(kw => normalizedMsg.includes(kw));
  if (isBookingGuidance) {
    return {
      intent: 'booking_guidance',
      confidence: 0.95
    };
  }

  // 1.6. Tra cứu theo Đạo diễn / Diễn viên
  const isDirectorActor = directorActorKeywords.some(kw => normalizedMsg.includes(kw));
  if (isDirectorActor) {
    return {
      intent: 'director_actor_query',
      confidence: 0.9
    };
  }

  // 1.8. Movie info query (Ưu tiên cao nhất khi có từ khóa tra cứu thông tin phim rõ ràng)
  const hasInfoKeyword = movieInfoKeywords.some(kw => normalizedMsg.includes(kw));
  if (hasInfoKeyword) {
    const extractedQuery = extractMovieSearchQuery(rawMsg, normalizedMsg);
    return {
      intent: 'movie_info_query',
      searchQuery: extractedQuery,
      confidence: 0.9
    };
  }

  // 2. Upcoming Showtime query (Hỏi phim sắp chiếu từ BÂY GIỜ theo rạp — ưu tiên TRƯỚC schedule_query thông thường)
  // Guard: Nếu câu có từ mốc ngày rõ ràng (ngày mai, cuối tuần...) hoặc từ "lich chieu" → KHÔNG phải upcoming, mà là schedule
  const hasExplicitDateRef = ['ngay mai', 'ngay kia', 'cuoi tuan', 'thu 2', 'thu 3', 'thu 4', 'thu 5', 'thu 6', 'thu 7', 'chu nhat', 'lich chieu']
    .some(kw => normalizedMsg.includes(kw));
  const isUpcomingShowtime = !hasExplicitDateRef && upcomingShowtimeKeywords.some(kw => normalizedMsg.includes(kw));
  if (isUpcomingShowtime) {
    return {
      intent: 'upcoming_showtime_query',
      confidence: 0.95
    };
  }

  // 2.5. Explicit Schedule query (Nếu câu chứa từ khóa yêu cầu lịch chiếu/suất chiếu tường minh)
  const hasExplicitSchedule = explicitScheduleKeywords.some(kw => normalizedMsg.includes(kw));
  if (hasExplicitSchedule) {
    const { targetDate, dateTitle } = extractRelativeDate(normalizedMsg);
    return {
      intent: 'schedule_query',
      targetDate,
      dateTitle,
      confidence: 0.95
    };
  }

  // 3. Mood query (Gợi ý theo tâm trạng / thể loại + hỗ trợ Negation Phủ Định)
  const { matchedMoods, negatedMoods } = findMatchedMoods(normalizedMsg);
  if (matchedMoods.length > 0 || negatedMoods.length > 0) {
    return {
      intent: 'mood_query',
      matchedMoods,
      negatedMoods,
      confidence: 0.9
    };
  }

  // 3.5. Flexible Schedule query (Các câu tra cứu lịch chiếu ngắn linh hoạt: "hôm nay có gì", "còn suất nào")
  if (isScheduleQuery(normalizedMsg)) {
    const { targetDate, dateTitle } = extractRelativeDate(normalizedMsg);
    return {
      intent: 'schedule_query',
      targetDate,
      dateTitle,
      confidence: 0.85
    };
  }

  // 4. Hỏi giá vé (Ticket Price Query - Hỗ trợ So khớp mờ Fuzzy)
  const inputWords = normalizedMsg.split(/\s+/).filter(Boolean);
  const isTicketPrice = ticketPriceKeywords.some(kw => {
    if (normalizedMsg.includes(kw)) return true;
    const kwTokens = kw.split(/\s+/);
    return isPhraseFuzzyMatch(inputWords, kwTokens);
  });
  if (isTicketPrice) {
    return {
      intent: 'ticket_price_query',
      confidence: 0.9
    };
  }

  // 5. Hỏi khuyến mãi (Promotion Query - Hỗ trợ So khớp mờ Fuzzy)
  const isPromotion = promotionKeywords.some(kw => {
    if (normalizedMsg.includes(kw)) return true;
    const kwTokens = kw.split(/\s+/);
    return isPhraseFuzzyMatch(inputWords, kwTokens);
  });
  if (isPromotion) {
    return {
      intent: 'promotion_query',
      confidence: 0.9
    };
  }

  // 6. Nếu tin nhắn ngắn (từ 3 đến 40 ký tự) không chứa từ khóa ngoài chủ đề -> có thể là tên phim
  if (normalizedMsg.length >= 3 && normalizedMsg.length <= 40 && !['hello', 'xin chao', 'hi', 'alo', 'cam on', 'thanks'].includes(normalizedMsg)) {
    return {
      intent: 'movie_info_query_or_fallback',
      searchQuery: normalizedMsg,
      confidence: 0.5
    };
  }

  // 7. Fallback mặc định
  return {
    intent: 'fallback',
    confidence: 0.3
  };
};
