import mongoose from 'mongoose';
import ApiError from '../utils/ApiError.js';
import { StatusCodes } from 'http-status-codes';
import { CinemaRoom } from '../models/cinemaRoomModel.js';
import { Showtime } from '../models/showTimeModel.js';
import { Booking } from '../models/bookingModel.js';
import { Seat } from '../models/seatModel.js';
import { ScheduleSeat } from '../models/scheduleSeatModel.js';
import { Member } from '../models/memberModel.js';
import { Notification } from '../models/notificationModel.js';
import { ScoreHistory } from '../models/scoreHistoryModel.js';
import { Movie } from '../models/movieModel.js';

export const MaintenanceService = {
  getSLATimeoutMinutes(showtimeStartTime) {
    const now = new Date();
    const diffMins = (new Date(showtimeStartTime).getTime() - now.getTime()) / (1000 * 60);
    if (diffMins > 60) return 30;
    if (diffMins >= 30) return 15;
    return 5;
  },

  getRowVal(seatName) {
    if (!seatName) return 99;
    const match = seatName.match(/^([A-Z]+)(\d+)$/);
    if (!match) return 99;
    const rowStr = match[1];
    let val = 0;
    for (let i = 0; i < rowStr.length; i++) {
      val = val * 26 + (rowStr.charCodeAt(i) - 64);
    }
    return val;
  },

  getColVal(seatName) {
    if (!seatName) return 99;
    const match = seatName.match(/^([A-Z]+)(\d+)$/);
    if (!match) return 99;
    return parseInt(match[2], 10);
  },

  scanReplacementSeats({ booking, brokenSeatIds, allRoomSeats, occupiedSeatIds }) {
    const bookingSeatsMap = {};
    (booking.seatIds || []).forEach(sid => {
      const match = allRoomSeats.find(s => s._id.toString() === sid.toString());
      if (match) bookingSeatsMap[sid.toString()] = match;
    });

    const brokenSeats = brokenSeatIds.map(id => bookingSeatsMap[id.toString()]).filter(Boolean);
    const usableSeats = (booking.seatIds || [])
      .map(id => bookingSeatsMap[id.toString()])
      .filter(s => s && !brokenSeatIds.includes(s._id.toString()));

    const targetType = brokenSeats[0]?.seatType || 'Regular';

    const brokenSet = new Set(brokenSeatIds.map(id => id.toString()));
    const availableSeats = allRoomSeats.filter(s =>
      !s.isDeleted &&
      s.status === 'active' &&
      !occupiedSeatIds.has(s._id.toString()) &&
      !brokenSet.has(s._id.toString())
    );

    // Priority 1: SAME_TIER adjacent to usable seats
    if (usableSeats.length > 0) {
      const sameTierAvailable = availableSeats.filter(s => s.seatType === targetType);
      const usableRows = [...new Set(usableSeats.map(s => this.getRowVal(s.seatName)))];
      const usableCols = usableSeats.map(s => this.getColVal(s.seatName));
      const minCol = Math.min(...usableCols);
      const maxCol = Math.max(...usableCols);

      const adjacentSameTier = sameTierAvailable.filter(s => {
        const r = this.getRowVal(s.seatName);
        const c = this.getColVal(s.seatName);
        return usableRows.includes(r) && (c === minCol - 1 || c === maxCol + 1);
      });

      if (adjacentSameTier.length >= brokenSeatIds.length) {
        const selected = adjacentSameTier.slice(0, brokenSeatIds.length);
        return {
          category: 'SAME_TIER',
          assignedSeatIds: selected.map(s => s._id),
          assignedSeatNames: selected.map(s => s.seatName)
        };
      }
    }

    // Priority 2: SAME_TIER new contiguous block or any same tier seats
    const sameTierAvailable = availableSeats.filter(s => s.seatType === targetType);
    if (sameTierAvailable.length >= brokenSeatIds.length) {
      const selected = sameTierAvailable.slice(0, brokenSeatIds.length);
      return {
        category: 'SAME_TIER',
        assignedSeatIds: selected.map(s => s._id),
        assignedSeatNames: selected.map(s => s.seatName)
      };
    }

    // Priority 3: HIGHER_TIER (free upgrade)
    if (targetType === 'Regular') {
      const vipAvailable = availableSeats.filter(s => s.seatType === 'VIP');
      if (vipAvailable.length >= brokenSeatIds.length) {
        const selected = vipAvailable.slice(0, brokenSeatIds.length);
        return {
          category: 'HIGHER_TIER',
          assignedSeatIds: selected.map(s => s._id),
          assignedSeatNames: selected.map(s => s.seatName)
        };
      }
    }

    // Priority 4: LOWER_TIER (downgrade)
    if (targetType === 'VIP') {
      const regAvailable = availableSeats.filter(s => s.seatType === 'Regular');
      if (regAvailable.length >= brokenSeatIds.length) {
        const selected = regAvailable.slice(0, brokenSeatIds.length);
        const oldAvgPrice = brokenSeats.reduce((sum, s) => sum + (s.price || 0), 0) / (brokenSeats.length || 1);
        let refundPoints = 0;
        selected.forEach(s => {
          const diff = oldAvgPrice - (s.price || 0);
          if (diff > 0) refundPoints += Math.round(diff / 10);
        });

        return {
          category: 'LOWER_TIER',
          assignedSeatIds: selected.map(s => s._id),
          assignedSeatNames: selected.map(s => s.seatName),
          priceDiffRefundPoints: refundPoints
        };
      }
    }

    // Priority 5: No seats in room
    return { category: 'ROOM_FULL' };
  },

  async handleStep5FullRoom({ booking, showtime, cinemaId, isDryRun, session, slaMinutes, expiresAt }) {
    const expireAt = expiresAt || new Date(Date.now() + (slaMinutes || 30) * 60 * 1000);
    // Step 5.1: Scan other movie at same cinema, same slot (+/- 30 mins)
    const timeWindow = 30 * 60 * 1000;
    const minTime = new Date(showtime.startTime.getTime() - timeWindow);
    const maxTime = new Date(showtime.startTime.getTime() + timeWindow);

    const altMovieShowtimes = isDryRun
      ? await Showtime.find({
          cinemaId,
          movieId: { $ne: showtime.movieId },
          status: { $in: ['published', 'draft'] },
          startTime: { $gte: minTime, $lte: maxTime }
        })
      : await Showtime.find({
          cinemaId,
          movieId: { $ne: showtime.movieId },
          status: { $in: ['published', 'draft'] },
          startTime: { $gte: minTime, $lte: maxTime }
        }).session(session);

    for (const altSt of altMovieShowtimes) {
      const altSeats = isDryRun
        ? await Seat.find({ roomId: altSt.roomId, isDeleted: false, status: 'active' })
        : await Seat.find({ roomId: altSt.roomId, isDeleted: false, status: 'active' }).session(session);

      const altBookedSS = isDryRun
        ? await ScheduleSeat.find({ showtimeId: altSt._id, status: { $in: [2, 'booked', 'holding'] } })
        : await ScheduleSeat.find({ showtimeId: altSt._id, status: { $in: [2, 'booked', 'holding'] } }).session(session);

      if (altSeats.length - altBookedSS.length >= booking.seatIds.length) {
        if (!isDryRun) {
          const auditLog = {
            timestamp: new Date().toISOString(),
            actionType: 'PROPOSE_OTHER_MOVIE',
            details: { bookingId: booking._id, altShowtimeId: altSt._id, slaMinutes, source: 'auto' }
          };

          const notification = new Notification({
            recipientId: booking.memberId,
            title: "Đề xuất đổi phim cùng khung giờ",
            message: `Phòng chiếu gặp sự cố. Chúng tôi gợi ý đổi sang suất chiếu phim khác vào lúc ${new Date(altSt.startTime).toLocaleTimeString('vi-VN')}. Vui lòng xác nhận trong ${slaMinutes} phút.`,
            type: "ACTION_REQUIRED",
            metadata: {
              bookingId: booking._id,
              altShowtimeId: altSt._id,
              slaTimeoutMinutes: slaMinutes,
              expiresAt,
              auditLog
            },
            expireAt
          });
          await notification.save({ session });
        }
        return { action: 'PROPOSE_OTHER_MOVIE', details: { altShowtimeId: altSt._id } };
      }
    }

    // Step 5.2: Scan next showtime today of same movie
    const startOfDay = new Date(showtime.startTime);
    startOfDay.setHours(0, 0, 0, 0);
    const endOfDay = new Date(showtime.startTime);
    endOfDay.setHours(23, 59, 59, 999);

    const nextShowtimesToday = isDryRun
      ? await Showtime.find({
          cinemaId,
          movieId: showtime.movieId,
          status: { $in: ['published', 'draft'] },
          startTime: { $gt: showtime.startTime, $lte: endOfDay },
          _id: { $ne: showtime._id }
        }).sort({ startTime: 1 })
      : await Showtime.find({
          cinemaId,
          movieId: showtime.movieId,
          status: { $in: ['published', 'draft'] },
          startTime: { $gt: showtime.startTime, $lte: endOfDay },
          _id: { $ne: showtime._id }
        }).session(session).sort({ startTime: 1 });

    for (const nextSt of nextShowtimesToday) {
      const nSeats = isDryRun
        ? await Seat.find({ roomId: nextSt.roomId, isDeleted: false, status: 'active' })
        : await Seat.find({ roomId: nextSt.roomId, isDeleted: false, status: 'active' }).session(session);

      const nBookedSS = isDryRun
        ? await ScheduleSeat.find({ showtimeId: nextSt._id, status: { $in: [2, 'booked', 'holding'] } })
        : await ScheduleSeat.find({ showtimeId: nextSt._id, status: { $in: [2, 'booked', 'holding'] } }).session(session);

      if (nSeats.length - nBookedSS.length >= booking.seatIds.length) {
        if (!isDryRun) {
          const auditLog = {
            timestamp: new Date().toISOString(),
            actionType: 'PROPOSE_NEXT_SHOWTIME',
            details: { bookingId: booking._id, altShowtimeId: nextSt._id, slaMinutes, source: 'auto' }
          };

          const notification = new Notification({
            recipientId: booking.memberId,
            title: "Đề xuất suất chiếu tiếp theo trong ngày",
            message: `Phòng chiếu gặp sự cố. Gợi ý đổi sang suất chiếu tiếp theo vào lúc ${new Date(nextSt.startTime).toLocaleTimeString('vi-VN')}. Vui lòng phản hồi trong ${slaMinutes} phút.`,
            type: "ACTION_REQUIRED",
            metadata: {
              bookingId: booking._id,
              altShowtimeId: nextSt._id,
              slaTimeoutMinutes: slaMinutes,
              expiresAt,
              auditLog
            },
            expireAt
          });
          await notification.save({ session });
        }
        return { action: 'PROPOSE_NEXT_SHOWTIME', details: { altShowtimeId: nextSt._id } };
      }
    }

    // NO MORE SHOWTIMES TODAY -> AUTO REFUND IMMEDIATELY WITHOUT WAITING
    const refundPoints = Math.round(booking.finalAmount / 10);
    if (!isDryRun) {
      booking.paymentStatus = 'refunded_to_points';
      await booking.save({ session });

      const member = isDryRun
        ? await Member.findById(booking.memberId)
        : await Member.findById(booking.memberId).session(session);

      if (member) {
        member.rewardPoints += refundPoints;
        await member.save({ session });

        const scoreHistory = new ScoreHistory({
          memberId: member._id,
          bookingId: booking._id,
          earnedPoints: refundPoints,
          remainingPoints: member.rewardPoints,
          reason: "Tự động hoàn 100% điểm thưởng do hết suất chiếu phù hợp trong ngày (sự cố bảo trì)"
        });
        await scoreHistory.save({ session });
      }

      await ScheduleSeat.deleteMany({
        showtimeId: showtime._id,
        seatId: { $in: booking.seatIds }
      }).session(session);

      const auditLog = {
        timestamp: new Date().toISOString(),
        actionType: 'AUTO_REFUND_NO_SHOWTIMES_LEFT',
        details: { bookingId: booking._id, refundPoints, reason: 'Hết suất chiếu phù hợp trong ngày', source: 'auto' }
      };

      const notification = new Notification({
        recipientId: booking.memberId,
        title: "Thông báo hoàn tiền vé tự động",
        message: `Rất tiếc, suất chiếu của bạn gặp sự cố và không còn suất chiếu nào khác phù hợp trong ngày hôm nay. Chúng tôi đã tự động hoàn lại ${refundPoints} điểm vào tài khoản của bạn.`,
        type: "SYSTEM_ALERT",
        metadata: { bookingId: booking._id, refundPoints, auditLog },
        expireAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
      });
      await notification.save({ session });
    }

    return { action: 'AUTO_REFUND_NO_SHOWTIMES_LEFT', details: { refundPoints } };
  },

  async handleBrokenSeats(roomId, brokenSeatIds = [], options = {}) {
    const isDryRun = !!options.dryRun;
    let session = null;
    if (!isDryRun) {
      session = await mongoose.startSession();
      session.startTransaction();
    }

    const report = {
      roomId,
      brokenSeatIds,
      dryRun: isDryRun,
      showtimesScanned: 0,
      bookingsProcessed: [],
      auditLogs: []
    };

    try {
      const roomQuery = CinemaRoom.findById(roomId);
      const room = isDryRun ? await roomQuery : await roomQuery.session(session);
      if (!room) throw new ApiError(StatusCodes.NOT_FOUND, "Room not found");

      const now = new Date();
      const stQuery = Showtime.find({ roomId: room._id, isDeleted: false, endTime: { $gt: now } });
      const showtimes = isDryRun ? await stQuery : await stQuery.session(session);
      report.showtimesScanned = showtimes.length;

      const brokenSet = new Set(brokenSeatIds.map(id => id.toString()));

      for (const showtime of showtimes) {
        const bkQuery = Booking.find({
          showtimeId: showtime._id,
          paymentStatus: { $in: ['paid', 'successful'] },
          checkedIn: { $ne: true }
        });
        const bookings = isDryRun ? await bkQuery : await bkQuery.session(session);

        for (const booking of bookings) {
          const bookingSeatIdsStr = (booking.seatIds || []).map(id => id.toString());
          const brokenInBooking = bookingSeatIdsStr.filter(id => brokenSet.has(id));

          if (brokenInBooking.length === 0) continue;

          const allRoomSeats = isDryRun
            ? await Seat.find({ roomId: room._id, isDeleted: false })
            : await Seat.find({ roomId: room._id, isDeleted: false }).session(session);

          const bookedScheduleSeats = isDryRun
            ? await ScheduleSeat.find({ showtimeId: showtime._id, status: { $in: [2, 'booked', 'holding'] } })
            : await ScheduleSeat.find({ showtimeId: showtime._id, status: { $in: [2, 'booked', 'holding'] } }).session(session);

          const occupiedSeatIds = new Set(bookedScheduleSeats.map(ss => ss.seatId.toString()));
          bookingSeatIdsStr.forEach(id => {
            if (brokenSet.has(id)) {
              occupiedSeatIds.delete(id);
            }
          });
          brokenSet.forEach(id => occupiedSeatIds.add(id));

          const candidateResult = this.scanReplacementSeats({
            booking,
            brokenSeatIds: brokenInBooking,
            allRoomSeats,
            occupiedSeatIds
          });

          const slaMinutes = this.getSLATimeoutMinutes(showtime.startTime);
          const expiresAt = new Date(Date.now() + slaMinutes * 60 * 1000);

          if (candidateResult.category === 'SAME_TIER' || candidateResult.category === 'HIGHER_TIER') {
            if (!isDryRun) {
              await ScheduleSeat.deleteMany({
                showtimeId: showtime._id,
                seatId: { $in: brokenInBooking }
              }).session(session);

              const assignedIds = [...candidateResult.assignedSeatIds];
              const member = booking.memberId
                ? (isDryRun ? await Member.findById(booking.memberId) : await Member.findById(booking.memberId).session(session))
                : null;
              const memberAccountId = member?.accountId || null;

              const newScheduleSeats = assignedIds.map(sid => ({
                showtimeId: showtime._id,
                seatId: sid,
                accountId: memberAccountId,
                status: 'booked'
              }));
              await ScheduleSeat.insertMany(newScheduleSeats, { session });

              const copyAssigned = [...assignedIds];
              const finalSeatIds = booking.seatIds.map(sid => {
                if (brokenSet.has(sid.toString())) {
                  return copyAssigned.shift();
                }
                return sid;
              });

              const updatedSeats = await Seat.find({ _id: { $in: finalSeatIds } }).session(session);
              const seatNameMap = {};
              updatedSeats.forEach(s => seatNameMap[s._id.toString()] = s.seatName);
              const finalSeatNames = finalSeatIds.map(sid => seatNameMap[sid.toString()]);

              booking.seatIds = finalSeatIds;
              booking.seatNames = finalSeatNames;
              await booking.save({ session });

              const auditLog = {
                timestamp: new Date().toISOString(),
                actionType: candidateResult.category === 'HIGHER_TIER' ? 'AUTO_UPGRADE' : 'AUTO_TRANSFER_SAME_TIER',
                details: {
                  bookingId: booking._id,
                  oldBrokenSeatIds: brokenInBooking,
                  category: candidateResult.category,
                  source: 'auto'
                }
              };

              const notifMsg = candidateResult.category === 'HIGHER_TIER'
                ? `Một số ghế của bạn gặp sự cố. Bạn đã được nâng hạng ghế miễn phí sang [${finalSeatNames.join(', ')}] tại phòng ${room.roomName}.`
                : `Ghế của bạn đã được cập nhật tự động sang [${finalSeatNames.join(', ')}] tại phòng ${room.roomName} do ghế cũ gặp sự cố.`;

              const notification = new Notification({
                recipientId: booking.memberId,
                title: "Thông báo cập nhật vị trí ghế",
                message: notifMsg,
                type: "SYSTEM_ALERT",
                metadata: { bookingId: booking._id, auditLog },
                expireAt: new Date(showtime.endTime.getTime() + 24 * 60 * 60 * 1000)
              });
              await notification.save({ session });
            }

            report.bookingsProcessed.push({
              bookingId: booking._id,
              action: candidateResult.category,
              newSeatNames: candidateResult.assignedSeatNames
            });
          } else if (candidateResult.category === 'LOWER_TIER') {
            if (!isDryRun) {
              const voucherCode = "VCH_DOWNGRADE_" + Math.random().toString(36).substring(2, 8).toUpperCase();
              const auditLog = {
                timestamp: new Date().toISOString(),
                actionType: 'PROPOSE_LOWER_TIER',
                details: {
                  bookingId: booking._id,
                  proposedSeats: candidateResult.assignedSeatNames,
                  slaMinutes,
                  source: 'auto'
                }
              };

              const title = "Cần xác nhận: Đổi sang ghế hạng thường";
              const message = `Do sự cố ghế hỏng, ghế mới đề xuất cho bạn là [${candidateResult.assignedSeatNames.join(', ')}]. Bạn sẽ nhận được voucher đền bù + hoàn điểm chênh lệch. Vui lòng phản hồi trong ${slaMinutes} phút.`;

              const notification = new Notification({
                recipientId: booking.memberId,
                title: title,
                message: message,
                type: "ACTION_REQUIRED",
                metadata: {
                  bookingId: booking._id,
                  proposedSeats: candidateResult.assignedSeatIds,
                  voucherCode,
                  slaTimeoutMinutes: slaMinutes,
                  expiresAt,
                  pointsRefundAmount: candidateResult.priceDiffRefundPoints || 0,
                  auditLog
                },
                expireAt
              });
              await notification.save({ session });
            }

            report.bookingsProcessed.push({
              bookingId: booking._id,
              action: 'PROPOSE_LOWER_TIER',
              slaTimeoutMinutes: slaMinutes
            });
          } else {
            const step5Result = await this.handleStep5FullRoom({
              booking,
              showtime,
              cinemaId: room.cinemaId,
              isDryRun,
              session,
              slaMinutes,
              expiresAt
            });

            report.bookingsProcessed.push({
              bookingId: booking._id,
              action: step5Result.action,
              details: step5Result.details
            });
          }
        }
      }

      if (!isDryRun && session) {
        await session.commitTransaction();
        session.endSession();
      }
      return report;
    } catch (error) {
      if (session) {
        try { await session.abortTransaction(); } catch (e) {}
        try { session.endSession(); } catch (e) {}
      }
      throw error;
    }
  },

  async handleRoomMaintenance(roomId, options = {}) {
    const isDryRun = !!options.dryRun;
    let session = null;
    if (!isDryRun) {
      session = await mongoose.startSession();
      session.startTransaction();
    }

    const report = {
      roomId,
      roomName: '',
      dryRun: isDryRun,
      showtimesScanned: 0,
      showtimesProcessed: [],
      anomalies: []
    };

    try {
      const roomQuery = CinemaRoom.findById(roomId);
      const room = isDryRun ? await roomQuery : await roomQuery.session(session);
      if (!room) throw new Error("Room not found");
      report.roomName = room.roomName;

      const now = new Date();

      const stQuery = Showtime.find({ roomId: room._id, isDeleted: false, endTime: { $gt: now } });
      const showtimes = isDryRun ? await stQuery : await stQuery.session(session);
      report.showtimesScanned = showtimes.length;

      console.log(`[DEBUG] handleRoomMaintenance (${isDryRun ? 'DRY-RUN' : 'LIVE'}): found ${showtimes.length} showtimes for room ${room.roomName}`);

      for (const showtime of showtimes) {
        const bkQuery = Booking.find({
          showtimeId: showtime._id,
          paymentStatus: { $in: ['paid', 'successful'] },
          checkedIn: { $ne: true }
        });
        const bookings = isDryRun ? await bkQuery : await bkQuery.session(session);

        console.log(`[DEBUG] Showtime ${showtime._id} (${showtime.showTime}): bookings count = ${bookings.length}`);

        if (bookings.length === 0) {
          console.log(`[DEBUG] Showtime ${showtime._id} has 0 bookings. ${isDryRun ? 'Skipping (dry-run)' : 'Cancelling showtime (preserving DB document)...'}`);
          if (!isDryRun) {
            showtime.status = 'cancelled';
            await showtime.save({ session });
          }
          continue;
        }

        if (showtime.endTime <= now) {
          console.log(`[DEBUG] Showtime ${showtime._id} is in the past. Skipping...`);
          continue;
        }

        // Suspend showtime
        if (!isDryRun) {
          showtime.status = 'suspended';
          await showtime.save({ session });
        }
        console.log(`[DEBUG] ${isDryRun ? '[DRY-RUN] Would suspend' : 'Suspended'} showtime ${showtime._id}`);

        // --- STEP 1: Gather original seat counts (VIP vs Regular vs Couple) & detect anomalies ---
        const seatIds = bookings.flatMap(b => b.seatIds || []);
        const sQuery = Seat.find({ _id: { $in: seatIds } });
        const bookedSeats = isDryRun ? await sQuery : await sQuery.session(session);
        const bookedSeatsMap = {};
        bookedSeats.forEach(s => {
          bookedSeatsMap[s._id.toString()] = s;
        });

        // Data anomaly check
        bookings.forEach(b => {
          if (!b.seatIds || b.seatIds.length === 0) {
            report.anomalies.push({ type: 'MISSING_SEAT_IDS', bookingId: b._id, showtimeId: showtime._id });
          }
          (b.seatIds || []).forEach(sid => {
            const seat = bookedSeatsMap[sid.toString()];
            if (!seat) {
              report.anomalies.push({ type: 'SEAT_NOT_FOUND_IN_DB', seatId: sid, bookingId: b._id, showtimeId: showtime._id });
            } else if (seat.price == null || seat.price === 0) {
              report.anomalies.push({ type: 'SEAT_PRICE_NULL_OR_ZERO', seatId: sid, seatName: seat.seatName, bookingId: b._id });
            }
          });
        });

        let soVeVIP = 0;
        let soVeThuong = 0;
        let soCapDoi = 0; // số cặp ghế đôi (1 cặp = 1 booking 2 ghế Couple)
        bookings.forEach(b => {
          const seatTypes = b.seatIds.map(sid => (bookedSeatsMap[sid.toString()] || {}).seatType || 'Regular');
          const isCouple = seatTypes.length >= 2 && seatTypes.every(t => t === 'Couple');
          if (isCouple) {
            soCapDoi++;
          } else {
            seatTypes.forEach(stype => {
              if (stype === 'VIP') soVeVIP++;
              else soVeThuong++;
            });
          }
        });
        // tongSoVe = tổng số ghế vật lý cần bố trí
        const tongSoVe = soVeVIP + soVeThuong + (soCapDoi * 2);
        console.log(`[DEBUG] Total tickets: ${tongSoVe} (VIP: ${soVeVIP}, Regular: ${soVeThuong}, Couple pairs: ${soCapDoi})`);

        // Find available rooms
        const availableRooms = await CinemaRoom.find({
          cinemaId: room.cinemaId,
          status: 'active',
          _id: { $ne: room._id }
        }).session(session);

        // Track resolved
        let resolved = false;

        // --- STEP 2: Try Room Switch (Cách A) ---
        // Find empty rooms in the same cinema with no conflicting showtimes
        for (const newRoom of availableRooms) {
          const conflictingShowtime = await Showtime.findOne({
            roomId: newRoom._id,
            $or: [
              { startTime: { $lt: showtime.endTime, $gte: showtime.startTime } },
              { endTime: { $gt: showtime.startTime, $lte: showtime.endTime } }
            ],
            status: { $in: ['published', 'draft'] }
          }).session(session);

          if (!conflictingShowtime) {
            // Find seats in newRoom matching the exact names in all bookings
            const allSeatNames = bookings.flatMap(b => b.seatNames);
            const newSeats = await Seat.find({
              roomId: newRoom._id,
              seatName: { $in: allSeatNames }
            }).session(session);

            if (newSeats.length === allSeatNames.length) {
              const allRoomSeats = isDryRun ? await Seat.find({ roomId: newRoom._id }) : await Seat.find({ roomId: newRoom._id }).session(session);
              const newSeatsMap = {};
              newSeats.forEach(s => {
                newSeatsMap[s.seatName] = s._id;
              });

              if (!isDryRun) {
                // Yes! We can clone the showtime here.
                const newShowtime = new Showtime({
                  cinemaId: showtime.cinemaId,
                  roomId: newRoom._id,
                  movieId: showtime.movieId,
                  showDate: showtime.showDate,
                  startTime: showtime.startTime,
                  endTime: showtime.endTime,
                  ticketPrice: showtime.ticketPrice,
                  status: 'published'
                });
                await newShowtime.save({ session });

                // Delete old schedule seats for this showtime's bookings
                await ScheduleSeat.deleteMany({
                  showtimeId: showtime._id,
                  seatId: { $in: seatIds }
                }).session(session);

                // Update each booking
                for (const booking of bookings) {
                  const member = await Member.findById(booking.memberId).session(session);
                  const bookingNewSeatIds = booking.seatNames.map(sname => newSeatsMap[sname]);

                  const newScheduleSeats = bookingNewSeatIds.map(sid => ({
                    showtimeId: newShowtime._id,
                    seatId: sid,
                    accountId: member ? member.accountId : null,
                    status: 'booked'
                  }));
                  await ScheduleSeat.insertMany(newScheduleSeats, { session });

                  booking.showtimeId = newShowtime._id;
                  booking.seatIds = bookingNewSeatIds;
                  booking.paymentStatus = 'paid';
                  await booking.save({ session });

                  await this.createAutoTransferNotification(booking.memberId, booking._id, room.roomName, newRoom.roomName, showtime.endTime, session);
                }

                // Create default ScheduleSeat for all other empty seats in the new room
                const assignedSeatIdsStr = newSeats.map(s => s._id.toString());
                const emptyRoomSeats = allRoomSeats.filter(s => !assignedSeatIdsStr.includes(s._id.toString()));
                const defaultScheduleSeats = emptyRoomSeats.map(s => ({
                  showtimeId: newShowtime._id,
                  seatId: s._id,
                  status: 1
                }));
                if (defaultScheduleSeats.length > 0) {
                  await ScheduleSeat.insertMany(defaultScheduleSeats, { session });
                }
              }

              report.showtimesProcessed.push({
                showtimeId: showtime._id,
                action: 'AUTO_ROOM_SWITCH',
                targetRoomName: newRoom.roomName,
                bookingsCount: bookings.length
              });

              resolved = true;
              break;
            }
          }
        }

        if (resolved) continue;

        // --- STEP 3: Try Merging into Alternative Showtimes (Cách B) ---
        console.log(`[DEBUG] Trying merge for showtime ${showtime._id} since Free Room was not found.`);
        const timeWindow = 15 * 60 * 1000;
        const targetMinTime = new Date(showtime.startTime.getTime() - timeWindow);
        const targetMaxTime = new Date(showtime.startTime.getTime() + timeWindow);

        // Find candidate showtimes
        const candidateShowtimes = await Showtime.find({
          cinemaId: showtime.cinemaId,
          movieId: showtime.movieId,
          status: { $in: ['published', 'draft'] },
          startTime: { $gte: targetMinTime, $lte: targetMaxTime },
          _id: { $ne: showtime._id }
        }).session(session);

        const candidates = [];
        for (const altSt of candidateShowtimes) {
          const altRoom = await CinemaRoom.findById(altSt.roomId).session(session);
          if (!altRoom || altRoom.status !== 'active') continue;

          const altRoomSeats = await Seat.find({ roomId: altSt.roomId }).session(session);
          // Find already booked/held seats in the altSt (using status 2 for sold or booked)
          const bookedScheduleSeats = await ScheduleSeat.find({
            showtimeId: altSt._id,
            status: { $in: [2, 'booked', 'holding'] }
          }).session(session);
          const bookedSeatIds = bookedScheduleSeats.map(s => s.seatId.toString());

          const emptySeats = altRoomSeats.filter(s => !bookedSeatIds.includes(s._id.toString()));
          const emptyVip = emptySeats.filter(s => s.seatType === 'VIP');
          const emptyRegular = emptySeats.filter(s => s.seatType === 'Regular');
          const emptyCouple = emptySeats.filter(s => s.seatType === 'Couple');
          // tongGheTrong = tổng ghế vật lý trống (mỗi ghế Couple là 1 đơn vị vật lý)
          const tongGheTrong = emptyVip.length + emptyRegular.length + emptyCouple.length;

          if (tongGheTrong >= tongSoVe) {
            candidates.push({
              altSt,
              altRoom,
              emptySeats,
              emptyVip,
              emptyRegular,
              emptyCouple,
              tongGheTrong
            });
          }
        }

        console.log(`[DEBUG] Found ${candidates.length} candidate alternative showtimes with enough seats.`);

        if (candidates.length === 0) {
          // --- STEP 3a: No Candidate Fits -> Refund All ---
          console.log(`[DEBUG] No candidate showtimes fit. Refunding all bookings (${isDryRun ? 'DRY-RUN' : 'LIVE'}).`);
          for (const booking of bookings) {
            const refundPoints = Math.round(booking.finalAmount / 10);

            if (!isDryRun) {
              booking.paymentStatus = 'refunded_to_points';
              const member = await Member.findById(booking.memberId).session(session);

              if (member) {
                member.rewardPoints += refundPoints;
                await member.save({ session });

                const scoreHistory = new ScoreHistory({
                  memberId: member._id,
                  bookingId: booking._id,
                  earnedPoints: refundPoints,
                  remainingPoints: member.rewardPoints,
                  reason: "Hoàn điểm 100% do hủy suất chiếu (bảo trì phòng và không có phòng thay thế)"
                });
                await scoreHistory.save({ session });
              }

              await ScheduleSeat.deleteMany({
                showtimeId: showtime._id,
                seatId: { $in: booking.seatIds }
              }).session(session);

              await booking.save({ session });
              await this.createCancelNotification(booking.memberId, booking._id, refundPoints, session);
            }
          }
          report.showtimesProcessed.push({
            showtimeId: showtime._id,
            action: 'FULL_REFUND_NO_ALT_SHOWTIME',
            totalBookings: bookings.length
          });
          continue; // Done with this showtime
        }

        // Rank candidates
        // Priority 0: đủ cặp ghế Đôi (không thể thay thế dễ dàng)
        // Priority 1: đủ VIP
        // Priority 2: số VIP trống nhiều nhất
        candidates.sort((a, b) => {
          // Priority 0 (TC22): Ưu tiên phòng đủ ghế Đôi
          if (soCapDoi > 0) {
            const hasEnoughCoupleA = Math.floor(a.emptyCouple.length / 2) >= soCapDoi ? 1 : 0;
            const hasEnoughCoupleB = Math.floor(b.emptyCouple.length / 2) >= soCapDoi ? 1 : 0;
            if (hasEnoughCoupleA !== hasEnoughCoupleB) return hasEnoughCoupleB - hasEnoughCoupleA;
          }

          // Priority 1 (TC28): Ưu tiên phòng khớp hạng HOÀN TOÀN (Đủ cả VIP và Thường)
          const matchExactA = (a.emptyVip.length >= soVeVIP && a.emptyRegular.length >= soVeThuong) ? 1 : 0;
          const matchExactB = (b.emptyVip.length >= soVeVIP && b.emptyRegular.length >= soVeThuong) ? 1 : 0;
          if (matchExactA !== matchExactB) return matchExactB - matchExactA;

          // Priority 2 (TC05): Chọn phòng có nhiều ghế VIP trống nhất
          if (b.emptyVip.length !== a.emptyVip.length) return b.emptyVip.length - a.emptyVip.length;

          // Priority 3 (TC29 Tie-break): Nếu ngang điểm, chọn theo thứ tự roomId tăng dần trong DB
          return a.altRoom._id.toString().localeCompare(b.altRoom._id.toString());
        });

        const bestCandidate = candidates[0];
        const { altSt, altRoom } = bestCandidate;
        console.log(`[DEBUG] Best candidate: Room ${altRoom.roomName} (${altRoom._id}) with ${bestCandidate.tongGheTrong} empty seats.`);

        // Distribute seats: sort bookings by createdAt ascending, then membership rank, then random
        const memberIds = bookings.map(b => b.memberId);
        const members = await Member.find({ _id: { $in: memberIds } }).session(session);
        const memberMap = {};
        members.forEach(m => {
          memberMap[m._id.toString()] = m;
        });

        const rankPriority = { gold: 3, silver: 2, bronze: 1 };
        const sortedBookings = [...bookings].sort((a, b) => {
          const timeA = new Date(a.createdAt).getTime();
          const timeB = new Date(b.createdAt).getTime();
          const timeDiff = timeA - timeB;

          if (Math.abs(timeDiff) > 10) {
            return timeDiff;
          }

          const memA = memberMap[a.memberId.toString()];
          const memB = memberMap[b.memberId.toString()];
          const rankA = memA ? (rankPriority[memA.membershipRank] || 1) : 1;
          const rankB = memB ? (rankPriority[memB.membershipRank] || 1) : 1;

          if (rankA !== rankB) {
            console.log(`[DEBUG] Trùng thời điểm đặt vé. So sánh hạng thành viên: ${memA?.fullName || 'N/A'} (${memA?.membershipRank || 'bronze'}) vs ${memB?.fullName || 'N/A'} (${memB?.membershipRank || 'bronze'})`);
            return rankB - rankA;
          }

          const rand = Math.random() - 0.5;
          console.log(`[DEBUG] Trùng cả thời điểm và hạng thành viên. Random phân bổ giữa ${memA?.fullName || 'N/A'} và ${memB?.fullName || 'N/A'}. Kết quả: ${rand < 0 ? 'Ưu tiên ' + (memA?.fullName || 'N/A') : 'Ưu tiên ' + (memB?.fullName || 'N/A')}`);
          return rand;
        });

        // Sort regular seats by beauty
        const midRow = Math.ceil(altRoom.rowsCount / 2);
        const midCol = Math.ceil(altRoom.colsCount / 2);

        const getRowVal = (seatName) => {
          const match = seatName.match(/^([A-Z]+)(\d+)$/);
          if (!match) return 99;
          const rowStr = match[1];
          let val = 0;
          for (let i = 0; i < rowStr.length; i++) {
            val = val * 26 + (rowStr.charCodeAt(i) - 64);
          }
          return val;
        };
        const getColVal = (seatName) => {
          const match = seatName.match(/^([A-Z]+)(\d+)$/);
          if (!match) return 99;
          return parseInt(match[2], 10);
        };

        const sortSeatsByBeauty = (seats) => {
          return [...seats].map(s => {
            const r = getRowVal(s.seatName);
            const c = getColVal(s.seatName);
            const score = Math.abs(r - midRow) + Math.abs(c - midCol);
            return { seat: s, score };
          }).sort((a, b) => a.score - b.score).map(x => x.seat);
        };

        const findConsecutiveSeats = (seats, count) => {
          const seatsByRow = {};
          seats.forEach(s => {
            const r = getRowVal(s.seatName);
            if (!seatsByRow[r]) seatsByRow[r] = [];
            seatsByRow[r].push(s);
          });

          for (const rowVal in seatsByRow) {
            const rowSeats = seatsByRow[rowVal].sort((a, b) => getColVal(a.seatName) - getColVal(b.seatName));
            for (let i = 0; i <= rowSeats.length - count; i++) {
              let consecutive = true;
              for (let j = 0; j < count - 1; j++) {
                const colCurr = getColVal(rowSeats[i + j].seatName);
                const colNext = getColVal(rowSeats[i + j + 1].seatName);
                if (colNext !== colCurr + 1) {
                  consecutive = false;
                  break;
                }
              }
              if (consecutive) {
                return rowSeats.slice(i, i + count);
              }
            }
          }
          return null;
        };

        let emptyVips = [...bestCandidate.emptyVip];
        let emptyRegulars = sortSeatsByBeauty(bestCandidate.emptyRegular);
        let emptyCouples = sortSeatsByBeauty(bestCandidate.emptyCouple);

        // Delete old schedule seats for original bookings
        if (!isDryRun) {
          await ScheduleSeat.deleteMany({
            showtimeId: showtime._id,
            seatId: { $in: seatIds }
          }).session(session);
        }

        // Process bookings one by one
        for (const booking of sortedBookings) {
          const bookingSeats = await Seat.find({ _id: { $in: booking.seatIds } }).session(session);
          const bookingSeatsMap = {};
          bookingSeats.forEach(s => {
            bookingSeatsMap[s._id.toString()] = s;
          });

          let assignedNewSeats = [];
          let downgradedVipCount = 0;
          let upgradedRegularCount = 0;
          let downgradedCoupleCount = 0; // số cặp đôi bị hạ hạng
          let coupleDowngradeType = ''; // 'VIP' | 'Regular' | 'separated'
          let refundPoints = 0;

          const seatTypesInBooking = bookingSeats.map(s => s.seatType);
          const isCouple = seatTypesInBooking.length >= 2 && seatTypesInBooking.every(t => t === 'Couple');

          // ---- COUPLE SEAT HANDLING (Case D & E) ----
          let consecutiveFound = false;
          if (isCouple) {
            const numSeatsNeeded = booking.seatIds.length; // = 2 cho 1 cặp đôi
            const avgOldPrice = bookingSeats.reduce((sum, s) => sum + s.price, 0) / bookingSeats.length;

            // Case D: còn đủ ghế Couple liền kề
            const coupleConsecutive = findConsecutiveSeats(emptyCouples, numSeatsNeeded);
            if (coupleConsecutive) {
              assignedNewSeats = coupleConsecutive;
              consecutiveFound = true;
              const assignedIdsStr = assignedNewSeats.map(s => s._id.toString());
              emptyCouples = emptyCouples.filter(s => !assignedIdsStr.includes(s._id.toString()));
            } else {
              // Case E: hết ghế Couple - hạ hạng
              downgradedCoupleCount++;

              // Ưu tiên 1: 2 ghế VIP liền kề
              const vipConsecutive = findConsecutiveSeats(emptyVips, numSeatsNeeded);
              if (vipConsecutive) {
                assignedNewSeats = vipConsecutive;
                consecutiveFound = true;
                coupleDowngradeType = 'VIP';
                const assignedIdsStr = assignedNewSeats.map(s => s._id.toString());
                emptyVips = emptyVips.filter(s => !assignedIdsStr.includes(s._id.toString()));
                // Hoàn chênh lệch theo từng ghế
                assignedNewSeats.forEach(newSeat => {
                  const diff = avgOldPrice - newSeat.price;
                  if (diff > 0) refundPoints += Math.round(diff / 10);
                });
              } else {
                // Ưu tiên 2: 2 ghế Thường liền kề
                const regularConsecutive = findConsecutiveSeats(emptyRegulars, numSeatsNeeded);
                if (regularConsecutive) {
                  assignedNewSeats = regularConsecutive;
                  consecutiveFound = true;
                  coupleDowngradeType = 'Regular';
                  const assignedIdsStr = assignedNewSeats.map(s => s._id.toString());
                  emptyRegulars = emptyRegulars.filter(s => !assignedIdsStr.includes(s._id.toString()));
                  assignedNewSeats.forEach(newSeat => {
                    const diff = avgOldPrice - newSeat.price;
                    if (diff > 0) refundPoints += Math.round(diff / 10);
                  });
                } else {
                  // Fallback: xếp rải rác 2 ghế gần nhau nhất (consecutiveFound = false -> cảnh báo)
                  coupleDowngradeType = 'separated';
                  for (let i = 0; i < numSeatsNeeded; i++) {
                    let seat = null;
                    if (emptyVips.length > 0) {
                      seat = emptyVips.shift();
                    } else if (emptyRegulars.length > 0) {
                      seat = emptyRegulars.shift();
                    } else {
                      seat = emptyCouples.shift();
                    }
                    if (seat) {
                      assignedNewSeats.push(seat);
                      const diff = avgOldPrice - seat.price;
                      if (diff > 0) refundPoints += Math.round(diff / 10);
                    }
                  }
                }
              }
            }
          }

          // ---- VIP / REGULAR HANDLING (existing logic) ----
          if (!isCouple) {
            // Attempt consecutive allocation for group bookings (N >= 2) of the same seat type
            if (booking.seatIds.length >= 2) {
              const firstType = bookingSeats[0].seatType;
              const allSameType = bookingSeats.every(s => s.seatType === firstType);

              if (allSameType && firstType !== 'Couple') {
                const targetEmptySeats = firstType === 'VIP' ? emptyVips : emptyRegulars;
                const consecutiveRun = findConsecutiveSeats(targetEmptySeats, booking.seatIds.length);
                if (consecutiveRun) {
                  assignedNewSeats = consecutiveRun;
                  consecutiveFound = true;

                  const assignedIdsStr = assignedNewSeats.map(s => s._id.toString());
                  if (firstType === 'VIP') {
                    emptyVips = emptyVips.filter(s => !assignedIdsStr.includes(s._id.toString()));
                  } else {
                    emptyRegulars = emptyRegulars.filter(s => !assignedIdsStr.includes(s._id.toString()));
                  }
                }
              }
            }

            // Fallback to default one-by-one allocation
            if (!consecutiveFound) {
              for (const oldSeatId of booking.seatIds) {
                const oldSeat = bookingSeatsMap[oldSeatId.toString()];
                if (oldSeat.seatType === 'VIP') {
                  if (emptyVips.length > 0) {
                    assignedNewSeats.push(emptyVips.shift());
                  } else {
                    // Downgrade to regular
                    const regularSeat = emptyRegulars.shift();
                    assignedNewSeats.push(regularSeat);
                    downgradedVipCount++;
                    const priceDiff = oldSeat.price - regularSeat.price;
                    if (priceDiff > 0) {
                      refundPoints += Math.round(priceDiff / 10);
                    }
                  }
                } else {
                  // Regular or Couple seat fallback
                  if (emptyRegulars.length > 0) {
                    assignedNewSeats.push(emptyRegulars.shift());
                  } else if (emptyVips.length > 0) {
                    // Upgrade to VIP
                    assignedNewSeats.push(emptyVips.shift());
                    upgradedRegularCount++;
                  } else if (emptyCouples.length > 0) {
                    assignedNewSeats.push(emptyCouples.shift());
                  }
                }
              }
            }
          }

          const newSeatNames = assignedNewSeats.map(s => s.seatName);
          const newSeatIds = assignedNewSeats.map(s => s._id);

          // Generate voucher code & message template accurately reflecting change nature
          let voucherType = 'SAME_CLASS_TRANSFER';
          let voucherPrefix = 'VCH_TRANSFER_';

          if (downgradedCoupleCount > 0) {
            voucherType = 'COUPLE_DOWNGRADE_PREMIUM';
            voucherPrefix = 'VCH_COUPLE_PREMIUM_';
          } else if (downgradedVipCount > 0) {
            voucherType = 'STANDARD_DOWNGRADE';
            voucherPrefix = 'VCH_DOWNGRADE_';
          } else if (upgradedRegularCount > 0) {
            voucherType = 'FREE_UPGRADE';
            voucherPrefix = 'VCH_UPGRADE_';
          } else {
            voucherType = 'SAME_CLASS_TRANSFER';
            voucherPrefix = 'VCH_TRANSFER_';
          }

          const voucherCode = voucherPrefix + Math.random().toString(36).substring(2, 8).toUpperCase();

          const title = "Sự cố phòng chiếu - Cần xác nhận dời vé";
          let changeDetailsMsg = "";
          if (downgradedCoupleCount > 0) {
            if (coupleDowngradeType === 'VIP') {
              changeDetailsMsg = `Ghế đôi của bạn được chuyển sang 2 ghế VIP liền kề [${newSeatNames.join(', ')}] tại phòng ${altRoom.roomName}. Chúng tôi hoàn lại ${refundPoints * 10} VNĐ (${refundPoints} điểm) chênh lệch + 1 Voucher đền bù hạng cao hơn.`;
            } else if (coupleDowngradeType === 'Regular') {
              changeDetailsMsg = `Ghế đôi của bạn được chuyển sang 2 ghế Thường liền kề [${newSeatNames.join(', ')}] tại phòng ${altRoom.roomName}. Chúng tôi hoàn lại ${refundPoints * 10} VNĐ (${refundPoints} điểm) chênh lệch + 1 Voucher đền bù hạng cao hơn.`;
            } else {
              changeDetailsMsg = `Ghế đôi của bạn được chuyển sang 2 ghế [${newSeatNames.join(', ')}] tại phòng ${altRoom.roomName}. Chúng tôi hoàn lại ${refundPoints * 10} VNĐ (${refundPoints} điểm) chênh lệch + 1 Voucher đền bù hạng cao hơn.`;
            }
          } else if (downgradedVipCount > 0) {
            changeDetailsMsg = `Ghế VIP của bạn được chuyển sang ghế thường [${newSeatNames.join(', ')}] và hoàn lại ${refundPoints * 10} VNĐ (${refundPoints} điểm) + 1 Voucher đền bù.`;
          } else if (upgradedRegularCount > 0) {
            changeDetailsMsg = `Ghế Thường của bạn được nâng hạng lên ghế VIP [${newSeatNames.join(', ')}] miễn phí + 1 Voucher đền bù.`;
          } else {
            changeDetailsMsg = `Ghế của bạn được chuyển sang [${newSeatNames.join(', ')}] cùng hạng + 1 Voucher đền bù.`;
          }

          const isSeparated = booking.seatIds.length >= 2 && !consecutiveFound;
          const separationWarning = isSeparated
            ? (downgradedCoupleCount > 0
              ? " Lưu ý: do phòng mới không còn đủ ghế liền kề, 2 ghế của bạn sẽ không ngồi cạnh nhau. Xin lỗi vì sự bất tiện này."
              : " (Do phòng mới không đủ cụm ghế liền kề, nhóm của bạn buộc phải xếp tách ghế. Rất mong bạn thông cảm.)")
            : "";
          const movie = session ? await Movie.findById(showtime.movieId).session(session) : await Movie.findById(showtime.movieId);
          const movieTitle = movie ? (movie.movieName || movie.title || 'Phim') : 'Phim';
          const message = `Suất chiếu ${movieTitle} tại phòng ${room.roomName} gặp sự cố. Chúng tôi đã chuyển sang phòng ${altRoom.roomName} cùng khung giờ. ${changeDetailsMsg}${separationWarning} Bạn có đồng ý đổi phòng chiếu không?`;

          if (!isDryRun) {
            const mQuery = Member.findById(booking.memberId);
            const member = isDryRun ? await mQuery : await mQuery.session(session);
            const bulkOps = assignedNewSeats.map(seat => ({
              updateOne: {
                filter: { showtimeId: altSt._id, seatId: seat._id },
                update: {
                  $set: {
                    status: 'booked',
                    accountId: member ? member.accountId : null
                  }
                },
                upsert: true
              }
            }));
            await ScheduleSeat.bulkWrite(bulkOps, { session });

            booking.showtimeId = altSt._id;
            booking.seatIds = newSeatIds;
            booking.seatNames = newSeatNames;
            booking.paymentStatus = 'pending';
            await booking.save({ session });

            const expireAt = new Date(altSt.endTime.getTime() + 24 * 60 * 60 * 1000);
            const notification = new Notification({
              recipientId: booking.memberId,
              title: title,
              message: message,
              type: "ACTION_REQUIRED",
              metadata: {
                bookingId: booking._id,
                newRoomId: altRoom._id,
                newShowtimeId: altSt._id,
                pointsRefundAmount: refundPoints,
                voucherCode: voucherCode,
                voucherType: voucherType,
                downgradedVipCount,
                downgradedCoupleCount
              },
              expireAt
            });
            await notification.save({ session });

            // Send FCM push notification
            try {
              const { NotificationService } = await import('./notificationService.js');
              const { Account } = await import('../models/accountModel.js');
              if (member && member.accountId) {
                const account = await Account.findById(member.accountId).select('fcmToken fcmTokens').session(session);
                if (account) {
                  const tokens = account.fcmTokens && account.fcmTokens.length > 0
                    ? account.fcmTokens
                    : (account.fcmToken ? [account.fcmToken] : []);
                  if (tokens.length > 0) {
                    await NotificationService.sendToDevice(tokens, title, message, {
                      type: 'MAINTENANCE_ACTION_REQUIRED',
                      bookingId: booking._id.toString()
                    });
                  }
                }
              }
            } catch (err) {
              console.error("[DEBUG] FCM error during alternative showtime notify:", err);
            }
          }
        }

        report.showtimesProcessed.push({
          showtimeId: showtime._id,
          action: 'PROPOSE_TRANSFER_ALTERNATIVE_ROOM',
          targetRoomName: altRoom.roomName,
          targetShowtimeId: altSt._id,
          bookingsCount: bookings.length
        });
      }

      if (!isDryRun && session) {
        await session.commitTransaction();
        session.endSession();
      }
      return report;
    } catch (error) {
      if (session) {
        try { await session.abortTransaction(); } catch (e) { }
        try { session.endSession(); } catch (e) { }
      }
      throw error;
    }
  },

  async createNotification(memberId, bookingId, newRoomId, newShowtimeId, endTime, session) {
    const expireAt = new Date(endTime.getTime() + 24 * 60 * 60 * 1000); // 24h sau suất chiếu

    const title = "Sự cố phòng chiếu - Cần xác nhận dời vé";
    const message = "Rạp đang bảo trì phòng chiếu. Chúng tôi đã chuẩn bị suất chiếu mới cho bạn. Vui lòng xác nhận hoặc hủy để hoàn điểm.";

    const notification = new Notification({
      recipientId: memberId,
      title: title,
      message: message,
      type: "ACTION_REQUIRED",
      metadata: { bookingId, newRoomId, newShowtimeId },
      expireAt
    });
    await notification.save({ session });

    try {
      const { NotificationService } = await import('./notificationService.js');
      const { Account } = await import('../models/accountModel.js');

      const mQuery = Member.findById(memberId);
      const member = session ? await mQuery.session(session) : await mQuery;
      if (member && member.accountId) {
        const aQuery = Account.findById(member.accountId).select('fcmToken fcmTokens');
        const account = session ? await aQuery.session(session) : await aQuery;
        if (account) {
          const tokens = account.fcmTokens && account.fcmTokens.length > 0
            ? account.fcmTokens
            : (account.fcmToken ? [account.fcmToken] : [])

          if (tokens.length > 0) {
            await NotificationService.sendToDevice(tokens, title, message, {
              type: 'MAINTENANCE_ACTION_REQUIRED',
              bookingId: bookingId.toString(),
              referenceId: bookingId.toString()
            });
          }
        }
      }
    } catch (err) {
      console.error("Error sending push notification for maintenance:", err);
    }
  },

  async createAutoTransferNotification(memberId, bookingId, oldRoomName, newRoomName, endTime, session) {
    const expireAt = new Date(endTime.getTime() + 24 * 60 * 60 * 1000); // 24h sau suất chiếu

    const title = "Thông báo tự động đổi phòng chiếu";
    const message = `Suất chiếu của bạn đã được chuyển từ phòng ${oldRoomName} sang phòng ${newRoomName} do sự cố bảo trì phòng chiếu. Vé của bạn vẫn giữ nguyên hiệu lực.`;

    const notification = new Notification({
      recipientId: memberId,
      title: title,
      message: message,
      type: "SYSTEM_ALERT",
      metadata: { bookingId },
      expireAt
    });
    await notification.save({ session });

    try {
      const { NotificationService } = await import('./notificationService.js');
      const { Account } = await import('../models/accountModel.js');

      const mQuery = Member.findById(memberId);
      const member = session ? await mQuery.session(session) : await mQuery;
      if (member && member.accountId) {
        const aQuery = Account.findById(member.accountId).select('fcmToken fcmTokens');
        const account = session ? await aQuery.session(session) : await aQuery;
        if (account) {
          const tokens = account.fcmTokens && account.fcmTokens.length > 0
            ? account.fcmTokens
            : (account.fcmToken ? [account.fcmToken] : [])

          if (tokens.length > 0) {
            await NotificationService.sendToDevice(tokens, title, message, {
              type: 'MAINTENANCE_AUTO_TRANSFERRED',
              bookingId: bookingId.toString()
            });
          }
        }
      }
    } catch (err) {
      console.error("Error sending push notification for auto maintenance:", err);
    }
  },

  async createCancelNotification(memberId, bookingId, refundAmount, session) {
    const expireAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000); // 30 ngày

    const title = "Hủy vé & Hoàn điểm tự động";
    const message = `Suất chiếu của bạn đã bị hủy do rạp bảo trì phòng. Hệ thống không tìm thấy suất chiếu thay thế phù hợp. Chúng tôi đã hoàn lại ${refundAmount} điểm vào tài khoản của bạn. Cáo lỗi vì sự bất tiện này.`;

    const notification = new Notification({
      recipientId: memberId,
      title: title,
      message: message,
      type: "SYSTEM_ALERT",
      metadata: { bookingId },
      expireAt
    });
    await notification.save({ session });

    try {
      const { NotificationService } = await import('./notificationService.js');
      const { Account } = await import('../models/accountModel.js');

      const member = await Member.findById(memberId).session(session);
      if (member && member.accountId) {
        const account = await Account.findById(member.accountId).select('fcmToken fcmTokens').session(session);
        if (account) {
          const tokens = account.fcmTokens && account.fcmTokens.length > 0
            ? account.fcmTokens
            : (account.fcmToken ? [account.fcmToken] : [])

          if (tokens.length > 0) {
            await NotificationService.sendToDevice(tokens, title, message, {
              type: 'MAINTENANCE_CANCELLED',
              bookingId: bookingId.toString()
            });
          }
        }
      }
    } catch (err) {
      console.error("Error sending cancel push notification:", err);
    }
  },

  // Module 2: User Action
  async resolveUserAction(bookingId, action) {
    if (action !== 'accept' && action !== 'decline') {
      throw new Error("Invalid action");
    }

    const booking = await Booking.findOneAndUpdate(
      { _id: bookingId, paymentStatus: 'pending' },
      { $set: { paymentStatus: action === 'accept' ? 'paid' : 'refunded_to_points' } },
      { returnDocument: 'after' }
    );
    if (!booking) {
      throw new ApiError(StatusCodes.CONFLICT, "Booking not found or not in pending transfer state");
    }

    const notification = await Notification.findOne({
      $or: [
        { "metadata.bookingId": booking._id },
        { "metadata.bookingId": booking._id.toString() }
      ]
    });

    const { Promotion } = await import('../models/promotionModel.js');
    const { NotificationService } = await import('./notificationService.js');
    const { Account } = await import('../models/accountModel.js');

    const member = await Member.findById(booking.memberId);

    if (action === 'accept') {
      if (member) {
        // Read metadata from notification for price difference refund
        if (notification && notification.metadata) {
          const { pointsRefundAmount } = notification.metadata;
          if (pointsRefundAmount > 0) {
            member.rewardPoints += pointsRefundAmount;
            await member.save();

            const scoreHistory = new ScoreHistory({
              memberId: member._id,
              bookingId: booking._id,
              earnedPoints: pointsRefundAmount,
              remainingPoints: member.rewardPoints,
              reason: "Hoàn điểm chênh lệch giá vé do dời phòng chiếu (hạ hạng)"
            });
            await scoreHistory.save();
          }
        }

        // 1. Query a VALID, non-deleted, active, non-expired promotion code with available usages from MongoDB
        const now = new Date();
        let promo = await Promotion.findOne({
          isDeleted: { $ne: true },
          startDate: { $lte: now },
          endDate: { $gte: now },
          $or: [
            { maxUsage: -1 },
            { $expr: { $lt: ["$usageCount", "$maxUsage"] } }
          ]
        });

        // Fallback: If no valid active promo found, find any non-deleted promo
        if (!promo) {
          promo = await Promotion.findOne({ isDeleted: { $ne: true } });
        }

        // Fallback 2: Create a standard compensation promo in MongoDB if DB has no promotions
        if (!promo) {
          promo = await Promotion.create({
            title: "Voucher Đền Bù Dời Phòng",
            detail: "Voucher đền bù do hỗ trợ dời phòng chiếu bảo trì",
            couponCode: "VCH_COMPENSATION_" + Date.now().toString().slice(-6),
            discountType: "fixed",
            discountValue: 50000,
            minOrderValue: 0,
            maxUsage: 100,
            usageCount: 0,
            startDate: new Date(Date.now() - 24 * 60 * 60 * 1000),
            endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
          });
        }

        const finalVoucherCode = promo.couponCode;
        const validUntilStr = promo.endDate ? new Date(promo.endDate).toLocaleDateString('vi-VN') : '30 ngày';
        const title = "Mã voucher đền bù đổi phòng";
        const message = `Cảm ơn bạn đã đồng ý đổi phòng chiếu. Chúng tôi gửi tặng bạn 1 voucher xem phim, mã: ${finalVoucherCode} (Hạn sử dụng đến ${validUntilStr}).`;

        const expireAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
        const vNotification = new Notification({
          recipientId: booking.memberId,
          title: title,
          message: message,
          type: "PROMOTION",
          metadata: { voucherCode: finalVoucherCode, promotionId: promo._id.toString() },
          expireAt
        });
        await vNotification.save();

        // 2. Send REAL FCM Push Notification to User's Mobile Device
        if (member.accountId) {
          const account = await Account.findById(member.accountId).select('fcmToken fcmTokens');
          if (account) {
            const tokens = account.fcmTokens && account.fcmTokens.length > 0
              ? account.fcmTokens
              : (account.fcmToken ? [account.fcmToken] : []);
            if (tokens.length > 0) {
              await NotificationService.sendToDevice(tokens, title, message, {
                type: 'promotion',
                referenceId: promo._id.toString(),
                voucherCode: finalVoucherCode
              });
            }
          }
        }
      }
    } else if (action === 'decline') {
      const refundPoints = Math.round(booking.finalAmount / 10);
      if (member) {
        member.rewardPoints += refundPoints;
        await member.save();

        const scoreHistory = new ScoreHistory({
          memberId: member._id,
          bookingId: booking._id,
          earnedPoints: refundPoints,
          remainingPoints: member.rewardPoints,
          reason: "Hoàn điểm 100% do từ chối dời phòng chiếu"
        });
        await scoreHistory.save();
      }

      // Release seats in the alternative showtime
      await ScheduleSeat.deleteMany({
        showtimeId: booking.showtimeId,
        seatId: { $in: booking.seatIds }
      });

      // Send FCM push notification & DB notification for Decline
      const title = "Xác nhận từ chối dời vé";
      const message = `Bạn đã từ chối đổi phòng chiếu. Hệ thống đã hoàn lại 100% điểm thưởng (${refundPoints} điểm) vào ví của bạn.`;
      const dNotification = new Notification({
        recipientId: booking.memberId,
        title: title,
        message: message,
        type: "SYSTEM_ALERT",
        metadata: { bookingId: booking._id.toString(), refundPoints },
        expireAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
      });
      await dNotification.save();

      if (member && member.accountId) {
        const account = await Account.findById(member.accountId).select('fcmToken fcmTokens');
        if (account) {
          const tokens = account.fcmTokens && account.fcmTokens.length > 0
            ? account.fcmTokens
            : (account.fcmToken ? [account.fcmToken] : []);
          if (tokens.length > 0) {
            await NotificationService.sendToDevice(tokens, title, message, {
              type: 'general',
              bookingId: booking._id.toString()
            });
          }
        }
      }
    }

    if (notification) {
      await Notification.deleteOne({ _id: notification._id });
    }
    return booking;
  },

  async previewRestoredSchedule(roomId) {
    const startTimePatch = new Date(Date.now() + 2 * 60 * 60 * 1000);
    const showtimes = await Showtime.find({
      roomId: roomId,
      status: 'suspended',
      startTime: { $gte: startTimePatch }
    });
    return showtimes;
  },

  async confirmRestoredSchedule(showtimeIds) {
    await Showtime.updateMany(
      { _id: { $in: showtimeIds } },
      { $set: { status: 'published' } }
    );
  },

  async handleTimeoutPendingBookings() {
    const thresholdTime = new Date(Date.now() + 30 * 60 * 1000); // 30 minutes in future

    // Find all bookings with paymentStatus: 'pending'
    const pendingBookings = await Booking.find({ paymentStatus: 'pending' });
    const showtimeIds = pendingBookings.map(b => b.showtimeId);

    // Find showtimes starting in less than 30 minutes
    const showtimes = await Showtime.find({
      _id: { $in: showtimeIds },
      startTime: { $lte: thresholdTime }
    });
    const startingShowtimeIdsStr = showtimes.map(s => s._id.toString());

    // Filter bookings
    const timedOutBookings = pendingBookings.filter(b =>
      startingShowtimeIdsStr.includes(b.showtimeId.toString())
    );

    console.log(`[DEBUG] Found ${timedOutBookings.length} pending bookings close to showtime. Processing timeout...`);
    for (const booking of timedOutBookings) {
      await this.resolveUserAction(booking._id, 'decline');
    }
  }
};
