import cron from "node-cron";
import { constants } from "../utils/constants.js";
import { Movie } from "../models/movieModel.js";
import { ScheduleSeat } from "../models/scheduleSeatModel.js";

 const startSeatCleanupJob  = async () => {
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
    const now = new Date();

    // Coming Soon -> Showing
    await Movie.updateMany(
        {
            releaseDate: { $lte: now },
            endDate: { $gte: now },
        status: "coming_soon"
        },
        {
            $set: { status: "showing" }
        }
    );

    // Showing -> Ended
    await Movie.updateMany(
        {
            endDate: { $lt: now },
            status: "showing"
        },
        {
            $set: { status: "ended" }
        }
    );
};
//update every day(00:00)
cron.schedule("0 0 * * *", updateMovieStatus);

// Xử lý Hết hạn Phản hồi Bảo trì phòng (Timeout Cronjob)
const resolveMaintenanceTimeouts = async () => {
  try {
    const { Notification } = await import('../models/notificationModel.js');
    const { MaintenanceService } = await import('../services/maintenanceService.js');
    
    // Tìm các thông báo yêu cầu hành động
    const notifications = await Notification.find({ type: "ACTION_REQUIRED" });
    const now = new Date();
    
    for (const notif of notifications) {
      const createdAt = notif._id.getTimestamp();
      const diffMins = (now - createdAt) / (1000 * 60);
      
      if (diffMins >= 30) {
        // Đã quá 30 phút, tự động decline
        try {
          await MaintenanceService.resolveUserAction(notif.metadata.bookingId, 'decline');
          console.log(`Auto-declined maintenance transfer for booking ${notif.metadata.bookingId}`);
        } catch (e) {
          console.error("Error auto-declining maintenance transfer:", e);
        }
      }
    }
  } catch (error) {
    console.error("Error in resolveMaintenanceTimeouts job:", error);
  }
};

cron.schedule("*/5 * * * *", resolveMaintenanceTimeouts); // Chạy mỗi 5 phút

export const CleanUpJobs = {
  startSeatCleanupJob,
  updateMovieStatus,
  resolveMaintenanceTimeouts
};