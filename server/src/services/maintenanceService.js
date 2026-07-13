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
  // Module 1: Xử lý Kỹ thuật khi Phòng Hỏng (Active -> Maintenance)
  async handleRoomMaintenance(roomId) {
    const session = await mongoose.startSession();
    session.startTransaction();
    try {
      const room = await CinemaRoom.findById(roomId).session(session);
      if (!room) throw new Error("Room not found");
      
      const now = new Date();
      
      // Tìm các suất chiếu trong tương lai của phòng này
      const showtimes = await Showtime.find({
        roomId: room._id,
        startTime: { $gt: now },
        status: { $in: ['published', 'draft'] }
      }).session(session);

      for (const showtime of showtimes) {
        // Đóng băng suất chiếu gốc
        showtime.status = 'suspended';
        await showtime.save({ session });

        // Tìm các booking hợp lệ của suất chiếu này
        const bookings = await Booking.find({
          showtimeId: showtime._id,
          paymentStatus: 'successful'
        }).session(session);

        if (bookings.length === 0) {
          continue; // 0 vé -> Đổi status suất chiếu thành suspended là đủ
        }

        for (const booking of bookings) {
          let resolved = false;

          // CÁCH A: Đổi phòng
          // 1. Tìm phòng trống
          const availableRooms = await CinemaRoom.find({
            cinemaId: room.cinemaId,
            status: 'active',
            _id: { $ne: room._id }
          }).session(session);

          for (const newRoom of availableRooms) {
            // Kiểm tra xem phòng mới có rảnh không (đơn giản hóa: không có showtime trùng)
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
                // Đủ ghế, tạo suất chiếu mới (Clone)
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

                // Giải phóng schedule_seats cũ và tạo mới
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

                // Cập nhật booking
                booking.showtimeId = newShowtime._id;
                booking.seatIds = newSeats.map(s => s._id);
                booking.paymentStatus = 'pending'; // Chuyển sang chờ xác nhận (ACTION_REQUIRED)
                await booking.save({ session });
                
                // Gửi thông báo (sẽ implement thực tế Firebase sau)
                await this.createNotification(booking.memberId, booking._id, newRoom._id, newShowtime._id, showtime.endTime, session);
                resolved = true;
                break;
              }
            }
          }

          if (resolved) continue;

          // CÁCH B: Gộp suất (Đơn giản hóa logic tìm suất trùng giờ)
          // ... Logic gộp suất tương tự Cách A nhưng tìm showtime khác thay vì tạo mới.
          // Để ngắn gọn, nếu Cách A thất bại, ta đi thẳng vào Fallback Cách C

          // CÁCH C: Fallback - Hủy vé hoàn điểm
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
    const notification = new Notification({
      recipientId: memberId,
      title: "Sự cố phòng chiếu - Cần xác nhận dời vé",
      message: "Rạp đang bảo trì phòng chiếu. Chúng tôi đã chuẩn bị suất chiếu mới cho bạn. Vui lòng xác nhận hoặc hủy để hoàn điểm.",
      type: "ACTION_REQUIRED",
      metadata: { bookingId, newRoomId, newShowtimeId },
      expireAt
    });
    await notification.save({ session });
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
      // Giải phóng ghế
      await ScheduleSeat.deleteMany({
        showtimeId: booking.showtimeId,
        seatId: { $in: booking.seatIds }
      });
      await booking.save();
    } else {
      throw new Error("Invalid action");
    }
    
    // Xóa/ẩn thông báo
    await Notification.deleteOne({ "metadata.bookingId": booking._id });
    return booking;
  },

  // Module 4: Khôi phục Lịch chiếu
  async previewRestoredSchedule(roomId) {
    const startTimePatch = new Date(Date.now() + 2 * 60 * 60 * 1000); // Hiện tại + 2 tiếng
    const showtimes = await Showtime.find({
      roomId: roomId,
      status: { $in: ['suspended', 'cancelled'] },
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
