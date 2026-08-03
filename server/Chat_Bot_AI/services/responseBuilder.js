/**
 * Module Xây Dựng Câu Trả Lời (Response Builder) Cho Chatbot AI
 * Chịu trách nhiệm format văn bản phản hồi tiếng Việt đẹp mắt, rõ ràng với Emoji và Markdown.
 * Đã hỗ trợ dữ liệu thật từ MongoDB Atlas cho Giá vé (Showtime/Seat) & Promotion.
 */

/**
 * Phản hồi khi vi phạm chính sách bảo mật
 * @returns {string}
 */
export const buildSecurityViolationResponse = () => {
  return `🛡️ **CẢNH BÁO BẢO MẬT HỆ THỐNG** 🛡️\n\n` +
    `Vì lý do an toàn và bảo mật thông tin, mình không thể cung cấp các dữ liệu nội bộ như mật khẩu admin, báo cáo doanh thu, chuỗi kết nối CSDL hoặc thông tin nhân viên rạp.\n\n` +
    `Bạn vui lòng đặt các câu hỏi liên quan đến **lịch chiếu phim, giá vé, chương trình khuyến mãi** hoặc **hướng dẫn đặt vé** nhé! 😉`;
};

/**
 * Phản hồi khi người dùng chào hỏi
 * @returns {string}
 */
export const buildGreetingResponse = () => {
  return `👋 **Xin chào bạn! Mình là Trợ Lý AI của Rạp Phim.** 🎬\n\n` +
    `Mình có thể hỗ trợ bạn các thông tin sau:\n` +
    `• 🍿 **Lịch chiếu phim:** "Hôm nay có phim gì chiếu?", "Lịch chiếu ngày mai"\n` +
    `• 🎟️ **Bảng giá vé:** "Giá vé xem phim bao nhiêu tiền?"\n` +
    `• 🎁 **Ưu đãi & Khuyến mãi:** "Có voucher giảm giá nào không?"\n` +
    `• 🎭 **Gợi ý theo tâm trạng:** "Tôi đang buồn muốn xem phim nhẹ nhàng"\n` +
    `• 📝 **Thông tin phim:** "Cho mình xem thông tin phim Conan"\n` +
    `• 🛒 **Hướng dẫn đặt vé:** "Cách đặt vé trên ứng dụng thế nào?"\n\n` +
    `Bạn cần mình hỗ trợ thông tin gì ngay bây giờ ạ? 😊`;
};

/**
 * Phản hồi nhận diện danh tính AI Chatbot
 * @returns {string}
 */
export const buildBotIdentityResponse = () => {
  return `🤖 **MÌNH LÀ TRỢ LÝ AI RẠP PHIM** 🎬\n\n` +
    `Đúng vậy, mình là Trợ Lý Trí Tuệ Nhân Tạo (AI Chatbot) được thiết kế riêng cho ứng dụng rạp chiếu phim!\n\n` +
    `Mình luôn sẵn sàng giúp bạn các tiện ích sau:\n` +
    `• 🍿 Tra cứu lịch chiếu & suất chiếu phim hot\n` +
    `• 🎟️ Xem chi tiết bảng giá vé & loại ghế\n` +
    `• 🎁 Tìm mã voucher giảm giá & chương trình khuyến mãi\n` +
    `• 🎭 Gợi ý phim theo tâm trạng hoặc thể loại yêu thích\n\n` +
    `Bạn muốn tìm hiểu thông tin gì về phim rạp ngay lúc này không ạ? 😊`;
};

/**
 * Phản hồi thông tin giá vé (Dữ liệu từ CSDL MongoDB hoặc fallback)
 * @param {Object|null} ticketPriceData - Dữ liệu giá vé động truy vấn từ DB
 * @returns {string}
 */
export const buildTicketPriceResponse = (ticketPriceData = null) => {
  let reply = `🎟️ **THÔNG TIN GIÁ VÉ RẠP PHIM** 🎟️\n\n`;

  if (ticketPriceData && (ticketPriceData.showtimePrices?.length > 0 || Object.keys(ticketPriceData.seatPriceMap || {}).length > 0)) {
    if (ticketPriceData.showtimePrices?.length > 0) {
      const minP = Math.min(...ticketPriceData.showtimePrices);
      const maxP = Math.max(...ticketPriceData.showtimePrices);
      const minStr = minP.toLocaleString('vi-VN');
      const maxStr = maxP.toLocaleString('vi-VN');
      const priceText = minP === maxP ? `${minStr} VNĐ` : `${minStr} VNĐ - ${maxStr} VNĐ`;
      reply += `• **Giá vé suất chiếu:** ${priceText} / vé\n`;
    }

    if (ticketPriceData.seatPriceMap && Object.keys(ticketPriceData.seatPriceMap).length > 0) {
      reply += `🔹 **Giá vé theo loại ghế:**\n`;
      for (const [type, prices] of Object.entries(ticketPriceData.seatPriceMap)) {
        const uniqueP = Array.from(new Set(prices)).sort((a, b) => a - b);
        const pStr = uniqueP.map(p => p.toLocaleString('vi-VN') + ' VNĐ').join(' - ');
        reply += `  - Ghế ${type}: ${pStr}\n`;
      }
    }
    reply += `\n*Giá vé thực tế có thể tùy thuộc vào suất chiếu và loại ghế bạn chọn trên sơ đồ phòng chiếu.* 😉`;
  } else {
    reply += `• **Vé 2D (T2 - T5):** 75.000 VNĐ / vé (Học sinh/Sinh viên: 60.000 VNĐ)\n` +
      `• **Vé 2D Cuối tuần (T6 - CN) & Ngày lễ:** 90.000 VNĐ / vé\n` +
      `• **Vé 3D / Công nghệ:** 110.000 VNĐ - 140.000 VNĐ / vé\n` +
      `• **Ghế đôi / Ghế VIP:** Phụ thu từ 15.000 - 30.000 VNĐ / ghế\n\n` +
      `*Giá vé thực tế có thể thay đổi tùy thuộc vào suất chiếu và rạp chọn. Bạn có thể kiểm tra giá chính xác khi chọn ghế trên app nhé! 😉*`;
  }

  return reply;
};

