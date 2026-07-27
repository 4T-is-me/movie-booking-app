/**
 * Service chuẩn hóa văn bản tiếng Việt cho chatbot.
 * Mở rộng từ điển hỗ trợ teencode, từ viết tắt, gõ lặp ký tự và câu dính liền.
 */

// Bảng thay thế các từ viết tắt / teencode phổ biến
const abbreviationMap = {
  'k': 'khong',
  'ko': 'khong',
  'khg': 'khong',
  'kg': 'khong',
  'kno': 'khong',
  'vs': 'voi',
  'dc': 'duoc',
  'dk': 'duoc',
  'bt': 'biet',
  'bik': 'biet',
  'bjet': 'biet',
  'film': 'phim',
  'fim': 'phim',
  'feem': 'phim',
  'movie': 'phim',
  'mov': 'phim',
  'phmi': 'phim',
  'phg': 'phim',
  'phimm': 'phim',
  'hoomm': 'hom',
  'nya': 'nay',
  'naof': 'nao',
  'suad': 'suat',
  'soat': 'suat',
  'thik': 'thich',
  'thiq': 'thich',
  'tich': 'thich',
  'mun': 'muon',
  'moin': 'muon',
  'tui': 'toi',
  'h': 'gio',
  'mn': 'moi nguoi',
  'mik': 'minh',
  'm': 'minh',
  'ad': 'admin',
  'tk': 'tai khoan',
  's': 'sao',
  'z': 'vay',
  'zay': 'vay',
  'v': 'vay',
  'dt': 'dien thoai',
  'gja': 'gia',
  'km': 'khuyen mai',
  'vc': 'voucher',
  'ud': 'uu dai',
  'hnay': 'hom nay',
  'hday': 'hom nay'
};

// Từ điển token phục vụ tách từ cho các câu dính liền không khoảng trắng
const dictionaryTokens = [
  'hom', 'nay', 'ngay', 'mai', 'kia', 'cuoi', 'tuan',
  'co', 'phim', 'phg', 'phmi', 'gi', 'khong', 'gia',
  've', 'bao', 'nhieu', 'tien', 'khuyen', 'mai', 'uu',
  'dai', 'voucher', 'muon', 'xem', 'thich', 'lich',
  'chieu', 'suat', 'thoi', 'tiet', 'rap', 'buon',
  'nhe', 'nhang', 'hoi', 'hop', 'vui', 've', 'hanh',
  'dong', 'hai', 'kinh', 'di', 'dat', 'giam'
];

/**
 * Loại bỏ dấu tiếng Việt khỏi chuỗi văn bản
 * @param {string} str - Chuỗi ban đầu
 * @returns {string} - Chuỗi không dấu
 */
export const removeVietnameseTones = (str) => {
  if (!str) return '';
  let result = str.replace(/[\u200B-\u200D\uFEFF\u00A0]/g, '');
  result = result.replace(/à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ/g, 'a');
  result = result.replace(/è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ/g, 'e');
  result = result.replace(/ì|í|ị|ỉ|ĩ/g, 'i');
  result = result.replace(/ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ/g, 'o');
  result = result.replace(/ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ/g, 'u');
  result = result.replace(/ỳ|ý|ỵ|ỷ|ỹ/g, 'y');
  result = result.replace(/đ/g, 'd');
  result = result.replace(/À|Á|Ạ|Ả|Ã|Â|Ầ|Ấ|Ậ|Ẩ|Ẫ|Ă|Ằ|Ắ|Ặ|Ẳ|Ẵ/g, 'A');
  result = result.replace(/È|É|Ẹ|Ẻ|Ẽ|Ê|Ề|Ế|Ệ|Ể|Ễ/g, 'E');
  result = result.replace(/Ì|Í|Ị|Ỉ|Ĩ/g, 'I');
  result = result.replace(/Ò|Ó|Ọ|Ỏ|Õ|Ô|Ồ|Ố|Ộ|Ổ|Ỗ|Ơ|Ờ|Ớ|Ợ|Ở|Ỡ/g, 'O');
  result = result.replace(/Ù|Ú|Ụ|Ủ|Ũ|Ư|Ừ|Ứ|Ự|Ử|Ữ/g, 'U');
  result = result.replace(/Ỳ|Ý|Ỵ|Ỷ|Ỹ/g, 'Y');
  result = result.replace(/Đ/g, 'D');
  // Loại bỏ một số ký tự đặc biệt thừa
  result = result.replace(/\u0300|\u0301|\u0303|\u0309|\u0323/g, ''); 
  result = result.replace(/\u02C6|\u0306|\u031B/g, ''); 
  return result;
};

/**
 * Tách từ cho các câu viết dính liền không có khoảng trắng (độ dài > 15 ký tự)
 * @param {string} text - Chuỗi dính liền không khoảng trắng
 * @returns {string} - Chuỗi đã được thêm khoảng trắng phân cách từ
 */
export const segmentStickyText = (text = '') => {
  if (!text || text.includes(' ') || text.length <= 15) {
    return text;
  }

  let result = [];
  let remaining = text;

  while (remaining.length > 0) {
    let matchedToken = null;
    for (let len = Math.min(10, remaining.length); len >= 2; len--) {
      const sub = remaining.substring(0, len);
      if (dictionaryTokens.includes(sub)) {
        matchedToken = sub;
        break;
      }
    }

    if (matchedToken) {
      result.push(matchedToken);
      remaining = remaining.substring(matchedToken.length);
    } else {
      result.push(remaining[0]);
      remaining = remaining.substring(1);
    }
  }

  const segmentedStr = result.join(' ').replace(/\s+/g, ' ').trim();
  return segmentedStr || text;
};

/**
 * Chuẩn hóa tin nhắn đầu vào của người dùng
 * @param {string} rawText - Tin nhắn thô từ người dùng
 * @returns {string} - Tin nhắn đã chuẩn hóa (lowercase, không dấu, thay từ viết tắt)
 */
export const normalizeText = (rawText) => {
  if (!rawText || typeof rawText !== 'string') return '';
  
  // 1. Chuyển thành chữ thường, xóa zero-width spaces và khoảng trắng thừa ở 2 đầu
  let text = rawText.replace(/[\u200B-\u200D\uFEFF\u00A0]/g, '').trim().toLowerCase();

  // 2. Bỏ dấu tiếng Việt
  text = removeVietnameseTones(text);

  // 3. Thay thế ký tự đặc biệt bằng khoảng trắng (chỉ giữ ký tự chữ và số)
  text = text.replace(/[^a-z0-9\s]/g, ' ');

  // 3.5. Rút gọn ký tự lặp 3+ lần liên tiếp xuống 2 ký tự (vd: "hooomm" -> "hoomm", "phimmm" -> "phimm")
  text = text.replace(/(.)\1{2,}/g, '$1$1');

  // 3.6. Tách từ cho câu viết dính liền không khoảng trắng (> 15 ký tự)
  if (!text.includes(' ') && text.length > 15) {
    text = segmentStickyText(text);
  }

  // 4. Tách các từ và thay thế từ viết tắt
  const words = text.split(/\s+/);
  const normalizedWords = words.map(w => abbreviationMap[w] || w);

  // 5. Nối lại thành chuỗi chuẩn hóa
  return normalizedWords.join(' ').trim();
};
