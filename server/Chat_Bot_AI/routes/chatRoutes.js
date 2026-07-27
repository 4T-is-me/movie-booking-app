/**
 * Router xử lý các yêu cầu Chatbot API (`POST /api/chat`, `GET /api/chat/history/:sessionId`)
 * Đã hỗ trợ: PII Masking, Multi-intent chéo luồng (Cross-Domain), Setup/Teardown Reset & System Maintenance Error UX.
 * Đã tích hợp query dữ liệu thật từ MongoDB cho Ticket Prices & Promotion Models.
 * Đã hỗ trợ Sanitize Movie Object tự động loại bỏ các field nội bộ trước khi gửi response.
 */

import express from 'express';
import { normalizeText } from '../services/textNormalizer.js';
import { classifyIntent } from '../services/intentClassifier.js';
import {
  findMoviesByMood,
  getTodayShowtimes,
  getMovieDetail,
  getPromotionsFromDB,
  getTicketPricesFromDB,
  getCinemasFromDB,
  getUpcomingShowtimesByCinema,
  getTopRatedMovies,
  findMoviesByDirectorOrActor,
  sanitizeMovieForResponse
} from '../services/movieMatcher.js';
import { maskPIIData } from '../config/securityGuardrails.js';
import {
  buildMoodResponse,
  buildScheduleResponse,
  buildMovieInfoResponse,
  buildFallbackResponse,
  buildSecurityViolationResponse,
  buildGreetingResponse,
  buildBotIdentityResponse,
  buildTicketPriceResponse,
  buildPromotionResponse,
  buildBookingGuidanceResponse,
  buildSoldOutResponse,
  buildSystemMaintenanceResponse,
  buildCinemaSelectionRequest,
  buildUpcomingShowtimeResponse,
  buildRecommendationPromptResponse,
  buildTopRatedResponse,
  buildMovieComparisonResponse,
  buildDirectorActorResponse,
  buildNegatedMoodResponse,
  buildEmptyMessageResponse
} from '../services/responseBuilder.js';

const router = express.Router();

// Lưu trữ lịch sử hội thoại in-memory theo sessionId: Map<sessionId, Array<{role, content, timestamp}>>
const sessionHistoryMap = new Map();

// Lưu vết rate limiting in-memory: Map<sessionId, Array<timestamp>>
const rateLimitMap = new Map();

// Lưu vết danh sách phim gợi ý gần nhất cho mỗi session để hỗ trợ gọi phim theo thứ tự ("phim số 1", "phim thứ 2")
const lastSuggestedMoviesMap = new Map();

/**
 * Lưu trạng thái chờ multi-turn: Map<sessionId, PendingContext>
 * PendingContext: { step: 'awaiting_cinema_selection', cinemaList: Array, createdAt: number }
 * Tự động hết hạn sau 3 phút nếu người dùng không chọn rạp
 */
const pendingContextMap = new Map();
const PENDING_CONTEXT_TTL_MS = 3 * 60 * 1000; // 3 phút

const MAX_HISTORY_LENGTH = 50; // Giới hạn tối đa 50 tin nhắn mỗi session
const RATE_LIMIT_MAX_REQUESTS = 20; // Tối đa 20 request
const RATE_LIMIT_WINDOW_MS = 60 * 1000; // Trong vòng 1 phút

/**
 * Dọn dẹp bộ nhớ cache session và rate limit (Setup/Teardown Environment Cleanup)
 */
export const resetChatbotSessionState = () => {
  sessionHistoryMap.clear();
  rateLimitMap.clear();
  pendingContextMap.clear();
  lastSuggestedMoviesMap.clear();
};

/**
 * Kiểm tra rate limit đơn giản theo sessionId
 * @param {string} sessionId
 * @returns {boolean} - true nếu vượt quá giới hạn, false nếu hợp lệ
 */
const isRateLimited = (sessionId) => {
  const now = Date.now();
  if (!rateLimitMap.has(sessionId)) {
    rateLimitMap.set(sessionId, [now]);
    return false;
  }

  const timestamps = rateLimitMap.get(sessionId).filter(ts => now - ts < RATE_LIMIT_WINDOW_MS);
  if (timestamps.length >= RATE_LIMIT_MAX_REQUESTS) {
    return true;
  }

  timestamps.push(now);
  rateLimitMap.set(sessionId, timestamps);
  return false;
};

/**
 * Lưu tin nhắn vào lịch sử hội thoại session
 * @param {string} sessionId
 * @param {string} role - 'user' hoặc 'bot'
 * @param {string} content - Nội dung tin nhắn
 */