/**
 * Phản hồi thông tin chương trình khuyến mãi / ưu đãi (Dữ liệu thực tế từ DB model Promotion)
 * @param {Array} promotions - Danh sách các promotion đang active từ DB
 * @returns {string}
 */
export const buildPromotionResponse = (promotions = []) => {
  let reply = `🎁 **CHƯƠNG TRÌNH KHUYẾN MÃI & ƯU ĐÃI** 🎁\n\n`;

  if (promotions && promotions.length > 0) {
    promotions.forEach((promo, idx) => {
      reply += `📌 **${idx + 1}. ${promo.title}**`;
      if (promo.couponCode) reply += ` (Mã: **${promo.couponCode}**)`;
      reply += `\n`;

      if (promo.detail) reply += `• **Chi tiết:** ${promo.detail}\n`;

      if (promo.discountValue) {
        const valStr = promo.discountType === 'percentage' 
          ? `${promo.discountValue}%` 
          : `${promo.discountValue.toLocaleString('vi-VN')} VNĐ`;
        reply += `• **Mức ưu đãi:** Giảm ${valStr}\n`;
      }

      if (promo.minOrderValue) {
        reply += `• **Đơn tối thiểu:** ${promo.minOrderValue.toLocaleString('vi-VN')} VNĐ\n`;
      }

      if (promo.endDate) {
        const endD = new Date(promo.endDate);
        reply += `• **Hạn sử dụng:** ${endD.getDate()}/${endD.getMonth() + 1}/${endD.getFullYear()}\n`;
      }
      reply += `\n`;
    });
    reply += `👉 Bạn hãy áp dụng Mã Voucher khi thanh toán tại bước Đặt Vé để nhận ưu đãi nhé! 🍿🎉`;
  } else {
    reply += `📌 **1. Ưu Đãi HSSV - GIẢM 20%** (Mã: **HSSV20**)\n` +
      `• **Chi tiết:** Giảm 20% cho sinh viên khi xem phim.\n\n` +
      `📌 **2. WELCOME NEW MEMBER - 50K** (Mã: **WELCOME50**)\n` +
      `• **Chi tiết:** Giảm ngay 50.000 VNĐ cho thành viên mới đăng ký.\n\n` +
      `👉 Bạn hãy áp dụng Mã Voucher khi thanh toán tại bước Đặt Vé để nhận ưu đãi nhé! 🍿🎉`;
  }

  return reply;
};

/**
 * Phản hồi danh sách phim gợi ý theo tâm trạng (mood)
 * @param {Array} movies - Danh sách các phim thỏa mãn mood
 * @param {Array} matchedMoods - Danh sách các mood key được nhận diện
 * @returns {string}
 */
export const buildMoodResponse = (movies = [], matchedMoods = []) => {
  if (!movies || movies.length === 0) {
    return `😢 Rất tiếc, hiện tại rạp chưa tìm thấy bộ phim phù hợp nhất với tâm trạng này của bạn. ` +
      `Bạn có thể tham khảo danh sách phim đang chiếu hôm nay bằng cách nhắn "xem lịch chiếu" nhé! 🍿`;
  }

  const moodLabels = {
    buon: 'Buồn / Thất tình / Tâm trạng',
    nhe_nhang: 'Nhẹ nhàng / Thư giãn / Lãng mạn',
    hoi_hop: 'Hồi hộp / Kinh dị / Bí ẩn',
    vui_ve: 'Vui vẻ / Hài hước / Giải trí',
    hanh_dong: 'Hành động / Kịch tính / Cháy nổ'
  };

  const selectedLabels = matchedMoods.map(m => moodLabels[m] || m).join(', ');

  let reply = `Chào bạn! Cảm ơn bạn đã chia sẻ. Nếu bạn đang muốn xem phim với tâm trạng **${selectedLabels}**, mình xin gợi ý các bộ phim phù hợp nhất dành cho bạn:\n\n`;

  movies.forEach((m, idx) => {
    reply += `🎬 **${idx + 1}. ${m.movieName}**`;
    if (m.rating) reply += ` ⭐ ${m.rating}/5`;
    reply += `\n`;
    if (m.movieGenre && m.movieGenre.length > 0) reply += `• **Thể loại:** ${m.movieGenre.join(', ')}\n`;
    if (m.duration) reply += `• **Thời lượng:** ${m.duration} phút\n`;
    if (m.censorship) reply += `• **Độ tuổi:** ${m.censorship}\n`;
    if (m.description) {
      const shortDesc = m.description.length > 120 ? m.description.substring(0, 120) + '...' : m.description;
      reply += `• **Tóm tắt:** ${shortDesc}\n`;
    }
    reply += `\n`;
  });

  reply += `Bạn có muốn xem lịch chiếu chi tiết hoặc đặt vé cho bộ phim nào ở trên không ạ? 😊`;
  return reply;
};

