/**
 * Service truy vấn dữ liệu phim, suất chiếu, giá vé và khuyến mãi thực tế từ MongoDB
 * Dùng Mongoose models: Movie, Showtime, Promotion, Seat, Cinema từ server/src/models/
 * Đã hỗ trợ Fuzzy Search (Levenshtein Distance) & Lọc lịch chiếu theo mốc thời gian tương đối.
 * Đã hỗ trợ Sanitize Movie Object loại bỏ các trường nội bộ của hệ thống/Mongoose.
 */

import { Movie } from '../../src/models/movieModel.js';
import { Showtime } from '../../src/models/showTimeModel.js';
import { Promotion } from '../../src/models/promotionModel.js';
import { Seat } from '../../src/models/seatModel.js';
import { Cinema } from '../../src/models/cinemaModel.js';
import { moodDictionary } from '../config/moodDictionary.js';
import { removeVietnameseTones } from './textNormalizer.js';

/**
 * Làm sạch bớt các trường nội bộ của Mongoose Document trước khi trả về client
 * Chỉ giữ lại các trường cần thiết cho giao diện hiển thị:
 * _id, movieName, duration, movieGenre, description, poster, rating, censorship, showtimes
/**
 * Làm sạch bớt các trường nội bộ của Mongoose Document trước khi trả về client
 * Chỉ giữ lại các trường cần thiết cho giao diện hiển thị:
 * _id, movieName, duration, movieGenre, description, poster, rating, censorship, showtimes
 * Đã tự động loại bỏ các dữ liệu rác (tên phim ngắn < 2 ký tự hoặc tên "ll", description "lll")
 * @param {Object} movie - Raw movie object từ Mongoose
 * @returns {Object|null} Clean sanitized movie object
 */
/**
 * Escape các ký tự đặc biệt HTML (XSS Protection)
 */
const escapeHTMLEntities = (str = '') => {
  if (typeof str !== 'string') return str;
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
};

export const sanitizeMovieForResponse = (movie) => {
  if (!movie) return null;
  const name = (movie.movieName || '').trim();
  if (name.length < 2 || name.toLowerCase() === 'll' || movie.description === 'lll') {
    return null;
  }

  const rawGenre = Array.isArray(movie.movieGenre) ? movie.movieGenre : [movie.movieGenre || ''];
  const sanitizedGenre = rawGenre.map(g => typeof g === 'string' ? escapeHTMLEntities(g) : g);

  const sanitized = {
    _id: movie._id,
    movieName: escapeHTMLEntities(name),
    duration: movie.duration || 0,
    movieGenre: sanitizedGenre,
    description: escapeHTMLEntities(movie.description || ''),
    poster: escapeHTMLEntities(movie.poster || ''),
    rating: movie.rating || 0,
    censorship: escapeHTMLEntities(movie.censorship || '')
  };

  if (Array.isArray(movie.showtimes) && movie.showtimes.length > 0) {
    sanitized.showtimes = movie.showtimes.map(st => typeof st === 'string' ? escapeHTMLEntities(st) : st);
  }

  return sanitized;
};

/**
 * Tính khoảng cách Levenshtein giữa 2 chuỗi để hỗ trợ Fuzzy Search khi gõ sai chính tả
 * @param {string} a - Chuỗi thứ nhất
 * @param {string} b - Chuỗi thứ hai
 * @returns {number} Khoảng cách sự khác biệt giữa 2 chuỗi
 */
export const levenshteinDistance = (a = '', b = '') => {
  const matrix = [];
  for (let i = 0; i <= b.length; i++) matrix[i] = [i];
  for (let j = 0; j <= a.length; j++) matrix[0][j] = j;

  for (let i = 1; i <= b.length; i++) {
    for (let j = 1; j <= a.length; j++) {
      if (b.charAt(i - 1) === a.charAt(j - 1)) {
        matrix[i][j] = matrix[i - 1][j - 1];
      } else {
        matrix[i][j] = Math.min(
          matrix[i - 1][j - 1] + 1,
          Math.min(matrix[i][j - 1] + 1, matrix[i - 1][j] + 1)
        );
      }
    }
  }

  return matrix[b.length][a.length];
};

