import { MaintenanceService } from '../services/maintenanceService.js';
import { StatusCodes } from 'http-status-codes';
import ApiError from '../utils/ApiError.js';
import { Booking } from '../models/bookingModel.js';
import { Member } from '../models/memberModel.js';

export const MaintenanceController = {
  // Bật/tắt bảo trì phòng (Giả lập trigger bảo trì phòng)
  async triggerMaintenance(req, res, next) {
    try {
      const { roomId } = req.params;
      const result = await MaintenanceService.handleRoomMaintenance(roomId);
      res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
      next(error);
    }
  },

  // Xử lý báo ghế hỏng trong phòng
  async handleBrokenSeats(req, res, next) {
    try {
      const { roomId } = req.params;
      const { brokenSeatIds } = req.body;
      const result = await MaintenanceService.handleBrokenSeats(roomId, brokenSeatIds);
      res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
      next(error);
    }
  },

  // API Tương tác User
  async resolveUserAction(req, res, next) {
    try {
      const { id } = req.params;
      const { action } = req.body; // 'accept' or 'decline'
      
      // Ownership check (STT 17)
      const booking = await Booking.findById(id);
      if (!booking) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Booking not found");
      }
      const member = await Member.findById(booking.memberId);
      if (!member || member.accountId.toString() !== req.user.accountId.toString()) {
        throw new ApiError(StatusCodes.FORBIDDEN, "Access Denied: You do not own this booking!");
      }

      const result = await MaintenanceService.resolveUserAction(id, action);
      res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
      next(error);
    }
  },

  // Khôi phục Lịch chiếu
  async previewRestoredSchedule(req, res, next) {
    try {
      const { roomId } = req.params;
      const result = await MaintenanceService.previewRestoredSchedule(roomId);
      res.status(StatusCodes.OK).json({ success: true, data: result });
    } catch (error) {
      next(error);
    }
  },

  async confirmRestoredSchedule(req, res, next) {
    try {
      const { showtimeIds } = req.body;
      await MaintenanceService.confirmRestoredSchedule(showtimeIds);
      res.status(StatusCodes.OK).json({ success: true, message: "Schedules restored successfully" });
    } catch (error) {
      next(error);
    }
  }
};