/**
 * Phản hồi danh sách lịch chiếu phim hôm nay (hoặc mốc ngày chỉ định)
 * @param {Array} moviesWithShowtimes - Danh sách phim kèm array showtimes
 * @param {string} dateTitle - Nhãn ngày (vd: 'HÔM NAY', 'NGÀY MAI', 'CUỐI TUẦN')
 * @param {string} cinemaName - Tên rạp đã chọn (tùy chọn)
 * @returns {string}
 */
export const buildScheduleResponse = (moviesWithShowtimes = [], dateTitle = 'HÔM NAY', cinemaName = '') => {
  const cinemaHeader = cinemaName ? ` TẠI ${cinemaName.toUpperCase()}` : '';

  if (!moviesWithShowtimes || moviesWithShowtimes.length === 0) {
    return `📅 Rất tiếc, rạp hiện chưa cập nhật lịch chiếu cho (${dateTitle.toUpperCase()})${cinemaHeader}. Vui lòng quay lại sau hoặc chọn mốc ngày khác bạn nhé! 🍿`;
  }

  let reply = `🎬 **DANH SÁCH PHIM VÀ LỊCH CHIẾU (${dateTitle.toUpperCase()})${cinemaHeader}** 🍿\n\n`;

  moviesWithShowtimes.forEach((m, idx) => {
    reply += `📌 **${idx + 1}. ${m.movieName}**\n`;
    if (m.movieGenre && m.movieGenre.length > 0) reply += `• **Thể loại:** ${m.movieGenre.join(', ')}\n`;
    if (m.duration) reply += `• **Thời lượng:** ${m.duration} phút\n`;
    
    if (m.showtimes && m.showtimes.length > 0) {
      reply += `• **Suất chiếu:** ${m.showtimes.join(' | ')}\n`;
    } else {
      reply += `• **Suất chiếu:** Chưa có suất chiếu mở bán trong ngày\n`;
    }
    reply += `\n`;
  });

  reply += `Bạn muốn xem chi tiết bộ phim nào thì cứ nhắn tên phim cho mình nhé! 😉`;
  return reply;
};

/**
 * Phản hồi chi tiết 1 bộ phim
 * @param {Object|null} movie - Thông tin phim
 * @param {string} searchQuery - Tên phim tìm kiếm
 * @returns {string}
 */
export const buildMovieInfoResponse = (movie = null, searchQuery = '') => {
  if (!movie) {
    return `🔍 Rất tiếc, mình không tìm thấy bộ phim nào có tên "${searchQuery}" trong hệ thống rạp. ` +
      `Bạn có thể nhắn "hôm nay có phim gì" để xem danh sách phim đang chiếu nhé! 🍿`;
  }

  let reply = `🎥 **THÔNG TIN PHIM: ${movie.movieName.toUpperCase()}**\n\n`;
  if (movie.movieGenre && movie.movieGenre.length > 0) reply += `• **Thể loại:** ${movie.movieGenre.join(', ')}\n`;
  if (movie.duration) reply += `• **Thời lượng:** ${movie.duration} phút\n`;
  if (movie.censorship) reply += `• **Độ tuổi:** ${movie.censorship}\n`;
  if (movie.language) reply += `• **Ngôn ngữ:** ${movie.language}\n`;
  if (movie.country) reply += `• **Quốc gia:** ${movie.country}\n`;
  if (movie.director) reply += `• **Đạo diễn:** ${movie.director}\n`;
  if (movie.actor && movie.actor.length > 0) reply += `• **Diễn viên:** ${movie.actor.join(', ')}\n`;
  
  reply += `\n📝 **Nội dung:** ${movie.description || 'Đang cập nhật...'}\n\n`;
  reply += `Bạn có muốn mình hỗ trợ xem lịch chiếu của phim này không ạ? 🎟️`;

  return reply;
};

/**
 * Phản hồi danh sách suất chiếu của duy nhất một bộ phim theo các rạp
 * @param {Object} movie - Object thông tin phim
 * @param {Array} showtimes - Danh sách suất chiếu từ DB
 * @returns {string}
 */
