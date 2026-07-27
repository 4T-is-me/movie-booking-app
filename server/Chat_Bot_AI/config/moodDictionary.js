/**
 * Từ điển tâm trạng (moodDictionary) và bảng dịch thể loại (genreTranslationMap).
 * Đã mở rộng phong phú các từ khóa cảm xúc, teencode, ngôn ngữ nói của người dùng.
 */

export const moodDictionary = {
  buon: {
    name: 'Buồn / Thất tình / Tâm trạng',
    keywords: [
      'buon', 'chan', 'that tinh', 'co don', 'khoc', 'sau', 'tam trang', 
      'met moi', 'suy', 'tuyet vong', 'nhung nho', 'den toi', 'bi ai', 
      'stress', 'sau lang', 'tieu cuc', 'vo vong', 'tan vo', 'buon ba'
    ],
    genres: ['Drama', 'Romance'],
    // Loại trừ thể loại hài hước, hành động khi đang buồn
    excludeGenres: ['Comedy', 'Action']
  },
  vui_ve: {
    name: 'Vui vẻ / Hài hước / Hào hứng',
    keywords: [
      'vui', 'hao hung', 'muon cuoi', 'hai huoc', 'giai tri', 'phoi phoi', 
      'yeu doi', 'cuoi', 'hai', 'xien ve', 'xa stress', 'funny', 'vui ve', 
      'cuoi nghieng nga', 'cuoi te ghe', 'hai bop bop', 'thu gian tam hon',
      'vui tuoi', 'cuoi nhe', 'hai huong'
    ],
    genres: ['Comedy', 'Animation', 'Adventure'],
    // Loại trừ thể loại kinh dị, chính kịch nặng khi đang vui
    excludeGenres: ['Horror', 'Thriller', 'Drama']
  },
  hoi_hop: {
    name: 'Hồi hộp / Giặt gân / Kinh dị',
    keywords: [
      'hoi hop', 'kinh di', 'cam giac manh', 'so', 'giat gan', 'am anh', 
      'ma', 'ly ky', 'run ruy', 'do mat', 'kinh hoang', 'huyen bi', 'xac chet', 
      'quy', 'giat thot tim', 'giao keu', 'nhot nhat', 'ron toc gay', 'ron nguoi',
      'so hai', 'ma quai', 'bi an'
    ],
    genres: ['Horror', 'Thriller', 'Mystery'],
    // Loại trừ hài hước, lãng mạn khi muốn hồi hộp
    excludeGenres: ['Comedy', 'Romance', 'Family']
  },
  hanh_dong: {
    name: 'Hành động / Kịch tính / Chiến đấu',
    keywords: [
      'hanh dong', 'manh me', 'ban phau', 'danh nhau', 'ngau', 'kich tinh', 
      'kieu hieu', 'chien dau', 'vo thuat', 'ruot đuoi', 'duoi bat', 'sieu anh hung', 
      'danh lon', 'chay no', 'bop co', 'giai cuu', 'vo hiep', 'gay can', 'danh dam'
    ],
    genres: ['Action', 'Adventure', 'Sci-Fi'],
    // Loại trừ lãng mạn, chính kịch nhẹ khi muốn hành động
    excludeGenres: ['Romance', 'Drama', 'Family']
  },
  nhe_nhang: {
    name: 'Nhẹ nhàng / Thư giãn / Lãng mạn',
    keywords: [
      'thu gian', 'nhe nhang', 'binh yen', 'chill', 'em diem', 'lang man', 
      'ngot ngao', 'tinh cam', 'mong mo', 'thoat khoi thuc tai', 'ngot', 
      'sau lang', 'tinh yeu', 'dieu dang', 'nhe nhang chill'
    ],
    genres: ['Romance', 'Animation', 'Comedy'],
    // Loại trừ hành động, kinh dị khi muốn nhẹ nhàng
    excludeGenres: ['Action', 'Horror', 'Thriller']
  },
  gia_dinh: {
    name: 'Gia đình / Trẻ em / Thiếu nhi',
    keywords: [
      'gia dinh', 'cung con', 'tre em', 'thieu nhi', 'nguoi than', 'am ap', 
      'tinh mau tu', 'hoat hoa', 'phim hoat hinh', 'cho con', 'sum vay', 
      'con nit', 'cho be', 'gia dinh nho', 'tinh phu tu'
    ],
    genres: ['Animation', 'Family', 'Adventure'],
    // Loại trừ kinh dị, hành động bạo lực khi xem cùng trẻ em
    excludeGenres: ['Horror', 'Thriller', 'Crime']
  }
};

export const genreTranslationMap = {
  'Action': 'Hành động',
  'Comedy': 'Hài hước',
  'Horror': 'Kinh dị',
  'Drama': 'Chính kịch / Tâm lý',
  'Animation': 'Hoạt hình',
  'Romance': 'Lãng mạn / Tình cảm',
  'Thriller': 'Giật gân',
  'Adventure': 'Phiêu lưu',
  'Sci-Fi': 'Khoa học viễn tưởng',
  'Family': 'Gia đình',
  'Mystery': 'Bí ẩn',
  'Fantasy': 'Kỳ ảo',
  'Crime': 'Tội phạm',
  'Documentary': 'Tài liệu'
};

/**
 * Trả về danh sách tên tiếng Việt của các thể loại tiếng Anh
 * @param {string[]} genres - Mảng thể loại tiếng Anh
 * @returns {string} - Chuỗi thể loại tiếng Việt phân cách bằng dấu phẩy
 */
export const translateGenresToVietnamese = (genres = []) => {
  if (!Array.isArray(genres) || genres.length === 0) return 'Chưa phân loại';
  return genres
    .map(g => genreTranslationMap[g] || g)
    .join(', ');
};
