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

export const MaintenanceService = {
  async handleRoomMaintenance(roomId) {
    const session = await mongoose.startSession();
    session.startTransaction();
    try {
      const room = await CinemaRoom.findById(roomId).session(session);
      if (!room) throw new Error("Room not found");

      const now = new Date();

      const showtimes = await Showtime.find({
        roomId: room._id,
        isDeleted: false
      }).session(session);

      console.log(`[DEBUG] handleRoomMaintenance: found ${showtimes.length} showtimes for room ${room.roomName}`);

      for (const showtime of showtimes) {
        const bookings = await Booking.find({
          showtimeId: showtime._id,
          paymentStatus: 'paid',
          checkedIn: { $ne: true }
        }).session(session);

        console.log(`[DEBUG] Showtime ${showtime._id} (${showtime.showTime}): bookings count = ${bookings.length}`);

        if (bookings.length === 0) {
          console.log(`[DEBUG] Showtime ${showtime._id} has 0 bookings. Deleting...`);
          await ScheduleSeat.deleteMany({ showtimeId: showtime._id }).session(session);
          await Showtime.deleteOne({ _id: showtime._id }).session(session);
          continue;
        }

        if (showtime.endTime <= now) {
          console.log(`[DEBUG] Showtime ${showtime._id} is in the past. Skipping...`);
          continue;
        }

        // Suspend showtime
        showtime.status = 'suspended';
        await showtime.save({ session });
        console.log(`[DEBUG] Suspended showtime ${showtime._id}`);

        // --- STEP 1: Gather original seat counts (VIP vs Regular) ---
        const seatIds = bookings.flatMap(b => b.seatIds);
        const bookedSeats = await Seat.find({ _id: { $in: seatIds } }).session(session);
        const bookedSeatsMap = {};
        bookedSeats.forEach(s => {
          bookedSeatsMap[s._id.toString()] = s.seatType; // 'VIP' or 'Regular'
        });

        let soVeVIP = 0;
        let soVeThuong = 0;
        bookings.forEach(b => {
          b.seatIds.forEach(sid => {
            const stype = bookedSeatsMap[sid.toString()] || 'Regular';
            if (stype === 'VIP') soVeVIP++;
            else soVeThuong++;
          });
        });
        const tongSoVe = soVeVIP + soVeThuong;
        console.log(`[DEBUG] Total tickets: ${tongSoVe} (VIP: ${soVeVIP}, Regular: ${soVeThuong})`);

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

              // Re-populate all seats in ScheduleSeat for the new room so it's a complete showtime
              const allRoomSeats = await Seat.find({ roomId: newRoom._id }).session(session);
              const newSeatsMap = {};
              newSeats.forEach(s => {
                newSeatsMap[s.seatName] = s._id;
              });

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
                booking.paymentStatus = 'paid'; // Auto-confirm, no change in paymentStatus
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
          const tongGheTrong = emptySeats.length;

          if (tongGheTrong >= tongSoVe) {
            candidates.push({
              altSt,
              altRoom,
              emptySeats,
              emptyVip,
              emptyRegular,
              tongGheTrong
            });
          }
        }

        console.log(`[DEBUG] Found ${candidates.length} candidate alternative showtimes with enough seats.`);

        if (candidates.length === 0) {
          // --- STEP 3a: No Candidate Fits -> Refund All ---
          console.log(`[DEBUG] No candidate showtimes fit. Refunding all bookings.`);
          for (const booking of bookings) {
            booking.paymentStatus = 'refunded_to_points';

            const member = await Member.findById(booking.memberId).session(session);
            const refundPoints = Math.round(booking.finalAmount / 10);

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
          continue; // Done with this showtime
        }

        // Rank candidates
        // Priority 1: gV >= soVeVIP
        // Priority 2: largest gV
        candidates.sort((a, b) => {
          const matchA = a.emptyVip.length >= soVeVIP ? 1 : 0;
          const matchB = b.emptyVip.length >= soVeVIP ? 1 : 0;
          if (matchA !== matchB) return matchB - matchA; // Match first
          return b.emptyVip.length - a.emptyVip.length; // Largest VIP count next
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

        // Delete old schedule seats for original bookings
        await ScheduleSeat.deleteMany({
          showtimeId: showtime._id,
          seatId: { $in: seatIds }
        }).session(session);

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
          let refundPoints = 0;

          // Attempt consecutive allocation for group bookings (N >= 2) of the same seat type
          let consecutiveFound = false;
          if (booking.seatIds.length >= 2) {
            const firstType = bookingSeats[0].seatType;
            const allSameType = bookingSeats.every(s => s.seatType === firstType);
            
            if (allSameType) {
              const targetEmptySeats = firstType === 'VIP' ? emptyVips : emptyRegulars;
              const consecutiveRun = findConsecutiveSeats(targetEmptySeats, booking.seatIds.length);
              if (consecutiveRun) {
                assignedNewSeats = consecutiveRun;
                consecutiveFound = true;
                
                // Remove assigned seats from correct array
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
                // Regular
                if (emptyRegulars.length > 0) {
                  assignedNewSeats.push(emptyRegulars.shift());
                } else {
                  // Upgrade to VIP
                  assignedNewSeats.push(emptyVips.shift());
                  upgradedRegularCount++;
                }
              }
            }
          }

          const member = await Member.findById(booking.memberId).session(session);
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

          const newSeatNames = assignedNewSeats.map(s => s.seatName);
          const newSeatIds = assignedNewSeats.map(s => s._id);

          // Update booking immediately to pending
          booking.showtimeId = altSt._id;
          booking.seatIds = newSeatIds;
          booking.seatNames = newSeatNames;
          booking.paymentStatus = 'pending';
          await booking.save({ session });

          // Generate voucher code (Mock)
          const voucherCode = 'VCH_AUTO_' + Math.random().toString(36).substring(2, 8).toUpperCase();

          // Title & message template
          const title = "Sự cố phòng chiếu - Cần xác nhận dời vé";
          let changeDetailsMsg = "";
          if (downgradedVipCount > 0) {
            changeDetailsMsg = `Ghế VIP của bạn được chuyển sang ghế thường [${newSeatNames.join(', ')}] và hoàn lại ${refundPoints * 10} VNĐ (${refundPoints} điểm) + 1 Voucher đền bù.`;
          } else if (upgradedRegularCount > 0) {
            changeDetailsMsg = `Ghế Thường của bạn được nâng hạng lên ghế VIP [${newSeatNames.join(', ')}] miễn phí + 1 Voucher đền bù.`;
          } else {
            changeDetailsMsg = `Ghế của bạn được chuyển sang [${newSeatNames.join(', ')}] cùng hạng + 1 Voucher đền bù.`;
          }
          const isSeparated = booking.seatIds.length >= 2 && !consecutiveFound;
          const separationWarning = isSeparated ? " (Do phòng mới không đủ cụm ghế liền kề, nhóm của bạn buộc phải xếp tách ghế. Rất mong bạn thông cảm.)" : "";
          const message = `Suất chiếu [Tên phim] tại phòng ${room.roomName} gặp sự cố. Chúng tôi đã chuyển sang phòng ${altRoom.roomName} cùng khung giờ. ${changeDetailsMsg}${separationWarning} Bạn có đồng ý đổi phòng chiếu không?`;

          // Store proposal details in notification metadata
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
              downgradedVipCount
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

      await session.commitTransaction();
      session.endSession();
      return { success: true, message: 'Room maintenance handled' };
    } catch (error) {
      await session.abortTransaction();
      session.endSession();
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

      const member = await Member.findById(memberId).session(session);
      if (member && member.accountId) {
        const account = await Account.findById(member.accountId).select('fcmToken fcmTokens').session(session);
        if (account) {
          const tokens = account.fcmTokens && account.fcmTokens.length > 0
            ? account.fcmTokens
            : (account.fcmToken ? [account.fcmToken] : [])

          if (tokens.length > 0) {
            await NotificationService.sendToDevice(tokens, title, message, {
              type: 'MAINTENANCE_ACTION_REQUIRED',
              bookingId: bookingId.toString()
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

      const member = await Member.findById(memberId).session(session);
      if (member && member.accountId) {
        const account = await Account.findById(member.accountId).select('fcmToken fcmTokens').session(session);
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
      { $set: { paymentStatus: action === 'accept' ? 'confirmed_transfer' : 'refunded_to_points' } },
      { new: true }
    );
    if (!booking) {
      throw new ApiError(StatusCodes.CONFLICT, "Booking not found or not in pending transfer state");
    }

    const notification = await Notification.findOne({ "metadata.bookingId": booking._id });

    if (action === 'accept') {
      // Read metadata from notification for compensation
      if (notification && notification.metadata) {
        const { pointsRefundAmount, voucherCode } = notification.metadata;
        const member = await Member.findById(booking.memberId);
        
        if (member) {
          // If they were downgraded, refund the price difference
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

          // Send voucher notification to user
          const title = "Mã voucher đền bù đổi phòng";
          const message = `Cảm ơn bạn đã đồng ý đổi phòng chiếu. Chúng tôi gửi tặng bạn 1 voucher xem phim miễn phí, mã: ${voucherCode}, hạn sử dụng 30 ngày.`;
          const expireAt = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
          const vNotification = new Notification({
            recipientId: booking.memberId,
            title: title,
            message: message,
            type: "PROMOTION",
            metadata: { voucherCode },
            expireAt
          });
          await vNotification.save();
        }
      }
    } else if (action === 'decline') {
      const member = await Member.findById(booking.memberId);
      if (member) {
        const refundPoints = Math.round(booking.finalAmount / 10);
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

      // Send cancel notification
      await this.createCancelNotification(booking.memberId, booking._id, Math.round(booking.finalAmount / 10), null);
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