export const buildMovieShowtimeDetailResponse = (movie, showtimes = []) => {
  if (!movie) return buildFallbackResponse();

  let reply = `🎬 **LỊCH CHIẾU PHIM: ${movie.movieName.toUpperCase()}** 🍿\n\n`;
  if (movie.duration) reply += `• **Thời lượng:** ${movie.duration} phút\n`;
  if (movie.movieGenre && movie.movieGenre.length > 0) reply += `• **Thể loại:** ${movie.movieGenre.join(', ')}\n`;

  if (!showtimes || showtimes.length === 0) {
    reply += `\n📅 Rất tiếc, bộ phim **"${movie.movieName}"** hiện chưa có suất chiếu tiếp theo được xếp lịch. Vui lòng tham khảo các phim khác nhé! 🎟️`;
    return reply;
  }

  // Nhóm suất chiếu theo từng rạp
  const cinemaMap = new Map();
  showtimes.forEach(st => {
    const cinemaName = st.cinemaId?.cinemaName || 'Rạp chiếu';
    if (!cinemaMap.has(cinemaName)) {
      cinemaMap.set(cinemaName, []);
    }
    let timeStr = st.showTime || '';
    if (!timeStr && st.startTime) {
      timeStr = new Date(st.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Ho_Chi_Minh' });
    }
    if (timeStr && !cinemaMap.get(cinemaName).includes(timeStr)) {
      cinemaMap.get(cinemaName).push(timeStr);
    }
  });

  reply += `\n📌 **Danh sách suất chiếu theo rạp:**\n\n`;
  for (const [cinemaName, times] of cinemaMap.entries()) {
    reply += `🎭 **${cinemaName}**\n`;
    reply += `• **Giờ chiếu:** ${times.join(' | ')}\n\n`;
  }

  reply += `👉 Bạn nhắn *"đặt vé"* để được hướng dẫn mua vé cho suất chiếu này nhé! 😉`;
  return reply;
};

/**
 * Phản hồi giá vé riêng cho bộ phim được chỉ định
 * @param {Object} movie - Phim cần tra cứu
 * @param {Object} ticketPriceData - Dữ liệu giá vé
 * @returns {string}
 */
export const buildTicketPriceForMovieResponse = (movie, ticketPriceData) => {
  if (!movie) return buildFallbackResponse();
  let reply = `🎫 **BẢNG GIÁ VÉ XEM PHIM: ${movie.movieName.toUpperCase()}** 🍿\n\n`;
  reply += `• **Giá vé tiêu chuẩn:** ${ticketPriceData?.formattedShowtimePrices || '75.000 VNĐ - 110.000 VNĐ / vé'}\n`;
  if (ticketPriceData?.formattedSeatPrices) {
    reply += `• **Theo loại ghế:** ${ticketPriceData.formattedSeatPrices}\n`;
  }
  reply += `\n💡 Giá vé được áp dụng cho bộ phim **${movie.movieName}** tại tất cả các rạp chiếu.`;
  return reply;
};

/**
 * Phản hồi chi tiết Đạo diễn / Diễn viên của duy nhất một bộ phim cụ thể
 * @param {Object} movie - Object thông tin phim
 * @param {'director'|'actor'|'all'} queryType - Loại thông tin tra cứu
 * @returns {string}
 */
export const buildMovieDirectorActorDetailResponse = (movie, queryType = 'all') => {
  if (!movie) return buildFallbackResponse();

  const directorStr = Array.isArray(movie.director) ? movie.director.join(', ') : (movie.director || 'Đang cập nhật');
  const actorList = movie.actors || movie.actor;
  const actorStr = Array.isArray(actorList) ? actorList.join(', ') : (actorList || 'Đang cập nhật');

  let reply = '';
  if (queryType === 'director') {
    reply = `🎬 **ĐẠO DIỄN PHIM: ${movie.movieName.toUpperCase()}** 🎥\n\n`;
    reply += `• **Đạo diễn:** ${directorStr}\n`;
    if (actorStr !== 'Đang cập nhật') {
      reply += `• **Dàn diễn viên chính:** ${actorStr}\n`;
    }
  } else if (queryType === 'actor') {
    reply = `🎭 **DÀN DIỄN VIÊN PHIM: ${movie.movieName.toUpperCase()}** 🌟\n\n`;
    reply += `• **Diễn viên:** ${actorStr}\n`;
    if (directorStr !== 'Đang cập nhật') {
      reply += `• **Đạo diễn:** ${directorStr}\n`;
    }
  } else {
    reply = `🎬 **ĐẠO DIỄN & DIỄN VIÊN PHIM: ${movie.movieName.toUpperCase()}** 🍿\n\n`;
    reply += `• **Đạo diễn:** ${directorStr}\n`;
    reply += `• **Dàn diễn viên:** ${actorStr}\n`;
  }

  if (movie.duration) reply += `• **Thời lượng:** ${movie.duration} phút\n`;
  if (movie.movieGenre && movie.movieGenre.length > 0) reply += `• **Thể loại:** ${movie.movieGenre.join(', ')}\n`;

  reply += `\nBạn có muốn xem lịch chiếu hoặc giá vé của phim này không ạ? 🎟️`;
  return reply;
};

/**
 * Phản hồi hỏi lại người dùng làm rõ khi ngữ cảnh có nhiều phim (Ambiguous Context)
 * @param {Array} movies - Danh sách phim vừa gợi ý
 * @returns {string}
 */
export const buildAmbiguousMovieClarificationResponse = (movies = []) => {
  if (!movies || movies.length === 0) {
    return buildFallbackResponse();
  }

  let reply = `😊 **Bạn đang muốn tra cứu thông tin / suất chiếu của bộ phim nào trong danh sách vừa gợi ý ạ?** 🍿\n\n`;
  movies.forEach((m, idx) => {
    reply += `📌 **${idx + 1}. ${m.movieName}**`;
    if (m.movieGenre && m.movieGenre.length > 0) reply += ` — ${m.movieGenre.join(', ')}`;
    reply += `\n`;
  });
  reply += `\n👉 Bạn nhắn **tên phim** hoặc **số thứ tự** (ví dụ: *"phim thứ 1"*, *"phim số 2"*) để mình hỗ trợ thông tin chính xác cho bạn nhé! 😉`;
  return reply;
};

/**
 * Phản hồi hướng dẫn đặt vé quy trình
 * @returns {string}
 */
export const buildBookingGuidanceResponse = () => {
  return `🛒 **HƯỚNG DẪN QUY TRÌNH ĐẶT VÉ PHIM TRÊN ỨNG DỤNG** 🎟️\n\n` +
    `Để đặt vé xem phim nhanh chóng, bạn thực hiện theo 4 bước sau:\n` +
    `• **Bước 1:** Chọn bộ phim bạn muốn xem trên danh sách Phim Đang Chiếu.\n` +
    `• **Bước 2:** Chọn Cụm Rạp, Ngày chiếu và Khung giờ suất chiếu phù hợp.\n` +
    `• **Bước 3:** Chọn vị trí ghế ngồi (Ghế Thường, VIP, Ghế Đôi) trên sơ đồ phòng chiếu.\n` +
    `• **Bước 4:** Áp dụng Mã Giảm Giá (nếu có) và tiến hành Thanh Toán an toàn.\n\n` +
    `🎟️ **Vé điện tử & Mã QR:** Sau khi hoàn tất đặt vé, mã QR Code vé xem phim sẽ hiển thị tại mục **"Vé của tôi"** trên ứng dụng. Bạn chỉ cần xuất trình mã QR này tại quầy rạp để vào phòng chiếu! 🍿🎉`;
};

/**
 * Phản hồi khi phim đã hết chỗ / sold out hoặc chưa có lịch mở bán
 * @param {string} movieName - Tên bộ phim
 * @returns {string}
 */
export const buildSoldOutResponse = (movieName = '') => {
  return `🚫 **THÔNG BÁO HẾT VÉ / RẤT TIẾC PHIM ĐÃ HẾT GHẾ TRỐNG** 🚫\n\n` +
    `Phim **"${movieName}"** hiện tại chưa có lịch mở bán hoặc đã HẾT SẠCH VÉ cho suất chiếu hôm nay.\n\n` +
    `Bạn vui lòng tham khảo suất chiếu các ngày tiếp theo hoặc chọn các bộ phim HOT khác đang có sẵn nhé! 🍿`;
};

/**
 * Phản hồi mặc định khi không hiểu ý định người dùng (Fallback)
 * @returns {string}
 */
export const buildFallbackResponse = () => {
  return `🤖 Rất tiếc, mình chưa hiểu rõ yêu cầu của bạn.\n\n` +
    `Bạn có thể thử hỏi mình các nội dung như:\n` +
    `• *"Hôm nay có phim gì chiếu?"*\n` +
    `• *"Cho xem bảng giá vé xem phim"* \n` +
    `• *"Có khuyến mãi gì mới không?"*\n` +
    `• *"Gợi ý phim hành động hài hước"*`;
};

/**
 * Phản hồi hệ thống/CSDL bảo trì sự cố (HTTP 500 UX handling)
 * @returns {string}
 */
export const buildSystemMaintenanceResponse = () => {
  return `⚠️ **HỆ THỐNG ĐANG BẢO TRÌ TẠM THỜI** ⚠️\n\n` +
    `Rất xin lỗi bạn, dịch vụ tra cứu thông tin chatbot đang bảo trì hoặc gặp chút gián đoạn nâng cấp CSDL.\n` +
    `Bạn vui lòng thử lại sau ít phút hoặc liên hệ trực tiếp tổng đài hỗ trợ rạp phim nhé! 🙏`;
};

/**
 * Phản hồi yêu cầu người dùng chọn rạp (bước 1 của luồng upcoming_showtime)
 * @param {Array} cinemas - Danh sách rạp từ DB: [{_id, cinemaName, address, city}]
 * @returns {string}
 */
export const buildCinemaSelectionRequest = (cinemas = []) => {
  if (!cinemas || cinemas.length === 0) {
    return `😔 Hiện tại hệ thống chưa có thông tin rạp chiếu phim nào đang hoạt động.\n` +
      `Bạn vui lòng thử lại sau hoặc liên hệ hotline rạp để được hỗ trợ nhé! 🙏`;
  }

  let reply = `🏟️ **BẠN MUỐN XEM LỊCH CHIẾU TẠI RẠP NÀO?** 🎬\n\n`;
  reply += `Vui lòng bấm chọn rạp chiếu bên dưới để xem danh sách suất chiếu nhé:`;

  return reply;
};

/**
 * Phản hồi danh sách phim sắp chiếu từ BÂY GIỜ đến hết ngày theo rạp đã chọn
 * @param {Array} moviesWithShowtimes - Danh sách phim kèm showtimes sắp tới
 * @param {string} cinemaName - Tên rạp đã chọn
 * @param {Date} fromTime - Mốc thời gian "từ bây giờ" để hiển thị
 * @returns {string}
 */
export const buildUpcomingShowtimeResponse = (moviesWithShowtimes = [], cinemaName = '', fromTime = new Date()) => {
  const timeStr = fromTime.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Asia/Ho_Chi_Minh'
  });

  if (!moviesWithShowtimes || moviesWithShowtimes.length === 0) {
    return `😔 **Không còn suất chiếu nào từ ${timeStr} đến hết ngày hôm nay** tại **${cinemaName}**.\n\n` +
      `Bạn có thể hỏi lịch chiếu ngày mai bằng cách nhắn _"lịch chiếu ngày mai"_ hoặc chọn rạp khác nhé! 🍿`;
  }

  let reply = `🎬 **PHIM SẮP CHIẾU TẠI ${cinemaName.toUpperCase()}** 🍿\n`;
  reply += `⏰ *(Từ ${timeStr} đến hết ngày hôm nay)*\n\n`;

  moviesWithShowtimes.forEach((m, idx) => {
    reply += `📌 **${idx + 1}. ${m.movieName}**\n`;
    if (m.movieGenre && m.movieGenre.length > 0) reply += `• **Thể loại:** ${m.movieGenre.join(', ')}\n`;
    if (m.duration) reply += `• **Thời lượng:** ${m.duration} phút\n`;
    if (m.censorship) reply += `• **Độ tuổi:** ${m.censorship}\n`;

    if (m.showtimes && m.showtimes.length > 0) {
      reply += `• **🕐 Suất sắp chiếu:** ${m.showtimes.join(' | ')}\n`;
    } else {
      reply += `• **Suất chiếu:** Không còn suất nào trong hôm nay\n`;
    }
    reply += `\n`;
  });

  reply += `💡 Nhắn tên phim để xem chi tiết hoặc nhắn _"đặt vé"_ để được hướng dẫn đặt vé nhé! 🎟️`;
  return reply;
};

