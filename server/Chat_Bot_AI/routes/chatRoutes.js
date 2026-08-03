/**
 * Router xử lý các yêu cầu Chatbot API (`POST /api/chat`, `GET /api/chat/history/:sessionId`)
 * Đã hỗ trợ: PII Masking, Multi-intent chéo luồng (Cross-Domain), Setup/Teardown Reset & System Maintenance Error UX.
 * Đã tích hợp query dữ liệu thật từ MongoDB cho Ticket Prices & Promotion Models.
 * Đã hỗ trợ Sanitize Movie Object tự động loại bỏ các field nội bộ trước khi gửi response.
 */

import express from 'express';
import multer from 'multer';
import { GoogleGenerativeAI } from '@google/generative-ai';
import { normalizeText } from '../services/textNormalizer.js';
import { classifyIntent, detectContextualSubIntent, isImplicitContextualFollowUp } from '../services/intentClassifier.js';
import { findMoviesByMood, getTodayShowtimes, getMovieDetail, getPromotionsFromDB, getTicketPricesFromDB, getCinemasFromDB, getUpcomingShowtimesByCinema, getShowtimesByCinemaAndDate, getShowtimesByMovieId, getTopRatedMovies, findMoviesByDirectorOrActor, sanitizeMovieForResponse, getMovieDetailMatches } from '../services/movieMatcher.js';
import { maskPIIData } from '../config/securityGuardrails.js';
import { buildMoodResponse, buildScheduleResponse, buildMovieInfoResponse, buildFallbackResponse, buildSecurityViolationResponse, buildGreetingResponse, buildBotIdentityResponse, buildTicketPriceResponse, buildPromotionResponse, buildBookingGuidanceResponse, buildSystemMaintenanceResponse, buildCinemaSelectionRequest, buildUpcomingShowtimeResponse, buildRecommendationPromptResponse, buildTopRatedResponse, buildMovieComparisonResponse, buildDirectorActorResponse, buildNegatedMoodResponse, buildEmptyMessageResponse, buildMovieShowtimeDetailResponse, buildTicketPriceForMovieResponse, buildAmbiguousMovieClarificationResponse, buildMovieDirectorActorDetailResponse } from '../services/responseBuilder.js';

const router = express.Router();

// Lưu trữ lịch sử hội thoại in-memory theo sessionId: Map<sessionId, Array<{role, content, timestamp}>>
const sessionHistoryMap = new Map();

// Lưu vết rate limiting in-memory: Map<sessionId, Array<timestamp>>
const rateLimitMap = new Map();

// Lưu vết danh sách phim gợi ý gần nhất cho mỗi session để hỗ trợ gọi phim theo thứ tự ("phim số 1", "phim thứ 2")
const lastSuggestedMoviesMap = new Map();

