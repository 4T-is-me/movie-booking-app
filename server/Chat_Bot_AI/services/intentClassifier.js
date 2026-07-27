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
  'gia 1 ve',
  'gia ve bao nhieu',
  've gia bao nhieu'
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
  'danh gia phim',
  'cho toi hoi phim',
  'cho minh hoi phim',
  'cho toi hoi ve phim',
  'cho minh hoi ve phim',
  'cho hoi phim',
  'cho toi xem phim',
  'thong tin ve phim',
  'cho toi hoi',
  'toi hoi phim',
  'khong phai phim'
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
 * Trích xuất thứ tự số phim từ câu hỏi ordinal (vd: "phim thứ 2", "phim số 1", "phim đầu tiên", "phim cuối cùng")
 * @param {string} normalizedMsg - Tin nhắn đã chuẩn hóa
 * @returns {number|null} 1-based index (vd: 1, 2, 3...) hoặc -1 cho phim cuối cùng, hoặc null
 */
export const extractMovieOrdinalIndex = (normalizedMsg = '') => {
  const norm = normalizedMsg.trim().toLowerCase();

  // Pattern dạng: "phim so 2", "phim thu 2", "so 2", "phim 2"
  const numMatch = norm.match(/(?:phim|so|thu|movie)\s*(?:so|thu)?\s*(\d+)/i) || norm.match(/^(\d+)$/);
  if (numMatch) {
    const val = parseInt(numMatch[1], 10);
    if (val >= 1 && val <= 10) return val;
  }

  if (/\b(dau tien|thu nhat|so 1|phim dau)\b/i.test(norm)) return 1;
  if (/\b(thu hai|so 2|phim hai)\b/i.test(norm)) return 2;
  if (/\b(thu ba|so 3|phim ba)\b/i.test(norm)) return 3;
  if (/\b(thu tu|so 4|phim tu)\b/i.test(norm)) return 4;
  if (/\b(thu nam|so 5|phim nam)\b/i.test(norm)) return 5;
  if (/\b(cuoi cung|phim cuoi)\b/i.test(norm)) return -1; // -1 đại diện cho phim cuối cùng

  return null;
};

/**
 * Nhận diện Sub-Intent cho câu hỏi tiếp nối (Follow-up Questions)
 * @param {string} normalizedMsg - Tin nhắn đã chuẩn hóa
 * @returns {'showtime'|'ticket_price'|'director'|'actor'|'director_actor'|'info'}
 */
export const detectContextualSubIntent = (normalizedMsg = '') => {
  const norm = normalizedMsg.toLowerCase();

  // 1. Hỏi suất chiếu / lịch chiếu / giờ chiếu / chiếu ở đâu
  if (['chieu', 'suat', 'may gio', 'khi nao', 'lich chieu', 'rap nao', 'chieu o dau', 'gio chieu', 'moi gio'].some(kw => norm.includes(kw))) {
    return 'showtime';
  }

  // 2. Hỏi giá vé
  if (['gia ve', 'bao nhieu tien', 've bao nhieu', 'tien ve', 'gia bao nhieu', 've'].some(kw => norm.includes(kw))) {
    return 'ticket_price';
  }

  // 3. Hỏi Đạo diễn
  if (['dao dien', 'ai lam dao dien'].some(kw => norm.includes(kw))) {
    return 'director';
  }

  // 4. Hỏi Diễn viên
  if (['dien vien', 'ai dong', 'dien vien nao', 'dien vien gi', 'co nhung dien vien nao'].some(kw => norm.includes(kw))) {
    return 'actor';
  }

  // 5. Hỏi Thể loại / Thời lượng / Nội dung / Chi tiết (Mặc định)
  return 'info';
};

/**
 * Kiểm tra xem tin nhắn có phải là câu hỏi thuộc tính tiếp nối ẩn (implicit follow-up) về bộ phim đang có trong context hay không
 * @param {string} normalizedMsg - Tin nhắn đã chuẩn hóa
 * @param {Array} lastMovies - Danh sách phim trong session context
 * @returns {boolean}
 */