/**
 * Phản hồi tư vấn khi người dùng chưa biết muốn xem gì (Recommendation Prompt)
 * @param {Array} movies - Danh sách phim hot để gợi ý
 * @returns {string}
 */
export const buildRecommendationPromptResponse = (movies = []) => {
  let reply = `🤔 **BẠN CHƯA BIẾT NÊN XEM PHIM GÌ? ĐỂ MÌNH GỢI Ý GIÚP NHÉ!** 🍿\n\n`;

  if (movies && movies.length > 0) {
    reply += `🔥 **Top Phim Đang Chiếu Rất Được Yêu Thích:**\n\n`;
    movies.slice(0, 2).forEach((m, idx) => {
      reply += `🎬 **${idx + 1}. ${m.movieName}**`;
      if (m.rating) reply += ` ⭐ ${m.rating}/5`;
      reply += `\n`;
      if (m.movieGenre && m.movieGenre.length > 0) reply += `• **Thể loại:** ${m.movieGenre.join(', ')}\n`;
      if (m.description) {
        const shortDesc = m.description.length > 100 ? m.description.substring(0, 100) + '...' : m.description;
        reply += `• **Tóm tắt:** ${shortDesc}\n`;
      }
      reply += `\n`;
    });
  }

  reply += `💡 **Hoặc bạn có thể cho mình biết gu xem phim nhé:**\n` +
    `• *"Mình thích phim hài hước vui vẻ"*\n` +
    `• *"Gợi ý phim hành động kịch tính"*\n` +
    `• *"Tìm phim tình cảm nhẹ nhàng"*\n` +
    `• *"Phim nào rating cao nhất?"*`;

  return reply;
};

