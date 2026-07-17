import cron from "node-cron";
import { constants } from "../utils/constants.js";
import { Movie } from "../models/movieModel.js";
import { ScheduleSeat } from "../models/scheduleSeatModel.js";
import { StaffShift } from "../models/staffShiftModel.js";
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
  updateStaffShiftStatus,
  resolveMaintenanceTimeouts

};