export const isImplicitContextualFollowUp = (normalizedMsg = '', lastMovies = []) => {
  if (!lastMovies || lastMovies.length === 0) return false;

  const norm = normalizedMsg.toLowerCase().trim();

  // Danh sách các mẫu câu hỏi thuộc tính ngắn tiếp nối
  const implicitPatterns = [
    'co dien vien nao', 'co nhung dien vien nao', 'dien vien la ai', 'ai dong', 'ai la dien vien', 'dien vien nao',
    'dao dien la ai', 'ai lam dao dien', 'ai la dao dien', 'dao dien nao',
    'thoi luong bao nhieu', 'bao nhieu phut', 'dai bao nhieu phut', 'dai bao nhieu',
    'the loai gi', 'thuoc the loai gi', 'the loai nao',
    'noi dung gi', 'tom tat noi dung', 'noi dung nhu the nao', 'noi dung ra sao',
    'gia ve bao nhieu', 've bao nhieu tien', 'tien ve bao nhieu',
    'chieu may gio', 'may gio chieu', 'suat chieu may gio', 'khi nao chieu', 'chieu o dau'
  ];

  // 1. Nếu tin nhắn khớp trực tiếp với các mẫu câu hỏi tiếp nối ngắn
  if (implicitPatterns.some(p => norm === p || norm.includes(p))) {
    return true;
  }

  // 2. Nếu câu ngắn (dưới 35 ký tự) và hỏi chung về "dien vien" hoặc "dao dien" mà KHÔNG chứa tên người riêng rõ ràng
  if (norm.length <= 35 && (norm.includes('dien vien') || norm.includes('dao dien'))) {
    if (['ai', 'nao', 'gi', 'co', 'nhung'].some(w => norm.includes(w))) {
      return true;
    }
  }

  return false;
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

  // Xóa cụm phủ định ban đầu (vd: "khong phai phim A, toi hoi phim B co")
  extracted = extracted.replace(/^khong phai (phim\s+)?[a-z0-9\s]+\s*(thay|khong|ma|voi|nhung|chuyen sang|toi hoi|toi muon|xem|co)\s*/ig, '');

  for (const kw of movieInfoKeywords) {
    if (extracted.includes(kw)) {
      extracted = extracted.replace(kw, '');
    }
  }

  extracted = extracted
    .replace(/^(gio thi|bay gio thi|gio cho (toi|minh)|bay gio|gio|a thoi|thoi|a|cho (toi|minh) (hoi|biet|xem)|muon (xem|biet)|cho hoi|xem|tin|phim|ve phim|hoi ve phim|toi hoi phim)\s+/ig, '')
    .replace(/\s+(thi sao|the nao|nhu the nao|nhu nao|la gi|ra sao|vay|nhe|ha|a|co)$/ig, '')
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

  // 0.1. Hỏi giá vé (Ticket Price Query)
  const hasExplicitScheduleKw = ['lich chieu', 'suat chieu', 'phim dang chieu'].some(kw => normalizedMsg.includes(kw));
  if (!hasExplicitScheduleKw) {
    const inputWords = normalizedMsg.split(/\s+/).filter(Boolean);
    const isTicketPrice = ticketPriceKeywords.some(kw => {
      if (normalizedMsg.includes(kw)) return true;
      const kwTokens = kw.split(/\s+/);
      return isPhraseFuzzyMatch(inputWords, kwTokens);
    });
    if (isTicketPrice) {
      return {
        intent: 'ticket_price_query',
        confidence: 0.95
      };
    }
  }

  // 0.5. TỪ KHÓA NGOÀI CHỦ ĐỀ (OFF-TOPIC) -> Ngắt sớm trừ khi có từ khóa thuộc ngữ cảnh điện ảnh
  const hasMovieDomainContext = ['phim', 'suat', 'chieu', 'rap', 've', 'actor', 'director', 'dao dien', 'dien vien', 'gia ve', 'thoi luong'].some(kw => normalizedMsg.includes(kw));
  const isOffTopic = !hasMovieDomainContext && offTopicKeywords.some(kw => normalizedMsg.includes(kw));
  if (isOffTopic) {
    return {
      intent: 'fallback',
      confidence: 0.1
    };
  }

  // 0.6. Tra cứu theo Ordinal Index ("phim thứ 2", "phim số 1", "phim đầu tiên", "phim cuối cùng")
  const ordinalIndex = extractMovieOrdinalIndex(normalizedMsg);
  if (ordinalIndex !== null && (normalizedMsg.includes('phim') || normalizedMsg.includes('xem') || normalizedMsg.includes('suat') || normalizedMsg.includes('thu') || normalizedMsg.includes('so') || normalizedMsg.includes('dau') || normalizedMsg.includes('cuoi') || /^\d+$/.test(normalizedMsg.trim()))) {
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

  // 5. Hỏi khuyến mãi (Promotion Query - Hỗ trợ So khớp mờ Fuzzy)
  const promoInputWords = normalizedMsg.split(/\s+/).filter(Boolean);
  const isPromotion = promotionKeywords.some(kw => {
    if (normalizedMsg.includes(kw)) return true;
    const kwTokens = kw.split(/\s+/);
    return isPhraseFuzzyMatch(promoInputWords, kwTokens);
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