/**
 * Phản hồi danh sách phim có rating cao nhất
 * @param {Array} movies - Danh sách phim rating cao
 * @returns {string}
 */
export const buildTopRatedResponse = (movies = []) => {
  if (!movies || movies.length === 0) {
    return `🌟 Hiện tại chưa có dữ liệu đánh giá phim. Bạn có thể hỏi *"hôm nay có phim gì"* để xem lịch chiếu nhé! 🍿`;
  }

  let reply = `⭐ **TOP PHIM CÓ ĐÁNH GIÁ (RATING) CAO NHẤT RẠP** 🏆\n\n`;

  movies.forEach((m, idx) => {
    reply += `🥇 **${idx + 1}. ${m.movieName.toUpperCase()}** ⭐ **${m.rating || 5.0}/5**\n`;
    if (m.movieGenre && m.movieGenre.length > 0) reply += `• **Thể loại:** ${m.movieGenre.join(', ')}\n`;
    if (m.duration) reply += `• **Thời lượng:** ${m.duration} phút\n`;
    if (m.description) {
      const shortDesc = m.description.length > 110 ? m.description.substring(0, 110) + '...' : m.description;
      reply += `• **Nội dung:** ${shortDesc}\n`;
    }
    reply += `\n`;
  });

  reply += `🎟️ Bạn muốn xem lịch chiếu hoặc thông tin chi tiết phim nào ở trên không ạ? 😊`;
  return reply;
};

