import cron from "node-cron";
import { constants } from "../utils/constants.js";
import { Movie } from "../models/movieModel.js";
import { ScheduleSeat } from "../models/scheduleSeatModel.js";
import { StaffShift } from "../models/staffShiftModel.js";
import { LeaveRequest } from "../models/leaveRequestModel.js";
const startSeatCleanupJob = async () => {
  const now = new Date();

  await ScheduleSeat.deleteMany(
    {
      holdExpiresAt: { $lt: now },
      status: "holding",
    }
  );
};
cron.schedule("*/30 * * * * *", startSeatCleanupJob);

const updateMovieStatus = async () => {
  try {
    const now = new Date();
    const showingResult = await Movie.updateMany(
      {
        releaseDate: { $lte: now },
        endDate: { $gte: now },
        status: "coming-soon"
      },
      {
        $set: { status: "showing" }
      }
    );

    const endedResult = await Movie.updateMany(
      {
        endDate: { $lt: now },
        status: { $in: ["showing", "coming-soon"] }
      },
      {
        $set: { status: "ended" }
      }
    );

  } catch (error) {
    console.error("[Cron Job Error] Failed to update movie status:", error);
  }
}
cron.schedule("0 0 * * *", updateMovieStatus);


const updateStaffShiftStatus = async () => {
  const now = new Date();

  const activeShifts = await StaffShift.find({
    status: { $in: ["Assigned", "Active"] }
  });

  if (activeShifts.length === 0) return;

  const bulkOps = [];

  for (const shiftItem of activeShifts) {
    const workDate = new Date(shiftItem.workDate);
    const startTime = new Date(workDate);
    const endTime = new Date(workDate);

    // Thiết lập giờ bắt đầu và kết thúc của ca
    switch (shiftItem.shift) {
      case constants.SHIFTS[0]:
        startTime.setHours(8, 0, 0, 0);
        endTime.setHours(12, 0, 0, 0);
        break;
      case constants.SHIFTS[1]:
        startTime.setHours(12, 0, 0, 0);
        endTime.setHours(16, 0, 0, 0);
        break;
      case constants.SHIFTS[2]:
        startTime.setHours(16, 0, 0, 0);
        endTime.setHours(20, 0, 0, 0);
        break;
      case constants.SHIFTS[3]:
        startTime.setHours(20, 0, 0, 0);
        endTime.setDate(endTime.getDate() + 1);
        endTime.setHours(0, 0, 0, 0);
        break;
      default:
        continue;
    }

    let newStatus = shiftItem.status;

    if (now >= endTime) {
      // Auto Completed
      newStatus = "Completed";
    } else if (now >= startTime && now < endTime) {
      // Auto Active
      newStatus = "Active";
    }

    // Nếu trạng thái tính toán thực tế khác trạng thái hiện tại trong DB -> Tạo lệnh update
    if (newStatus !== shiftItem.status) {
      bulkOps.push({
        updateOne: {
          filter: { _id: shiftItem._id },
          update: { $set: { status: newStatus } }
        }
      });
    }
  }

  // Tiến hành cập nhật đồng loạt nếu có sự thay đổi
  if (bulkOps.length > 0) {
    await StaffShift.bulkWrite(bulkOps);
    console.log(`[Cron Job] Automatically updated ${bulkOps.length} staff shifts status.`);
  }
};

cron.schedule("0 0 * * * *", updateStaffShiftStatus);




// Xử lý Hết hạn Phản hồi Bảo trì phòng (Timeout Cronjob) theo SLA động
const resolveMaintenanceTimeouts = async () => {
  try {
    const { Notification } = await import('../models/notificationModel.js');
    const { MaintenanceService } = await import('../services/maintenanceService.js');

    // Tìm các thông báo yêu cầu hành động
    const notifications = await Notification.find({ type: "ACTION_REQUIRED" });
    const now = new Date();

    for (const notif of notifications) {
      const createdAt = notif._id.getTimestamp();
      const diffMins = (now.getTime() - createdAt.getTime()) / (1000 * 60);
      const slaLimit = notif.metadata?.slaTimeoutMinutes || 30;
      const isExpired = notif.metadata?.expiresAt
        ? (now >= new Date(notif.metadata.expiresAt))
        : (diffMins >= slaLimit);

      if (isExpired) {
        if (!notif.metadata || !notif.metadata.bookingId) {
          await Notification.deleteOne({ _id: notif._id });
          continue;
        }
        // Quá hạn SLA -> tự động thực hiện phương án fallback (decline -> hoàn 100% điểm)
        try {
          await MaintenanceService.resolveUserAction(notif.metadata.bookingId, 'decline');
          console.log(`[SLA TIMEOUT] Auto-processed fallback (decline & refund) for booking ${notif.metadata.bookingId} after ${Math.round(diffMins)}m SLA timeout`);
        } catch (e) {
          if (e.statusCode === 409 || e.message?.includes('Booking not found')) {
            await Notification.deleteOne({ _id: notif._id });
            console.log(`Cleaned up stale ACTION_REQUIRED notification for booking ${notif.metadata.bookingId}`);
          } else {
            console.error("Error auto-declining maintenance transfer:", e);
          }
        }
      }
    }
  } catch (error) {
    console.error("Error in resolveMaintenanceTimeouts job:", error);
  }
};