/**
 * Gợi ý phim dựa trên danh sách các tag tâm trạng (moodKeys) và tag bị phủ định (negatedMoodKeys)
 * Hỗ trợ lọc giao (Match Score) khi người dùng chọn nhiều thể loại kết hợp (Mixed Intents)
 * Hỗ trợ loại trừ thể loại đối lập và thể loại bị phủ định (Negation)
 * @param {string[]} moodKeys - Mảng các key mood (ví dụ: ['buon', 'nhe_nhang'])
 * @param {number} limit - Số lượng phim tối đa trả về (mặc định 2)
 * @param {string[]} negatedMoodKeys - Mảng các key mood bị phủ định (ví dụ: ['hoi_hop'])
 * @returns {Promise<Array>} Danh sách các phim phù hợp kèm điểm ưu tiên
 */
export const findMoviesByMood = async (moodKeys = [], limit = 2, negatedMoodKeys = []) => {
  try {
    const targetGenresSet = new Set();
    const excludeGenresSet = new Set();

    moodKeys.forEach(key => {
      const moodInfo = moodDictionary[key];
      if (moodInfo && Array.isArray(moodInfo.genres)) {
        moodInfo.genres.forEach(g => targetGenresSet.add(g));
      }
      if (moodKeys.length === 1 && moodInfo && Array.isArray(moodInfo.excludeGenres)) {
        moodInfo.excludeGenres.forEach(g => excludeGenresSet.add(g));
      }
    });

    // Thu thập tất cả các thể loại thuộc mood bị phủ định (ví dụ: người dùng nói "không muốn xem kinh dị")
    negatedMoodKeys.forEach(key => {
      const moodInfo = moodDictionary[key];
      if (moodInfo && Array.isArray(moodInfo.genres)) {
        moodInfo.genres.forEach(g => excludeGenresSet.add(g));
      }
    });

    // Nếu người dùng CHỈ phủ định (không chọn mood nào khác, ví dụ: "tôi không muốn xem kinh dị")
    if (targetGenresSet.size === 0 && negatedMoodKeys.length > 0) {
      const allExclude = Array.from(excludeGenresSet);
      const candidates = await Movie.find({
        isDeleted: { $ne: true },
        movieGenre: { $nin: allExclude }
      }).sort({ rating: -1 }).lean();

      const validCandidates = (candidates || []).filter(m => m.movieName && m.movieName.length >= 2 && m.movieName !== 'll');
      return validCandidates.slice(0, limit);
    }

    const targetGenres = Array.from(targetGenresSet);
    if (targetGenres.length === 0) return [];

    // Loại bỏ các thể loại mục tiêu khỏi danh sách loại trừ (tránh loại trừ nhầm)
    const safeExcludeGenres = Array.from(excludeGenresSet).filter(g => !targetGenresSet.has(g));

    // Xây dựng query MongoDB với điều kiện loại trừ thể loại đối lập
    const query = {
      isDeleted: { $ne: true },
      movieGenre: { $in: targetGenres }
    };

    if (safeExcludeGenres.length > 0) {
      query.movieGenre = {
        $in: targetGenres,
        $nin: safeExcludeGenres
      };
    }

    let matchedMovies = await Movie.find(query).lean();

    // Fallback: Nếu không tìm thấy phim thỏa excludeGenres, query lại không có điều kiện loại trừ
    if (matchedMovies.length === 0 && safeExcludeGenres.length > 0) {
      matchedMovies = await Movie.find({
        isDeleted: { $ne: true },
        movieGenre: { $in: targetGenres }
      }).lean();
    }

    if (!matchedMovies || matchedMovies.length === 0) return [];

    const scoredMovies = matchedMovies.map(movie => {
      const genres = movie.movieGenre || [];
      const matchCount = genres.filter(g => targetGenres.includes(g)).length;
      // Phim có thể loại đối lập bị trừ điểm ưu tiên
      const penaltyCount = safeExcludeGenres.length > 0
        ? genres.filter(g => safeExcludeGenres.includes(g)).length
        : 0;
      return {
        ...movie,
        score: matchCount - penaltyCount * 0.5
      };
    });

    scoredMovies.sort((a, b) => b.score - a.score || (b.rating || 0) - (a.rating || 0));
    return scoredMovies.slice(0, limit);
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in findMoviesByMood:', error);
    return [];
  }
};