/**
 * Phản hồi so sánh hai bộ phim
 * @param {Object|null} movieA
 * @param {Object|null} movieB
 * @param {string} nameA
 * @param {string} nameB
 * @returns {string}
 */
export const buildMovieComparisonResponse = (movieA, movieB, nameA = '', nameB = '') => {
  if (!movieA && !movieB) {
    return `🔍 Rất tiếc, mình không tìm thấy thông tin phim "${nameA}" và "${nameB}" trong CSDL rạp để so sánh. 🍿`;
  }

  if (!movieA || !movieB) {
    const foundMovie = movieA || movieB;
    const missingName = !movieA ? nameA : nameB;
    return `🔍 Mình tìm thấy thông tin phim **"${foundMovie.movieName}"** (Rating: ⭐ ${foundMovie.rating || 'N/A'}/5, Thể loại: ${foundMovie.movieGenre?.join(', ') || 'N/A'}), nhưng chưa có dữ liệu về phim "${missingName}" để so sánh chi tiết. 🍿`;
  }

  let reply = `⚖️ **SO SÁNH HAI BỘ PHIM: ${movieA.movieName.toUpperCase()} vs ${movieB.movieName.toUpperCase()}** 🎬\n\n`;

  reply += `📌 **1. ${movieA.movieName}**\n`;
  reply += `• **Đánh giá:** ⭐ ${movieA.rating || 'N/A'}/5\n`;
  reply += `• **Thể loại:** ${movieA.movieGenre?.join(', ') || 'Chưa cập nhật'}\n`;
  reply += `• **Thời lượng:** ${movieA.duration || 'N/A'} phút\n`;
  reply += `• **Tóm tắt:** ${movieA.description ? movieA.description.substring(0, 100) + '...' : 'N/A'}\n\n`;

  reply += `📌 **2. ${movieB.movieName}**\n`;
  reply += `• **Đánh giá:** ⭐ ${movieB.rating || 'N/A'}/5\n`;
  reply += `• **Thể loại:** ${movieB.movieGenre?.join(', ') || 'Chưa cập nhật'}\n`;
  reply += `• **Thời lượng:** ${movieB.duration || 'N/A'} phút\n`;
  reply += `• **Tóm tắt:** ${movieB.description ? movieB.description.substring(0, 100) + '...' : 'N/A'}\n\n`;

  if (movieA.rating && movieB.rating) {
    if (movieA.rating > movieB.rating) {
      reply += `💡 **Nhận xét:** **"${movieA.movieName}"** đang có điểm đánh giá khán giả cao hơn (⭐ ${movieA.rating} vs ⭐ ${movieB.rating}).`;
    } else if (movieB.rating > movieA.rating) {
      reply += `💡 **Nhận xét:** **"${movieB.movieName}"** đang có điểm đánh giá khán giả cao hơn (⭐ ${movieB.rating} vs ⭐ ${movieA.rating}).`;
    } else {
      reply += `💡 **Nhận xét:** Cả hai bộ phim đều có cùng mức đánh giá ⭐ ${movieA.rating}/5. Tùy thuộc gu thể loại bạn yêu thích nhé!`;
    }
  }

  reply += `\n\nBạn muốn đặt vé cho phim nào trong hai phim này ạ? 🎟️`;
  return reply;
};

/**
 * Phản hồi danh sách phim theo Đạo diễn / Diễn viên
 * @param {Array} movies
 * @param {string} personName
 * @param {string} type - 'director' | 'actor' | 'all'
 * @returns {string}
 */
