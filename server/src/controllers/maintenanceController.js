import { MaintenanceService } from '../services/maintenanceService.js';
import { StatusCodes } from 'http-status-codes';

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

  // API Tương tác User
  async resolveUserAction(req, res, next) {
    try {
      const { id } = req.params;
      const { action } = req.body; // 'accept' or 'decline'
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