/**
 * Lấy danh sách các phim đang có suất chiếu hoặc đang chiếu theo mốc ngày (targetDate)
 * Đã tự động lọc bỏ các suất chiếu trùng lặp (Deduplicate showtimes) và sắp xếp theo thứ tự thời gian.
 * @param {number} limit - Số lượng tối đa trả về (mặc định 5)
 * @param {Date|null} targetDate - Ngày tra cứu lịch chiếu (mặc định là hôm nay)
 * @returns {Promise<Array>}
 */
export const getTodayShowtimes = async (limit = 5, targetDate = null) => {
  try {
    const baseDate = targetDate ? new Date(targetDate) : new Date();
    const dayStart = new Date(baseDate);
    dayStart.setHours(0, 0, 0, 0);

    const dayEnd = new Date(baseDate);
    dayEnd.setHours(23, 59, 59, 999);

    const showtimesToday = await Showtime.find({
      isDeleted: { $ne: true },
      showDate: { $gte: dayStart, $lte: dayEnd }
    })
      .populate('movieId')
      .lean();

    const movieMap = new Map();
    if (showtimesToday && showtimesToday.length > 0) {
      for (const st of showtimesToday) {
        if (st.movieId && st.movieId._id && !st.movieId.isDeleted) {
          const movieIdStr = st.movieId._id.toString();
          if (!movieMap.has(movieIdStr)) {
            movieMap.set(movieIdStr, {
              ...st.movieId,
              showtimes: []
            });
          }

          let timeStr = st.showTime || '';
          if (!timeStr && st.startTime) {
            timeStr = new Date(st.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
          }

          if (timeStr) {
            const currentShowtimes = movieMap.get(movieIdStr).showtimes;
            if (!currentShowtimes.includes(timeStr)) {
              currentShowtimes.push(timeStr);
            }
          }
        }
      }
    }

    // Sắp xếp các mốc giờ chiếu tăng dần chronologically
    for (const movie of movieMap.values()) {
      movie.showtimes.sort();
    }

    if (movieMap.size === 0) {
      const activeMovies = await Movie.find({
        isDeleted: { $ne: true },
        status: { $in: ['showing', 'now_showing', 'active'] }
      })
        .limit(limit)
        .lean();

      return activeMovies.map(m => ({ ...m, showtimes: [] }));
    }

    return Array.from(movieMap.values()).slice(0, limit);
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in getTodayShowtimes:', error);
    try {
      return await Movie.find({ isDeleted: { $ne: true } }).limit(limit).lean();
    } catch (e) {
      return [];
    }
  }
};

/**
 * Lấy danh sách suất chiếu của một rạp cụ thể theo mốc ngày (targetDate)
 * @param {string|null} cinemaId - ObjectId của rạp
 * @param {Date|null} targetDate - Ngày tra cứu (mặc định hôm nay)
 * @param {number} limit - Số lượng phim tối đa
 * @returns {Promise<Array>}
 */
export const getShowtimesByCinemaAndDate = async (cinemaId = null, targetDate = null, limit = 5) => {
  try {
    const baseDate = targetDate ? new Date(targetDate) : new Date();
    const dayStart = new Date(baseDate);
    dayStart.setHours(0, 0, 0, 0);

    const dayEnd = new Date(baseDate);
    dayEnd.setHours(23, 59, 59, 999);

    const filter = {
      isDeleted: { $ne: true },
      $or: [
        { showDate: { $gte: dayStart, $lte: dayEnd } },
        { startTime: { $gte: dayStart, $lte: dayEnd } }
      ]
    };

    if (cinemaId) {
      filter.cinemaId = cinemaId;
    }

    const showtimesList = await Showtime.find(filter)
      .populate('movieId')
      .sort({ startTime: 1 })
      .lean();

    const movieMap = new Map();
    if (showtimesList && showtimesList.length > 0) {
      for (const st of showtimesList) {
        if (st.movieId && st.movieId._id && !st.movieId.isDeleted) {
          const movieIdStr = st.movieId._id.toString();
          if (!movieMap.has(movieIdStr)) {
            movieMap.set(movieIdStr, {
              ...st.movieId,
              showtimes: []
            });
          }

          let timeStr = st.showTime || '';
          if (!timeStr && st.startTime) {
            timeStr = new Date(st.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Ho_Chi_Minh' });
          }

          if (timeStr) {
            const currentShowtimes = movieMap.get(movieIdStr).showtimes;
            if (!currentShowtimes.includes(timeStr)) {
              currentShowtimes.push(timeStr);
            }
          }
        }
      }
    }

    for (const movie of movieMap.values()) {
      movie.showtimes.sort();
    }

    if (movieMap.size === 0) {
      const activeMovies = await Movie.find({
        isDeleted: { $ne: true },
        status: { $in: ['showing', 'now_showing', 'active'] }
      })
        .limit(limit)
        .lean();

      return activeMovies.map(m => ({ ...m, showtimes: [] }));
    }

    return Array.from(movieMap.values()).slice(0, limit);
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in getShowtimesByCinemaAndDate:', error);
    return [];
  }
};

/**
 * Lấy danh sách suất chiếu của duy nhất một bộ phim theo tất cả các rạp
 * @param {string} movieId - ObjectId của phim
 * @returns {Promise<Array>}
 */
export const getShowtimesByMovieId = async (movieId) => {
  try {
    if (!movieId) return [];
    const dayStart = new Date();
    dayStart.setHours(0, 0, 0, 0);

    const showtimes = await Showtime.find({
      movieId,
      isDeleted: { $ne: true },
      $or: [
        { showDate: { $gte: dayStart } },
        { startTime: { $gte: dayStart } }
      ]
    })
      .populate('cinemaId')
      .sort({ startTime: 1 })
      .lean();

    return showtimes || [];
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in getShowtimesByMovieId:', error);
    return [];
  }
};

/**
 * Tìm kiếm chi tiết một bộ phim theo tên (hỗ trợ Fuzzy Search khi gõ sai chính tả / gõ thiếu dấu)
 * @param {string} searchQuery - Từ khóa/tên phim cần tìm
 * @returns {Promise<Object|null>} Document phim hoặc null nếu không tìm thấy
 */
/**
 * Tìm kiếm chi tiết và kiểm tra mờ mơ hồ (1 phim khớp vs 2+ phim khớp)
 * @param {string} searchQuery - Từ khóa/tên phim cần tìm
 * @returns {Promise<{status: 'single'|'multiple'|'none', movie: Object|null, movies: Array}>}
 */
export const getMovieDetailMatches = async (searchQuery) => {
  if (!searchQuery) return { status: 'none', movie: null, movies: [] };

  try {
    const rawQuery = searchQuery.trim();
    const normalizedQuery = removeVietnameseTones(rawQuery.toLowerCase());
    const cleanQuery = normalizedQuery.replace(/[^a-z0-9\s]/g, '').replace(/\s+/g, ' ').trim();

    if (!cleanQuery || cleanQuery.length < 2) return { status: 'none', movie: null, movies: [] };

    const allMovies = await Movie.find({ isDeleted: { $ne: true } }).lean();
    const validMovies = allMovies.filter(m => {
      const name = (m.movieName || '').trim();
      return name.length >= 2 && name.toLowerCase() !== 'll' && m.description !== 'lll';
    });

    // 1. Khớp CHÍNH XÁC 100%
    const exactMatches = validMovies.filter(m => {
      const normalizedName = removeVietnameseTones((m.movieName || '').toLowerCase());
      const cleanName = normalizedName.replace(/[^a-z0-9\s]/g, '').replace(/\s+/g, ' ').trim();
      return normalizedName === normalizedQuery || cleanName === cleanQuery;
    });

    if (exactMatches.length === 1) {
      return { status: 'single', movie: exactMatches[0], movies: exactMatches };
    } else if (exactMatches.length > 1) {
      return { status: 'multiple', movie: null, movies: exactMatches };
    }

    // 2. Khớp TIỀN TỐ (startsWith) khi cleanQuery >= 3 ký tự
    if (cleanQuery.length >= 3) {
      const prefixMatches = validMovies.filter(m => {
        const normalizedName = removeVietnameseTones((m.movieName || '').toLowerCase());
        const cleanName = normalizedName.replace(/[^a-z0-9\s]/g, '').replace(/\s+/g, ' ').trim();
        return cleanName.startsWith(cleanQuery) || cleanQuery.startsWith(cleanName) || normalizedName.startsWith(normalizedQuery) || normalizedQuery.startsWith(normalizedName);
      });

      if (prefixMatches.length === 1) {
        return { status: 'single', movie: prefixMatches[0], movies: prefixMatches };
      } else if (prefixMatches.length > 1) {
        return { status: 'multiple', movie: null, movies: prefixMatches };
      }
    }

    // 3. Khớp SUBSTRING (includes 2 chiều) khi cleanQuery >= 3 ký tự
    if (cleanQuery.length >= 3) {
      const substringMatches = validMovies.filter(m => {
        const normalizedName = removeVietnameseTones((m.movieName || '').toLowerCase());
        const cleanName = normalizedName.replace(/[^a-z0-9\s]/g, '').replace(/\s+/g, ' ').trim();
        return cleanName.includes(cleanQuery) || cleanQuery.includes(cleanName) || normalizedName.includes(normalizedQuery) || normalizedQuery.includes(normalizedName);
      });

      if (substringMatches.length === 1) {
        return { status: 'single', movie: substringMatches[0], movies: substringMatches };
      } else if (substringMatches.length > 1) {
        return { status: 'multiple', movie: null, movies: substringMatches };
      }
    }

    // 4. Fuzzy Search (Levenshtein Distance)
    let bestMatch = null;
    let minDistance = Infinity;

    for (const m of validMovies) {
      const cleanName = removeVietnameseTones((m.movieName || '').toLowerCase())
        .replace(/[^a-z0-9\s]/g, '')
        .replace(/\s+/g, ' ')
        .trim();

      const dist = levenshteinDistance(cleanQuery, cleanName);
      const maxAllowedDist = Math.max(2, Math.floor(cleanName.length * 0.4));

      if (dist <= maxAllowedDist && dist < minDistance) {
        minDistance = dist;
        bestMatch = m;
      }
    }

    if (bestMatch) {
      return { status: 'single', movie: bestMatch, movies: [bestMatch] };
    }

    return { status: 'none', movie: null, movies: [] };
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in getMovieDetailMatches:', error);
    return { status: 'none', movie: null, movies: [] };
  }
};

export const getMovieDetail = async (searchQuery) => {
  const res = await getMovieDetailMatches(searchQuery);
  return res.movie || (res.movies.length > 0 ? res.movies[0] : null);
};

/**
 * Kiểm tra tình trạng suất chiếu và ghế trống của 1 bộ phim trong ngày
 * @param {string} movieId - ObjectId của bộ phim
 * @param {Date|null} targetDate - Ngày cần kiểm tra
 * @returns {Promise<{isSoldOut: boolean, showtimesCount: number}>}
 */
export const checkMovieShowtimeAvailability = async (movieId, targetDate = null) => {
  if (!movieId) return { isSoldOut: true, showtimesCount: 0 };

  try {
    const baseDate = targetDate ? new Date(targetDate) : new Date();
    const dayStart = new Date(baseDate);
    dayStart.setHours(0, 0, 0, 0);

    const dayEnd = new Date(baseDate);
    dayEnd.setHours(23, 59, 59, 999);

    const showtimes = await Showtime.find({
      movieId,
      isDeleted: { $ne: true },
      showDate: { $gte: dayStart, $lte: dayEnd }
    }).lean();

    if (!showtimes || showtimes.length === 0) {
      return { isSoldOut: true, showtimesCount: 0 };
    }

    return { isSoldOut: false, showtimesCount: showtimes.length };
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in checkMovieShowtimeAvailability:', error);
    return { isSoldOut: false, showtimesCount: 1 };
  }
};

/**
 * Truy vấn các chương trình khuyến mãi / Voucher đang active từ CSDL MongoDB (Promotion Model)
 * @param {number} limit - Tối đa số voucher trả về (mặc định 5)
 * @returns {Promise<Array>}
 */
export const getPromotionsFromDB = async (limit = 5) => {
  try {
    const now = new Date();
    const activePromotions = await Promotion.find({
      isDeleted: { $ne: true },
      $or: [
        { endDate: { $gte: now } },
        { endDate: { $exists: false } },
        { endDate: null }
      ]
    })
      .limit(limit)
      .lean();

    return activePromotions || [];
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in getPromotionsFromDB:', error);
    return [];
  }
};

/**
 * Lấy danh sách các rạp đang hoạt động từ CSDL MongoDB
 * @returns {Promise<Array>} Danh sách rạp: _id, cinemaName, address, city
 */
export const getCinemasFromDB = async () => {
  try {
    const cinemas = await Cinema.find({
      isDeleted: { $ne: true },
      status: 'active'
    })
      .select('_id cinemaName address city')
      .lean();
    return cinemas || [];
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in getCinemasFromDB:', error);
    return [];
  }
};

/**
 * Lấy danh sách phim sắp chiếu từ BÂY GIỜ đến hết ngày theo rạp chỉ định
 * Chỉ lấy các suất có startTime >= now và startTime <= 23:59:59 hôm nay
 * @param {string} cinemaId - ObjectId của rạp cần tra cứu
 * @param {number} limit - Số phim tối đa trả về (mặc định 5)
 * @returns {Promise<Array>} Danh sách phim kèm các suất chiếu sắp tới
 */
export const getUpcomingShowtimesByCinema = async (cinemaId, limit = 5) => {
  try {
    const now = new Date();
    const dayEnd = new Date(now);
    dayEnd.setHours(23, 59, 59, 999);

    const upcomingShowtimes = await Showtime.find({
      isDeleted: { $ne: true },
      cinemaId,
      startTime: { $gte: now, $lte: dayEnd }
    })
      .populate('movieId')
      .sort({ startTime: 1 })
      .lean();

    if (!upcomingShowtimes || upcomingShowtimes.length === 0) return [];

    // Group theo phim, giữ các suất chiếu theo thứ tự thời gian
    const movieMap = new Map();
    for (const st of upcomingShowtimes) {
      if (!st.movieId || st.movieId.isDeleted) continue;
      const movieIdStr = st.movieId._id.toString();

      if (!movieMap.has(movieIdStr)) {
        movieMap.set(movieIdStr, {
          ...st.movieId,
          showtimes: []
        });
      }

      const movie = movieMap.get(movieIdStr);
      // Lưu thông tin giờ chiếu đầy đủ (startTime để hiển thị giờ:phút)
      const showTimeFmt = st.startTime
        ? new Date(st.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Ho_Chi_Minh' })
        : (st.showTime || '');
      if (showTimeFmt && !movie.showtimes.includes(showTimeFmt)) {
        movie.showtimes.push(showTimeFmt);
      }
    }

    const result = Array.from(movieMap.values());
    return result.slice(0, limit);
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in getUpcomingShowtimesByCinema:', error);
    return [];
  }
};

/**
 * Truy vấn dữ liệu giá vé thực tế từ CSDL MongoDB (Showtime.ticketPrice & Seat.price)
 * @returns {Promise<Object>} Object chứa thông tin giá vé theo suất chiếu và loại ghế
 */
export const getTicketPricesFromDB = async () => {
  try {
    const showtimes = await Showtime.find({ isDeleted: { $ne: true } })
      .select('ticketPrice')
      .lean();

    const seats = await Seat.find({ isDeleted: { $ne: true } })
      .select('seatType price')
      .lean();

    let showtimePrices = [];
    if (showtimes && showtimes.length > 0) {
      showtimePrices = showtimes
        .map(s => s.ticketPrice)
        .filter(p => typeof p === 'number' && p > 0);
    }

    const seatPriceMap = {};
    if (seats && seats.length > 0) {
      seats.forEach(s => {
        if (s.seatType && typeof s.price === 'number') {
          if (!seatPriceMap[s.seatType]) seatPriceMap[s.seatType] = [];
          seatPriceMap[s.seatType].push(s.price);
        }
      });
    }

    return {
      showtimePrices,
      seatPriceMap
    };
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in getTicketPricesFromDB:', error);
    return { showtimePrices: [], seatPriceMap: {} };
  }
};

/**
 * Lấy danh sách phim có điểm rating cao nhất từ MongoDB
 * @param {number} limit - Số lượng phim tối đa (mặc định 3)
 * @returns {Promise<Array>} Danh sách phim được sắp xếp theo rating giảm dần
 */
export const getTopRatedMovies = async (limit = 3) => {
  try {
    const movies = await Movie.find({
      isDeleted: { $ne: true }
    })
      .sort({ rating: -1 })
      .lean();

    const validMovies = (movies || []).filter(m => m.movieName && m.movieName.trim().length >= 2 && m.movieName.toLowerCase() !== 'll');
    return validMovies.slice(0, limit);
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in getTopRatedMovies:', error);
    return [];
  }
};

/**
 * Tìm kiếm phim theo Đạo diễn hoặc Diễn viên
 * @param {string} queryStr - Chuỗi tìm kiếm (vd: "Peyton Reed", "Sam Worthington")
 * @param {number} limit - Số lượng phim tối đa
 * @returns {Promise<{movies: Array, personName: string, type: string}>}
 */
export const findMoviesByDirectorOrActor = async (queryStr = '', limit = 5) => {
  if (!queryStr) return { movies: [], personName: '', type: 'all' };

  try {
    const rawQuery = queryStr.trim();
    const normalizedQuery = removeVietnameseTones(rawQuery.toLowerCase());

    // Loại bỏ các từ dừng (stop words) tiếng Việt phổ biến trong câu hỏi đạo diễn/diễn viên
    const stopWords = [
      'dao dien', 'dien vien', 'phim cua', 'phim co', 'phim', 'co', 'dong',
      'khong', 'nao', 'cua', 'do', 'la', 'chua', 'chay', 'oi', 'vay', 'xem'
    ];

    let cleanPattern = normalizedQuery;
    stopWords.forEach(sw => {
      const reg = new RegExp(`(?:^|\\s)${sw}(?:$|\\s)`, 'gi');
      cleanPattern = cleanPattern.replace(reg, ' ');
    });

    cleanPattern = cleanPattern.replace(/[^a-z0-9\s]/g, '').replace(/\s+/g, ' ').trim();
    const searchPattern = cleanPattern || normalizedQuery;
    const searchTokens = searchPattern.split(' ').filter(w => w.length >= 2);

    const allMovies = await Movie.find({ isDeleted: { $ne: true } }).lean();
    const validMovies = allMovies.filter(m => m.movieName && m.movieName.trim().length >= 2 && m.movieName.toLowerCase() !== 'll');

    const matchedMovies = [];
    let detectedType = 'all';

    for (const m of validMovies) {
      // Hỗ trợ cả trường director mảng hoặc chuỗi
      const directorNorm = Array.isArray(m.director)
        ? m.director.map(d => removeVietnameseTones((d || '').toLowerCase())).join(' ')
        : removeVietnameseTones((m.director || '').toLowerCase());

      // Hỗ trợ cả trường actors (số nhiều) hoặc actor (số ít) mảng hoặc chuỗi
      const actorsArr = Array.isArray(m.actors) 
        ? m.actors 
        : Array.isArray(m.actor) 
        ? m.actor 
        : [m.actor || m.actors || ''];
      const actorsNorm = actorsArr.map(a => removeVietnameseTones((a || '').toLowerCase())).join(' ');

      // So khớp nếu tất cả các token từ khóa tìm thấy trong directorNorm hoặc actorsNorm
      const isDirectorMatch = searchTokens.length > 0 && searchTokens.every(token => directorNorm.includes(token));
      const isActorMatch = searchTokens.length > 0 && searchTokens.every(token => actorsNorm.includes(token));

      if (isDirectorMatch) {
        matchedMovies.push(m);
        detectedType = 'director';
      } else if (isActorMatch) {
        matchedMovies.push(m);
        detectedType = 'actor';
      } else if (directorNorm && searchPattern && (directorNorm.includes(searchPattern) || searchPattern.includes(directorNorm))) {
        matchedMovies.push(m);
        detectedType = 'director';
      } else if (actorsNorm && searchPattern && (actorsNorm.includes(searchPattern) || searchPattern.includes(actorsNorm))) {
        matchedMovies.push(m);
        detectedType = 'actor';
      }
    }

    // Capitalize tên tìm kiếm cho đẹp mắt
    const prettyName = searchPattern
      .split(' ')
      .map(w => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');

    return {
      movies: matchedMovies.slice(0, limit),
      personName: prettyName || rawQuery,
      type: detectedType
    };
  } catch (error) {
    console.error('[Chatbot movieMatcher] Error in findMoviesByDirectorOrActor:', error);
    return { movies: [], personName: queryStr, type: 'all' };
  }
};

