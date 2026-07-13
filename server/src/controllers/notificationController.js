import { Notification } from '../models/notificationModel.js';
import { StatusCodes } from 'http-status-codes';

export const NotificationController = {
  // 1. Lấy danh sách thông báo của 1 Member (cho biểu tượng chuông)
  async getUserNotifications(req, res, next) {
    try {
      // Giả định req.user chứa thông tin đăng nhập, hoặc truyền memberId qua params/query
      const { memberId } = req.params; 
      
      const notifications = await Notification.find({ recipientId: memberId })
        .sort({ createdAt: -1 }) // Mới nhất lên đầu
        .limit(50); // Chỉ lấy 50 thông báo gần nhất
        
      res.status(StatusCodes.OK).json({ success: true, data: notifications });
    } catch (error) {
      next(error);
    }
  },

  // 2. Đánh dấu 1 thông báo là đã đọc
  async markAsRead(req, res, next) {
    try {
      const { id } = req.params;
      
      const notification = await Notification.findByIdAndUpdate(
        id, 
        { isRead: true },
        { new: true }
      );
      
      if (!notification) {
        return res.status(StatusCodes.NOT_FOUND).json({ success: false, message: "Notification not found" });
      }
      
      res.status(StatusCodes.OK).json({ success: true, data: notification });
    } catch (error) {
      next(error);
    }
  },

  // 3. Lấy số lượng thông báo chưa đọc (để hiển thị số chấm đỏ trên chuông)
  async getUnreadCount(req, res, next) {
    try {
      const { memberId } = req.params; 
      
      const count = await Notification.countDocuments({ 
        recipientId: memberId, 
        isRead: false 
      });
      
      res.status(StatusCodes.OK).json({ success: true, count });
    } catch (error) {
      next(error);
    }
  }
};
