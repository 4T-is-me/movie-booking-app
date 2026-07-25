import mongoose from 'mongoose';
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
        endTime: { $gt: now },
        status: { $ne: 'suspended' }
      }).session(session);

      for (const showtime of showtimes) {
        const bookings = await Booking.find({
          showtimeId: showtime._id,
          paymentStatus: 'paid'
        }).session(session);

        if (bookings.length === 0) {
          // Xóa suất chiếu (và các ghế đang được giữ nếu có) khỏi CSDL nếu chưa có vé thanh toán
          await ScheduleSeat.deleteMany({ showtimeId: showtime._id }).session(session);
          await Showtime.deleteOne({ _id: showtime._id }).session(session);
          continue;
        }

        // Với các suất chiếu đã có booking, giữ nguyên không xóa mà chuyển sang trạng thái suspended để dời phòng
        showtime.status = 'suspended';
        await showtime.save({ session });

        for (const booking of bookings) {
          let resolved = false;


          const availableRooms = await CinemaRoom.find({
            cinemaId: room.cinemaId,
            status: 'active',
            _id: { $ne: room._id }
          }).session(session);

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
              // Seat Mapping
              const newSeats = await Seat.find({
                roomId: newRoom._id,
                seatName: { $in: booking.seatNames }
              }).session(session);

              if (newSeats.length === booking.seatNames.length) {
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

                await ScheduleSeat.deleteMany({
                  showtimeId: showtime._id,
                  seatId: { $in: booking.seatIds }
                }).session(session);

                const member = await Member.findById(booking.memberId).session(session);

                const newScheduleSeats = newSeats.map(seat => ({
                  showtimeId: newShowtime._id,
                  seatId: seat._id,
                  accountId: member ? member.accountId : null,
                  status: 'booked'
                }));
                await ScheduleSeat.insertMany(newScheduleSeats, { session });

                booking.showtimeId = newShowtime._id;
                booking.seatIds = newSeats.map(s => s._id);
                booking.paymentStatus = 'pending';
                await booking.save({ session });

                await this.createNotification(booking.memberId, booking._id, newRoom._id, newShowtime._id, showtime.endTime, session);
                resolved = true;
                break;
              }
            }
          }

          // Added by Antigravity - Incident Management (Cách B) - 2026-07-23
          if (!resolved) {
            const timeWindow = 15 * 60 * 1000;
            const targetMinTime = new Date(showtime.startTime.getTime() - timeWindow);
            const targetMaxTime = new Date(showtime.startTime.getTime() + timeWindow);

            const alternativeShowtimes = await Showtime.find({
              cinemaId: showtime.cinemaId,
              movieId: showtime.movieId,
              status: { $in: ['published', 'draft'] },
              startTime: { $gte: targetMinTime, $lte: targetMaxTime },
              _id: { $ne: showtime._id }
            }).session(session);

            for (const altShowtime of alternativeShowtimes) {
              const altRoomSeats = await Seat.find({ roomId: altShowtime.roomId }).session(session);
              const bookedSeats = await ScheduleSeat.find({ showtimeId: altShowtime._id, status: { $in: ['booked', 'holding'] } }).session(session);

              const bookedSeatIds = bookedSeats.map(s => s.seatId.toString());
              const availableSeats = altRoomSeats.filter(s => !bookedSeatIds.includes(s._id.toString()));

              if (availableSeats.length >= booking.seatNames.length) {
                const selectedSeats = availableSeats.slice(0, booking.seatNames.length);
                const member = await Member.findById(booking.memberId).session(session);

                const priceDiff = (showtime.ticketPrice - altShowtime.ticketPrice) * booking.seatNames.length;
                if (priceDiff > 0 && member) {
                  member.rewardPoints += priceDiff;
                  await member.save({ session });

                  const scoreHistory = new ScoreHistory({
                    memberId: member._id,
                    bookingId: booking._id,
                    earnedPoints: priceDiff,
                    remainingPoints: member.rewardPoints,
                    reason: "Hoàn tiền chênh lệch vé do dời suất chiếu"
                  });
                  await scoreHistory.save({ session });
                }

                await ScheduleSeat.deleteMany({
                  showtimeId: showtime._id,
                  seatId: { $in: booking.seatIds }
                }).session(session);

                const newScheduleSeats = selectedSeats.map(seat => ({
                  showtimeId: altShowtime._id,
                  seatId: seat._id,
                  accountId: member ? member.accountId : null,
                  status: 'booked'
                }));
                await ScheduleSeat.insertMany(newScheduleSeats, { session });

                booking.showtimeId = altShowtime._id;
                booking.seatIds = selectedSeats.map(s => s._id);
                booking.seatNames = selectedSeats.map(s => s.seatName);
                booking.paymentStatus = 'pending';
                await booking.save({ session });

                await this.createNotification(booking.memberId, booking._id, altShowtime.roomId, altShowtime._id, altShowtime.endTime, session);
                resolved = true;
                break;
              }
            }
          }

          if (resolved) continue;

          booking.paymentStatus = 'refunded_to_points';

          const member = await Member.findById(booking.memberId).session(session);
          if (member) {
            member.rewardPoints += booking.finalAmount;
            await member.save({ session });

            const scoreHistory = new ScoreHistory({
              memberId: member._id,
              bookingId: booking._id,
              earnedPoints: booking.finalAmount,
              remainingPoints: member.rewardPoints,
              reason: "Hoàn điểm do hệ thống hủy vé (bảo trì phòng chiếu)"
            });
            await scoreHistory.save({ session });
          }

          await ScheduleSeat.deleteMany({
            showtimeId: showtime._id,
            seatId: { $in: booking.seatIds }
          }).session(session);

          await booking.save({ session });

          await this.createCancelNotification(booking.memberId, booking._id, booking.finalAmount, session);
        }
      }

      await session.commitTransaction();
      session.endSession();
      return { success: true, message: "Room maintenance handled" };
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
      const { notificationService } = await import('./notificationService.js');
      const { Account } = await import('../models/accountModel.js');

      const member = await Member.findById(memberId).session(session);
      if (member && member.accountId) {
        const account = await Account.findById(member.accountId).select('fcmToken fcmTokens').session(session);
        if (account) {
          const tokens = account.fcmTokens && account.fcmTokens.length > 0
            ? account.fcmTokens
            : (account.fcmToken ? [account.fcmToken] : [])

          if (tokens.length > 0) {
            await notificationService.sendToDevice(tokens, title, message, {
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
      const { notificationService } = await import('./notificationService.js');
      const { Account } = await import('../models/accountModel.js');

      const member = await Member.findById(memberId).session(session);
      if (member && member.accountId) {
        const account = await Account.findById(member.accountId).select('fcmToken fcmTokens').session(session);
        if (account) {
          const tokens = account.fcmTokens && account.fcmTokens.length > 0
            ? account.fcmTokens
            : (account.fcmToken ? [account.fcmToken] : [])

          if (tokens.length > 0) {
            await notificationService.sendToDevice(tokens, title, message, {
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
    const booking = await Booking.findById(bookingId);
    if (!booking || booking.paymentStatus !== 'pending') {
      throw new Error("Booking not found or not in pending transfer state");
    }

    if (action === 'accept') {
      booking.paymentStatus = 'confirmed_transfer';
      await booking.save();
    } else if (action === 'decline') {
      booking.paymentStatus = 'refunded_to_points';
      const member = await Member.findById(booking.memberId);
      if (member) {
        member.rewardPoints += booking.finalAmount;
        await member.save();

        const scoreHistory = new ScoreHistory({
          memberId: member._id,
          bookingId: booking._id,
          earnedPoints: booking.finalAmount,
          remainingPoints: member.rewardPoints,
          reason: "Hoàn điểm do quá hạn phản hồi dời vé (bảo trì phòng)"
        });
        await scoreHistory.save();
      }
      await ScheduleSeat.deleteMany({
        showtimeId: booking.showtimeId,
        seatId: { $in: booking.seatIds }
      });
      await booking.save();
    } else {
      throw new Error("Invalid action");
    }

    await Notification.deleteOne({ "metadata.bookingId": booking._id });
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
  }
};