/**
 * Lưu trạng thái chờ multi-turn: Map<sessionId, PendingContext>
 * PendingContext: { step: 'awaiting_cinema_selection', cinemaList: Array, targetDate, dateTitle, isUpcomingOnly, createdAt: number }
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
  lastSuggestedMoviesMap.clear();
  pendingContextMap.clear();
};

export { lastSuggestedMoviesMap, pendingContextMap };

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
    const { message, sessionId, isFromVision } = req.body || {};
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
    const rawMsg = message.trim();
    const normalizedMessage = normalizeText(message);
    
    let classification;
    if (isFromVision) {
      classification = {
        intent: 'movie_info_query',
        searchQuery: rawMsg,
        confidence: 1.0,
        isFromVision: true
      };
    } else {
      classification = classifyIntent(normalizedMessage, message);
    }
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

      const rawMsgTrim = message.trim();
      const msgLower = normalizedMessage.toLowerCase();
      const rawMsgLower = rawMsgTrim.toLowerCase();

      // 1. Thử nhận diện bằng _id (khi client truyền _id từ button click)
      selectedCinema = cinemaList.find(c => c._id && c._id.toString() === rawMsgTrim);

      // 2. Thử nhận diện bằng tên rạp khớp chính xác (khi client truyền tên rạp từ button click)
      if (!selectedCinema) {
        selectedCinema = cinemaList.find(c => {
          const rawName = (c.cinemaName || '').toLowerCase().trim();
          const nameNorm = normalizeText(c.cinemaName).toLowerCase();
          return rawName === rawMsgLower || nameNorm === msgLower;
        });
      }

      // 3. Thử nhận diện bằng số thứ tự ("1", "2", ...)
      if (!selectedCinema) {
        const numMatch = normalizedMessage.trim().match(/^(\d+)$/);
        if (numMatch) {
          const idx = parseInt(numMatch[1], 10) - 1;
          if (idx >= 0 && idx < cinemaList.length) {
            selectedCinema = cinemaList[idx];
          }
        }
      }

      // 4. Thử nhận diện bằng tên rạp / thành phố (fuzzy: includes)
      if (!selectedCinema) {
        selectedCinema = cinemaList.find(c => {
          const nameNorm = normalizeText(c.cinemaName).toLowerCase();
          const cityNorm = normalizeText(c.city || '').toLowerCase();
          const rawName = (c.cinemaName || '').toLowerCase();
          return nameNorm.includes(msgLower) || msgLower.includes(nameNorm) ||
                 rawName.includes(rawMsgLower) || rawMsgLower.includes(rawName) ||
                 cityNorm.includes(msgLower) || msgLower.includes(cityNorm);
        });
      }

      if (selectedCinema) {
        // Đã chọn được rạp → Query lịch chiếu hoặc suất chiếu sắp tới theo rạp đã chọn
        pendingContextMap.delete(effectiveSessionId);
        if (activePending.isUpcomingOnly) {
          const now = new Date();
          const upcomingMovies = await getUpcomingShowtimesByCinema(selectedCinema._id);
          reply = buildUpcomingShowtimeResponse(upcomingMovies, selectedCinema.cinemaName, now);
          matchedMovies = upcomingMovies;
        } else {
          const targetDate = activePending.targetDate || null;
          const dateTitle = activePending.dateTitle || 'hôm nay';
          const scheduleMovies = await getShowtimesByCinemaAndDate(selectedCinema._id, targetDate);
          reply = buildScheduleResponse(scheduleMovies, dateTitle, selectedCinema.cinemaName);
          matchedMovies = scheduleMovies;
        }
      } else if (intent === 'upcoming_showtime_query' || intent === 'schedule_query') {
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

        const resObj = {
          success: true,
          reply,
          matchedIntent: selectedCinema
            ? (activePending.isUpcomingOnly ? 'upcoming_showtime_result' : 'schedule_result')
            : 'cinema_selection_request',
          matchedMovies: cleanMatchedMovies
        };

        if (!selectedCinema && cinemaList.length > 0) {
          resObj.cinemaOptions = cinemaList.map(c => ({
            _id: c._id,
            cinemaName: c.cinemaName,
            address: c.address || '',
            city: c.city || ''
          }));
        }

        return res.json(resObj);
      }
    }
    // === HẾT XỬ LÝ MULTI-TURN ===

    // === XỬ LÝ CONTEXTUAL PRONOUN & IMPLICIT FOLLOW-UP QUERY (phim đó, phim này, nó, có diễn viên nào, đạo diễn là ai...) ===
    const hasPronounRef = ['phim do', 'phim nay', 'bo phim do', 'phim day', 'bo phim nay', 'no chieu', 'no gia ve', 'no la phim', 'no dao dien', 'noi dung phim do'].some(kw => normalizedMessage.includes(kw));
    const lastMovies = lastSuggestedMoviesMap.get(effectiveSessionId) || [];
    const isImplicitFollowUp = isImplicitContextualFollowUp(normalizedMessage, lastMovies);

    if ((hasPronounRef || isImplicitFollowUp) && intent !== 'movie_ordinal_query') {

      if (lastMovies.length === 1) {
        // Rõ ràng 100%: Chỉ có đúng 1 phim được nhắc trước đó
        const targetMovie = lastMovies[0];
        matchedMovies = [targetMovie];
        const subIntent = detectContextualSubIntent(normalizedMessage);

        if (subIntent === 'showtime') {
          const showtimes = await getShowtimesByMovieId(targetMovie._id);
          reply = buildMovieShowtimeDetailResponse(targetMovie, showtimes);
        } else if (subIntent === 'ticket_price') {
          const priceData = await getTicketPricesFromDB();
          reply = buildTicketPriceForMovieResponse(targetMovie, priceData);
        } else if (subIntent === 'director' || subIntent === 'actor' || subIntent === 'director_actor') {
          reply = buildMovieDirectorActorDetailResponse(targetMovie, subIntent);
        } else {
          reply = buildMovieInfoResponse(targetMovie, targetMovie.movieName);
        }
      } else if (lastMovies.length > 1) {
        // Ngữ cảnh chưa rõ ràng (nhiều hơn 1 phim) -> Hỏi lại người dùng làm rõ
        reply = buildAmbiguousMovieClarificationResponse(lastMovies);
        matchedMovies = lastMovies;
      } else {
        // Chưa có lịch sử gợi ý phim trước đó -> lấy danh sách phim hot hôm nay để hỏi lại
        const todayMovies = await getTodayShowtimes(5);
        if (todayMovies.length > 0) {
          reply = buildAmbiguousMovieClarificationResponse(todayMovies);
          matchedMovies = todayMovies;
        } else {
          reply = buildFallbackResponse();
        }
      }

      if (reply) {
        saveMessageToHistory(effectiveSessionId, 'bot', reply);
        const cleanMovies = matchedMovies.map(sanitizeMovieForResponse).filter(Boolean);
        // CHỈ cập nhật cache nếu phản hồi trả về nhiều hơn 1 phim
        if (cleanMovies.length > 1) {
          lastSuggestedMoviesMap.set(effectiveSessionId, cleanMovies);
        }
        return res.json({
          success: true,
          reply,
          matchedIntent: 'contextual_pronoun_resolution',
          matchedMovies: cleanMovies
        });
      }
    }
    // === HẾT XỬ LÝ CONTEXTUAL PRONOUN ===

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
        const lastMovies = lastSuggestedMoviesMap.get(effectiveSessionId) || [];
        let ordinalIndex = classification.ordinalIndex;

        if (lastMovies.length > 0) {
          if (ordinalIndex === -1) {
            ordinalIndex = lastMovies.length;
          }

          if (ordinalIndex > lastMovies.length || ordinalIndex < 1) {
            reply = `📌 Danh sách vừa gợi ý chỉ có ${lastMovies.length} bộ phim, không có phim thứ ${ordinalIndex} đâu bạn ơi 😅\n\n👉 Bạn nhắn tên phim hoặc số thứ tự từ 1 đến ${lastMovies.length} để mình hỗ trợ nhé! 😉`;
            break;
          }

          const targetMovie = lastMovies[ordinalIndex - 1];
          if (targetMovie) {
            matchedMovies = [targetMovie];
            const subIntent = detectContextualSubIntent(normalizedMessage);

            if (subIntent === 'showtime') {
              const showtimes = await getShowtimesByMovieId(targetMovie._id);
              reply = buildMovieShowtimeDetailResponse(targetMovie, showtimes);
            } else if (subIntent === 'ticket_price') {
              const priceData = await getTicketPricesFromDB();
              reply = buildTicketPriceForMovieResponse(targetMovie, priceData);
            } else if (subIntent === 'director' || subIntent === 'actor' || subIntent === 'director_actor') {
              reply = buildMovieDirectorActorDetailResponse(targetMovie, subIntent);
            } else {
              reply = buildMovieInfoResponse(targetMovie, targetMovie.movieName);
            }
          }
        } else {
          // Chưa có danh sách gợi ý trong session
          const todayMovies = await getTodayShowtimes(5);
          if (ordinalIndex === -1) ordinalIndex = todayMovies.length;
          if (ordinalIndex > todayMovies.length || ordinalIndex < 1) {
            reply = `📌 Danh sách hiện tại chỉ có ${todayMovies.length} bộ phim, không có phim thứ ${ordinalIndex} đâu bạn ơi 😅`;
          } else {
            const targetMovie = todayMovies[ordinalIndex - 1];
            if (targetMovie) {
              matchedMovies = [targetMovie];
              reply = buildMovieInfoResponse(targetMovie, targetMovie.movieName);
            } else {
              reply = buildFallbackResponse();
            }
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
          isUpcomingOnly: true,
          createdAt: Date.now()
        });
        reply = buildCinemaSelectionRequest(cinemaList);
        break;
      }

      case 'schedule_query': {
        const targetDate = classification.targetDate || null;
        const dateTitle = classification.dateTitle || 'hôm nay';
        const cinemaList = await getCinemasFromDB();
        pendingContextMap.set(effectiveSessionId, {
          step: 'awaiting_cinema_selection',
          cinemaList,
          targetDate,
          dateTitle,
          isUpcomingOnly: false,
          createdAt: Date.now()
        });
        reply = buildCinemaSelectionRequest(cinemaList);
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
        const matchResult = await getMovieDetailMatches(searchQuery);

        if (matchResult.status === 'single') {
          const movie = matchResult.movie;
          matchedMovies = [movie];
          reply = buildMovieInfoResponse(movie, searchQuery);
        } else if (matchResult.status === 'multiple') {
          matchedMovies = matchResult.movies;
          reply = buildAmbiguousMovieClarificationResponse(matchResult.movies);
        } else {
          reply = buildMovieInfoResponse(null, searchQuery);
        }
        break;
      }

      case 'movie_info_query_or_fallback': {
        const searchQuery = classification.searchQuery || normalizedMessage;
        const matchResult = await getMovieDetailMatches(searchQuery);

        if (matchResult.status === 'single') {
          const movie = matchResult.movie;
          matchedMovies = [movie];
          reply = buildMovieInfoResponse(movie, searchQuery);
        } else if (matchResult.status === 'multiple') {
          matchedMovies = matchResult.movies;
          reply = buildAmbiguousMovieClarificationResponse(matchResult.movies);
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

    // Lưu danh sách/bộ phim vừa gợi ý hoặc tra cứu vào session context để dùng cho các lượt tra cứu tiếp theo
    // Ghi đè toàn bộ cache với kết quả phim mới nhất bất kể 1 hay nhiều phim (movie_info_query, mood_query...)
    if (cleanMatchedMovies.length > 0 && intent !== 'contextual_pronoun_resolution') {
      lastSuggestedMoviesMap.set(effectiveSessionId, cleanMatchedMovies);
    }

    // 6. Lưu câu trả lời của bot vào history
    saveMessageToHistory(effectiveSessionId, 'bot', reply);

    // 7. Trả về JSON cho client
    const resPayload = {
      success: true,
      reply,
      matchedIntent: intent,
      matchedMovies: cleanMatchedMovies
    };

    if (['upcoming_showtime_query', 'schedule_query'].includes(intent)) {
      const activePendingCtx = pendingContextMap.get(effectiveSessionId);
      const currentCinemaList = activePendingCtx?.cinemaList || [];
      if (currentCinemaList.length > 0) {
        resPayload.cinemaOptions = currentCinemaList.map(c => ({
          _id: c._id,
          cinemaName: c.cinemaName,
          address: c.address || '',
          city: c.city || ''
        }));
      }
    }

    return res.json(resPayload);

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

// === MỚI: Tích hợp Multer và Google Generative AI (Gemini) ===
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 }
});

/**
 * POST /api/chat/vision
 * Nhận hình ảnh, dùng Gemini lấy tên phim (Xử lý trực tiếp trên Bộ nhớ RAM, không lưu đĩa)
 */