cron.schedule("*/5 * * * *", resolveMaintenanceTimeouts); // Chạy mỗi 5 phút


// Auto Reject Pending Leave Requests when shifts start
const autoRejectAndAbsentJob = async () => {
  try {
    const now = new Date();

    const pendingRequests = await LeaveRequest.find({
      status: "Pending"
    }).populate("staffShiftId");

    const bulkOps = [];

    for (const req of pendingRequests) {
      const shift = req.staffShiftId;
      if (!shift) continue;

      const workDate = new Date(shift.workDate);
      const startTime = new Date(workDate);

      switch (shift.shift) {
        case constants.SHIFTS[0]:
          startTime.setHours(8, 0, 0, 0);
          break;
        case constants.SHIFTS[1]:
          startTime.setHours(12, 0, 0, 0);
          break;
        case constants.SHIFTS[2]:
          startTime.setHours(16, 0, 0, 0);
          break;
        case constants.SHIFTS[3]:
          startTime.setHours(20, 0, 0, 0);
          break;
        default:
          continue;
      }

      if (startTime <= now) {
        bulkOps.push({
          updateOne: {
            filter: { _id: req._id },
            update: {
              $set: {
                status: "Rejected",
                rejectReason: "Request expired because the shift has already started."
              }
            }
          }
        });
      }
    }

    if (bulkOps.length > 0) {
      await LeaveRequest.bulkWrite(bulkOps);
      console.log(
        `[Cron Job] Auto rejected ${bulkOps.length} expired leave requests.`
      );
    }
  } catch (error) {
    console.error("[Cron Job Error]", error);
  }
};

cron.schedule("*/1 * * * *", autoRejectAndAbsentJob);

// Auto Update Movie Tiers based on release date and recent booking performance (Hybrid Logic)
const autoUpdateMovieTiers = async () => {
  try {
    const now = new Date();
    const sevenDaysAgo = new Date(now);
    sevenDaysAgo.setDate(now.getDate() - 7);
    
    // Get all showing movies that have autoTier enabled
    const activeMovies = await Movie.find({ 
      status: { $in: ["showing"] }, 
      isDeleted: false,
      isAutoTier: true
    });

    if (activeMovies.length === 0) return;

    // Dynamically import to prevent any circular dependencies
    const { Booking } = await import('../models/bookingModel.js');
    
    // Aggregate successful bookings in the last 7 days grouped by movieId
    const bookingStats = await Booking.aggregate([
      { 
        $match: { 
          paymentStatus: 'successful',
          createdAt: { $gte: sevenDaysAgo }
        }
      },
      {
        $lookup: {
          from: 'showtimes',
          localField: 'showtimeId',
          foreignField: '_id',
          as: 'showtime'
        }
      },
      { $unwind: '$showtime' },
      {
        $group: {
          _id: '$showtime.movieId',
          totalBookings: { $sum: 1 }
        }
      }
    ]);

    // Create a map for quick lookup: { "movieId_string": totalBookings }
    const bookingMap = {};
    bookingStats.forEach(stat => {
      bookingMap[stat._id.toString()] = stat.totalBookings;
    });

    const bulkOps = [];

    for (const movie of activeMovies) {
      if (!movie.releaseDate) continue;

      const diffTime = Math.abs(now - new Date(movie.releaseDate));
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      const recentBookings = bookingMap[movie._id.toString()] || 0;
      
      let newTier = movie.tier;
      
      // Default auto-tiering logic based on time decay
      if (diffDays <= 14) {
        newTier = 1; // Tier 1 for first 14 days
      } else if (diffDays <= 30) {
        newTier = 2; // Tier 2 for days 15-30
      } else {
        newTier = 3; // Tier 3 for older than 30 days
      }

      // Booking Overrides (Hybrid factors)
      if (diffDays > 14 && recentBookings > 100) {
        newTier = 1; // Keep it at Tier 1 because it's a hot movie
      } else if (diffDays > 30 && recentBookings > 50) {
        newTier = 2; // Keep it at Tier 2 instead of dropping to Tier 3
      } else if (diffDays <= 14 && recentBookings < 10) {
        newTier = 2; // Flop movie, drop to Tier 2 early
      }

      if (newTier !== movie.tier) {
        bulkOps.push({
          updateOne: {
            filter: { _id: movie._id },
            update: { $set: { tier: newTier } }
          }
        });
      }
    }

    if (bulkOps.length > 0) {
      await Movie.bulkWrite(bulkOps);
      console.log(`[Cron Job] Automatically updated tier for ${bulkOps.length} movies (Hybrid Logic).`);
    }

  } catch (error) {
    console.error("[Cron Job Error] Failed to auto-update movie tiers:", error);
  }
};

cron.schedule("0 0 * * *", autoUpdateMovieTiers); // Runs every day at midnight

export const CleanUpJobs = {
  startSeatCleanupJob,
  updateMovieStatus,
  updateStaffShiftStatus,
  resolveMaintenanceTimeouts,
  autoRejectAndAbsentJob,
  autoUpdateMovieTiers
};