const saveMessageToHistory = (sessionId, role, content) => {
  if (!sessionId) return;
  if (!sessionHistoryMap.has(sessionId)) {
    sessionHistoryMap.set(sessionId, []);
  }

  const history = sessionHistoryMap.get(sessionId);
  history.push({
    role,
    content,
    timestamp: new Date().toISOString()
  });

  if (history.length > MAX_HISTORY_LENGTH) {
    sessionHistoryMap.set(sessionId, history.slice(history.length - MAX_HISTORY_LENGTH));
  }
};

/**
 * POST /api/chat
 * Body: { message: string, sessionId: string }
 */
router.post('/', async (req, res) => {
  try {
    const { message, sessionId } = req.body || {};
    const effectiveSessionId = sessionId && typeof sessionId === 'string' ? sessionId.trim() : 'default_session';

    // 1. Validate Input & Xử lý tin nhắn rỗng / khoảng trắng nhẹ nhàng
    if (!message || typeof message !== 'string' || !message.trim()) {
      const emptyReply = buildEmptyMessageResponse();
      saveMessageToHistory(effectiveSessionId, 'bot', emptyReply);
      return res.json({
        success: true,
        reply: emptyReply,
        matchedIntent: 'empty_message',
        matchedMovies: []
      });
    }

    if (message.length > 500) {
      return res.status(400).json({
        success: false,
        message: 'Nội dung tin nhắn quá dài (tối đa 500 ký tự).'
      });
    }

    // 2. Rate limit check
    if (isRateLimited(effectiveSessionId)) {
      return res.status(429).json({
        success: false,
        message: 'Bạn gửi tin nhắn quá nhanh. Vui lòng chờ 1 phút trước khi thử lại.'
      });
    }

    // 3. Tự động che (masking) các dữ liệu nhạy cảm PII (SĐT, số thẻ) trước khi lưu vào history/log
    const maskedMessage = maskPIIData(message);
    saveMessageToHistory(effectiveSessionId, 'user', maskedMessage);

    // 3.5. Xử lý câu hỏi gộp nhiều dòng / nhiều ý (Multi-Question Split)
    const subQuestions = message.split(/[\n;?]+/).map(s => s.trim()).filter(s => s.length >= 2);
    if (subQuestions.length > 1) {
      const multiReplies = [];
      let allMatchedMovies = [];

      for (const q of subQuestions) {
        const normQ = normalizeText(q);
        const clsQ = classifyIntent(normQ, q);

        if (clsQ.intent === 'schedule_query') {
          const m = await getTodayShowtimes(3, clsQ.targetDate);
          multiReplies.push(buildScheduleResponse(m, clsQ.dateTitle || 'hôm nay'));
          allMatchedMovies.push(...m);
        } else if (clsQ.intent === 'ticket_price_query') {
          const p = await getTicketPricesFromDB();
          multiReplies.push(buildTicketPriceResponse(p));
        } else if (clsQ.intent === 'promotion_query') {
          const promo = await getPromotionsFromDB();
          multiReplies.push(buildPromotionResponse(promo));
        } else if (clsQ.intent === 'booking_guidance') {
          multiReplies.push(buildBookingGuidanceResponse());
        } else if (clsQ.intent === 'movie_ordinal_query') {
          const idx = clsQ.ordinalIndex || 1;
          const lastMovies = lastSuggestedMoviesMap.get(effectiveSessionId) || [];
          const targetM = lastMovies[idx - 1];
          if (targetM) {
            multiReplies.push(buildMovieInfoResponse(targetM, targetM.movieName));
            allMatchedMovies.push(targetM);
          }
        }
      }

      if (multiReplies.length > 0) {
        const combinedReply = multiReplies.join('\n\n---\n\n');
        saveMessageToHistory(effectiveSessionId, 'bot', combinedReply);
        const cleanMovies = allMatchedMovies.map(sanitizeMovieForResponse).filter(Boolean);
        if (cleanMovies.length > 0) lastSuggestedMoviesMap.set(effectiveSessionId, cleanMovies);

        return res.json({
          success: true,
          reply: combinedReply,
          matchedIntent: 'multi_question_result',
          matchedMovies: cleanMovies
        });
      }
    }

    // 4. Chuẩn hóa tin nhắn và phân loại ý định
    const normalizedMessage = normalizeText(message);
    const classification = classifyIntent(normalizedMessage, message);
    const { intent } = classification;

    let reply = '';
    let matchedMovies = [];

    // === XỬ LÝ MULTI-TURN: Kiểm tra pending context trước khi vào switch intent ===
    const pendingCtx = pendingContextMap.get(effectiveSessionId);
    const isPendingExpired = pendingCtx && (Date.now() - pendingCtx.createdAt > PENDING_CONTEXT_TTL_MS);
    if (isPendingExpired) {
      pendingContextMap.delete(effectiveSessionId);
    }

    const activePending = pendingContextMap.get(effectiveSessionId);

    if (activePending?.step === 'awaiting_cinema_selection') {
      // Người dùng đang trong bước chọn rạp
      const cinemaList = activePending.cinemaList || [];
      let selectedCinema = null;

      // Thử nhận diện bằng số thứ tự ("1", "2", ...)
      const numMatch = normalizedMessage.trim().match(/^(\d+)$/);
      if (numMatch) {
        const idx = parseInt(numMatch[1], 10) - 1;
        if (idx >= 0 && idx < cinemaList.length) {
          selectedCinema = cinemaList[idx];
        }
      }

      // Thử nhận diện bằng tên rạp (fuzzy: includes)
      if (!selectedCinema) {
        const msgLower = normalizedMessage.toLowerCase();
        selectedCinema = cinemaList.find(c => {
          const nameNorm = normalizeText(c.cinemaName).toLowerCase();
          const cityNorm = normalizeText(c.city || '').toLowerCase();
          return nameNorm.includes(msgLower) || msgLower.includes(nameNorm) ||
                 cityNorm.includes(msgLower) || msgLower.includes(cityNorm);
        });
      }

      if (selectedCinema) {
        // Đã chọn được rạp → Query phim sắp chiếu
        pendingContextMap.delete(effectiveSessionId);
        const now = new Date();
        const upcomingMovies = await getUpcomingShowtimesByCinema(selectedCinema._id);
        reply = buildUpcomingShowtimeResponse(upcomingMovies, selectedCinema.cinemaName, now);
        matchedMovies = upcomingMovies;
      } else if (intent === 'upcoming_showtime_query') {
        reply = buildCinemaSelectionRequest(cinemaList);
      } else {
        pendingContextMap.delete(effectiveSessionId);
      }

      if (reply) {
        saveMessageToHistory(effectiveSessionId, 'bot', reply);
        const cleanMatchedMovies = Array.isArray(matchedMovies)
          ? matchedMovies.map(sanitizeMovieForResponse).filter(Boolean)
          : [];
        if (cleanMatchedMovies.length > 0) lastSuggestedMoviesMap.set(effectiveSessionId, cleanMatchedMovies);

        return res.json({
          success: true,
          reply,
          matchedIntent: 'upcoming_showtime_result',
          matchedMovies: cleanMatchedMovies
        });
      }
    }
    // === HẾT XỬ LÝ MULTI-TURN ===

    // 5. Thực thi logic xử lý theo Intent
    switch (intent) {
      case 'security_violation': {
        reply = buildSecurityViolationResponse();
        break;
      }

      case 'greeting': {
        reply = buildGreetingResponse();
        break;
      }

      case 'bot_identity': {
        reply = buildBotIdentityResponse();
        break;
      }

      case 'recommendation_prompt': {
        matchedMovies = await getTopRatedMovies(3);
        reply = buildRecommendationPromptResponse(matchedMovies);
        break;
      }

      case 'top_rated_query': {
        matchedMovies = await getTopRatedMovies(5);
        reply = buildTopRatedResponse(matchedMovies);
        break;
      }

      case 'movie_comparison_query': {
        const cleanNorm = normalizedMessage.replace(/gieu|giua|phim|hay hon|so sanh|nen xem|khong|nao|gi/gi, '').trim();
        const parts = cleanNorm.split(/va|voi|vs/i).map(s => s.trim()).filter(Boolean);
        const nameA = parts[0] || 'Phim A';
        const nameB = parts[1] || 'Phim B';
        const movieA = await getMovieDetail(nameA);
        const movieB = await getMovieDetail(nameB);
        matchedMovies = [movieA, movieB].filter(Boolean);
        reply = buildMovieComparisonResponse(movieA, movieB, nameA, nameB);
        break;
      }

      case 'movie_ordinal_query': {
        const ordinalIndex = classification.ordinalIndex || 1;
        const lastMovies = lastSuggestedMoviesMap.get(effectiveSessionId) || [];
        const targetMovie = lastMovies[ordinalIndex - 1];

        if (targetMovie) {
          matchedMovies = [targetMovie];
          reply = buildMovieInfoResponse(targetMovie, targetMovie.movieName);
        } else {
          const todayMovies = await getTodayShowtimes(5);
          const fallbackMovie = todayMovies[ordinalIndex - 1] || todayMovies[0];
          if (fallbackMovie) {
            matchedMovies = [fallbackMovie];
            reply = buildMovieInfoResponse(fallbackMovie, fallbackMovie.movieName);
          } else {
            reply = buildFallbackResponse();
          }
        }
        break;
      }

      case 'director_actor_query': {
        const { movies, personName, type } = await findMoviesByDirectorOrActor(message);
        matchedMovies = movies;
        reply = buildDirectorActorResponse(movies, personName, type);
        break;
      }

      case 'ticket_price_query': {
        const ticketPriceData = await getTicketPricesFromDB();
        reply = buildTicketPriceResponse(ticketPriceData);
        break;
      }

      case 'promotion_query': {
        const promotions = await getPromotionsFromDB();
        reply = buildPromotionResponse(promotions);
        break;
      }

      case 'booking_guidance': {
        reply = buildBookingGuidanceResponse();
        break;
      }

      case 'upcoming_showtime_query': {
        const cinemaList = await getCinemasFromDB();
        pendingContextMap.set(effectiveSessionId, {
          step: 'awaiting_cinema_selection',
          cinemaList,
          createdAt: Date.now()
        });
        reply = buildCinemaSelectionRequest(cinemaList);
        break;
      }

      case 'schedule_query': {
        const targetDate = classification.targetDate || null;
        const dateTitle = classification.dateTitle || 'hôm nay';
        matchedMovies = await getTodayShowtimes(5, targetDate);
        reply = buildScheduleResponse(matchedMovies, dateTitle);
        break;
      }

      case 'mood_query': {
        const matchedMoods = classification.matchedMoods || [];
        const negatedMoods = classification.negatedMoods || [];
        matchedMovies = await findMoviesByMood(matchedMoods, 2, negatedMoods);

        if (matchedMoods.length === 0 && negatedMoods.length > 0) {
          const moodLabels = {
            hoi_hop: 'Kinh dị / Hồi hộp',
            buon: 'Buồn / Thất tình',
            vui_ve: 'Vui vẻ / Hài hước',
            hanh_dong: 'Hành động / Kịch tính',
            nhe_nhang: 'Nhẹ nhàng / Lãng mạn'
          };
          const negatedLabel = negatedMoods.map(m => moodLabels[m] || m).join(', ');
          reply = buildNegatedMoodResponse(matchedMovies, negatedLabel);
        } else {
          reply = buildMoodResponse(matchedMovies, matchedMoods);
        }
        break;
      }

      case 'movie_info_query': {
        const searchQuery = classification.searchQuery || normalizedMessage;
        const movie = await getMovieDetail(searchQuery);
        if (movie) matchedMovies = [movie];
        reply = buildMovieInfoResponse(movie, searchQuery);
        break;
      }

      case 'movie_info_query_or_fallback': {
        const searchQuery = classification.searchQuery || normalizedMessage;
        const movie = await getMovieDetail(searchQuery);
        if (movie) {
          matchedMovies = [movie];
          reply = buildMovieInfoResponse(movie, searchQuery);
        } else {
          reply = buildFallbackResponse();
        }
        break;
      }

      case 'fallback':
      default: {
        reply = buildFallbackResponse();
        const fallbackLog = {
          timestamp: new Date().toISOString(),
          event: 'CHATBOT_FALLBACK',
          sessionId: effectiveSessionId,
          rawMessage: message,
          normalizedMessage
        };
        console.log('[FALLBACK_LOGGER]', JSON.stringify(fallbackLog));
        break;
      }
    }

    // 5.5. Đa ý định chéo luồng (Cross-Domain Multi-Intent Handling)
    if (['schedule_query', 'mood_query', 'movie_info_query', 'movie_info_query_or_fallback'].includes(intent) && 
        (normalizedMessage.includes('gia ve') || normalizedMessage.includes('bao nhieu tien') || normalizedMessage.includes('gia bao nhieu'))) {
      const ticketPriceData = await getTicketPricesFromDB();
      reply += '\n\n' + buildTicketPriceResponse(ticketPriceData);
    }

    // Sanitize matchedMovies trước khi trả về client để ẩn các trường hệ thống/nội bộ Mongoose
    const cleanMatchedMovies = Array.isArray(matchedMovies)
      ? matchedMovies.map(sanitizeMovieForResponse).filter(Boolean)
      : [];

    // Lưu danh sách phim vừa gợi ý vào session context để dùng cho các lượt tra cứu tiếp theo (vd: "phim thứ 2")
    if (cleanMatchedMovies.length > 0) {
      lastSuggestedMoviesMap.set(effectiveSessionId, cleanMatchedMovies);
    }

    // 6. Lưu câu trả lời của bot vào history
    saveMessageToHistory(effectiveSessionId, 'bot', reply);

    // 7. Trả về JSON cho client
    return res.json({
      success: true,
      reply,
      matchedIntent: intent,
      matchedMovies: cleanMatchedMovies
    });

  } catch (error) {
    console.error('[Chatbot Route Error]:', error);
    return res.status(500).json({
      success: false,
      message: buildSystemMaintenanceResponse()
    });
  }
});

/**
 * GET /api/chat/history/:sessionId
 * Lấy lịch sử hội thoại cho sessionId
 */
router.get('/history/:sessionId', (req, res) => {
  const { sessionId } = req.params;
  const history = sessionHistoryMap.get(sessionId) || [];
  return res.json({
    success: true,
    sessionId,
    history
  });
});

export default router;