export const buildDirectorActorResponse = (movies = [], personName = '', type = 'all') => {
  if (!movies || movies.length === 0) {
    return `🔍 Rất tiếc, rạp hiện chưa có bộ phim nào của đạo diễn/diễn viên **"${personName}"** trong danh sách đang chiếu. ` +
      `Bạn có thể tham khảo danh sách phim hot bằng cách nhắn *"hôm nay có phim gì"* nhé! 🍿`;
  }

  const roleTitle = type === 'director' ? 'ĐẠO DIỄN' : type === 'actor' ? 'DIỄN VIÊN' : 'ĐẠO DIỄN / DIỄN VIÊN';
  let reply = `🎬 **DANH SÁCH PHIM CỦA ${roleTitle}: ${personName.toUpperCase()}** 🌟\n\n`;

  movies.forEach((m, idx) => {
    reply += `📌 **${idx + 1}. ${m.movieName}**`;
    if (m.rating) reply += ` ⭐ ${m.rating}/5`;
    reply += `\n`;
    if (m.director) {
      const dStr = Array.isArray(m.director) ? m.director.join(', ') : m.director;
      reply += `• **Đạo diễn:** ${dStr}\n`;
    }
    const actorList = m.actors || m.actor;
    if (actorList && (Array.isArray(actorList) ? actorList.length > 0 : !!actorList)) {
      const aStr = Array.isArray(actorList) ? actorList.join(', ') : actorList;
      reply += `• **Diễn viên:** ${aStr}\n`;
    }
    if (m.movieGenre && m.movieGenre.length > 0) reply += `• **Thể loại:** ${m.movieGenre.join(', ')}\n`;
    reply += `\n`;
  });

  reply += `Bạn có muốn xem lịch chiếu hoặc đặt vé cho bộ phim nào ở trên không ạ? 😊`;
  return reply;
};

/**
 * Phản hồi khi người dùng phủ định (không muốn xem) một thể loại/tâm trạng
 * @param {Array} movies - Danh sách phim thay thế
 * @param {string} negatedMoodLabel - Tên tâm trạng/thể loại bị phủ định
 * @returns {string}
 */
export const buildNegatedMoodResponse = (movies = [], negatedMoodLabel = '') => {
  let reply = `👌 **Dạ mình đã ghi nhận bạn không muốn xem thể loại/tâm trạng ${negatedMoodLabel}.**\n\n`;

  if (movies && movies.length > 0) {
    reply += `🍿 **Dưới đây là các gợi ý phim khác phù hợp dành cho bạn:**\n\n`;
    movies.forEach((m, idx) => {
      reply += `🎬 **${idx + 1}. ${m.movieName}**`;
      if (m.rating) reply += ` ⭐ ${m.rating}/5`;
      reply += `\n`;
      if (m.movieGenre && m.movieGenre.length > 0) reply += `• **Thể loại:** ${m.movieGenre.join(', ')}\n`;
      if (m.duration) reply += `• **Thời lượng:** ${m.duration} phút\n`;
      if (m.description) {
        const shortDesc = m.description.length > 110 ? m.description.substring(0, 110) + '...' : m.description;
        reply += `• **Tóm tắt:** ${shortDesc}\n`;
      }
      reply += `\n`;
    });
    reply += `Bạn muốn xem lịch chiếu hay thông tin chi tiết phim nào ở trên không ạ? 😊`;
  } else {
    reply += `Rất tiếc hiện rạp chưa có phim khác ngoài thể loại này. Bạn có thể tham khảo bảng giá vé hoặc lịch chiếu ngày mai nhé! 🎟️`;
  }

  return reply;
};

/**
 * Phản hồi khi người dùng gửi tin nhắn rỗng / khoảng trắng
 * @returns {string}
 */
export const buildEmptyMessageResponse = () => {
  return `💬 **Hình như bạn chưa nhập nội dung tin nhắn.**\n\n` +
    `Mình có thể hỗ trợ bạn tra cứu:\n` +
    `• 🍿 *"Hôm nay có phim gì chiếu?"*\n` +
    `• 🎟️ *"Giá vé xem phim bao nhiêu tiền?"*\n` +
    `• 🎁 *"Có mã giảm giá khuyến mãi nào không?"*\n` +
    `• 🌟 *"Phim nào rating cao nhất?"*\n\n` +
    `Bạn gõ tin nhắn để mình tư vấn ngay nhé! 😊`;
};

/**
 * Phản hồi thông tin định dạng chiếu phim (IMAX, 4DX...)
 * @returns {string}
 */
export const buildCinemaFormatResponse = () => {
  return `🎥 **THÔNG TIN ĐỊNH DẠNG CHIẾU PHIM ĐẶC BIỆT** 🎥\n\n` +
    `Hiện tại rạp có hỗ trợ các công nghệ trình chiếu tiên tiến mang lại trải nghiệm tuyệt vời:\n\n` +
    `🌟 **IMAX (Image Maximum):**\n` +
    `• Kích thước màn hình khổng lồ, cong ôm sát tầm nhìn.\n` +
    `• Độ nét cực cao, màu sắc sống động và sáng hơn phim 2D thông thường.\n` +
    `• Hệ thống âm thanh vòm đa chiều cực kỳ chân thực.\n` +
    `• Đặc biệt phù hợp với các phim bom tấn hành động, viễn tưởng.\n\n` +
    `💺 **4DX (4D Experience):**\n` +
    `• Ghế ngồi chuyển động linh hoạt (rung lắc, nghiêng, nâng) đồng bộ với cảnh phim.\n` +
    `• Hiệu ứng môi trường thực tế như gió thổi, phun sương, chớp sáng, sương mù, và mùi hương.\n` +
    `• Mang lại cảm giác hóa thân trực tiếp vào nhân vật.\n\n` +
    `Bạn có muốn kiểm tra lịch chiếu các bộ phim IMAX hoặc 4DX hôm nay không? 😊`;
};