router.post('/vision', upload.single('image'), async (req, res) => {
  try {
    if (!req.file || !req.file.buffer) {
      return res.status(400).json({ success: false, message: 'Không tìm thấy file ảnh đính kèm.' });
    }

    const apiKey = (process.env.GEMINI_API_KEY || process.env.GOOGLE_CALENDAR_API_KEY || '').trim();
    const prompt = `Đây là hình ảnh (poster/ảnh chụp màn hình) của một bộ phim.
Bạn hãy cố gắng nhận diện và trả về DUY NHẤT TÊN CỦA BỘ PHIM ĐÓ (bằng tiếng Việt hoặc tiếng Anh tuỳ theo poster).
Tuyệt đối KHÔNG trả lời thêm bất kỳ câu nào khác ngoài tên phim.
Nếu không nhận diện được, hãy trả về chuỗi "UNKNOWN".`;

    const mimeType = req.file.mimetype || 'image/jpeg';
    const base64Data = req.file.buffer.toString("base64");
    let text = null;

    const modelNames = ["gemini-flash-latest", "gemini-3.5-flash", "gemini-3.6-flash", "gemini-2.5-flash", "gemini-2.0-flash"];
    let lastErr = null;

    for (const modelName of modelNames) {
      try {
        const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${apiKey}`;
        const headers = { 
          'Content-Type': 'application/json',
          'x-goog-api-key': apiKey
        };

        const body = {
          contents: [
            {
              parts: [
                { text: prompt },
                {
                  inline_data: {
                    mime_type: mimeType,
                    data: base64Data
                  }
                }
              ]
            }
          ]
        };

        const apiRes = await fetch(url, {
          method: 'POST',
          headers,
          body: JSON.stringify(body)
        });

        const data = await apiRes.json();
        if (apiRes.ok && data?.candidates?.[0]?.content?.parts?.[0]?.text) {
          text = data.candidates[0].content.parts[0].text.trim();
          break;
        } else if (data?.error) {
          console.warn(`[Vision API] Model '${modelName}' returned error (${apiRes.status}):`, data.error.message || data.error);
          lastErr = new Error(`[${modelName}] ${data.error.message || JSON.stringify(data.error)}`);
        }
      } catch (err) {
        console.warn(`[Vision API] Model '${modelName}' fetch exception:`, err.message);
        lastErr = err;
      }
    }

    // Backup: Thử dùng SDK nếu fetch trực tiếp không lấy được
    if (!text) {
      try {
        const genAI = new GoogleGenerativeAI(apiKey);
        const model = genAI.getGenerativeModel({ model: "gemini-flash-latest" });
        const result = await model.generateContent([prompt, { inlineData: { data: base64Data, mimeType } }]);
        const response = await result.response;
        text = response.text().trim();
      } catch (sdkErr) {
        console.error(`[Vision API SDK Error]:`, sdkErr.message);
        throw lastErr || sdkErr;
      }
    }

    if (!text || text === "UNKNOWN") {
      return res.json({ success: false, text: null, message: 'AI không nhận ra phim nào trong ảnh.' });
    }

    // Tiền xử lý: Xoá ký tự markdown, ngoặc kép, và tiền tố thừa (như "Tên phim:")
    let cleanText = text.replace(/[*"`]/g, '').trim();
    cleanText = cleanText.replace(/^(Tên phim|Bộ phim|Tên bộ phim|Movie title|Title):\s*/i, '').trim();
    // Xoá các câu chú thích phía sau nếu Gemini tự ý viết thành nhiều dòng
    cleanText = cleanText.split('\n')[0].trim();
    
    // Cắt bỏ phần phụ đề sau dấu gạch ngang (nếu có)
    cleanText = cleanText.split('-')[0].trim();

    return res.json({ success: true, text: cleanText });

  } catch (error) {
    console.error('[Vision API Error]:', error?.message || error);
    if (error?.message?.includes('API_KEY_SERVICE_BLOCKED') || error?.status === 403) {
      return res.json({ 
        success: false, 
        message: 'API Key Gemini chưa được bật hoặc bị chặn dịch vụ. Vui lòng cập nhật GEMINI_API_KEY hợp lệ trong file .env.' 
      });
    }
    return res.status(500).json({ success: false, message: 'Lỗi khi xử lý hình ảnh.' });
  }
});

