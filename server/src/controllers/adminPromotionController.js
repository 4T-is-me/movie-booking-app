import * as adminPromotionService from '../services/adminPromotionService.js';

export const createPromotion = async (req, res) => {
    try {
        const { title, detail, discountLevel, startTime, endTime } = req.body;

        if (!title || !detail || discountLevel === undefined || !startTime || !endTime) {
            return res.status(400).json({ success: false, message: 'Thiếu thông tin khuyến mãi bắt buộc.' });
        }

        if (new Date(startTime) >= new Date(endTime)) {
            return res.status(400).json({ success: false, message: 'Thời gian kết thúc phải sau thời gian bắt đầu.' });
        }

        const promotion = await adminPromotionService.createPromotion(req.body);
        return res.status(201).json({ success: true, data: promotion, message: 'Tạo khuyến mãi thành công.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};

export const updatePromotion = async (req, res) => {
    try {
        const { id } = req.params;
        const promotion = await adminPromotionService.updatePromotion(id, req.body);
        
        if (!promotion) return res.status(404).json({ success: false, message: 'Không tìm thấy khuyến mãi.' });
        
        return res.status(200).json({ success: true, data: promotion, message: 'Cập nhật thành công.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};

export const deletePromotion = async (req, res) => {
    try {
        const { id } = req.params;
        const promotion = await adminPromotionService.deletePromotion(id);
        
        if (!promotion) return res.status(404).json({ success: false, message: 'Không tìm thấy khuyến mãi.' });
        
        return res.status(200).json({ success: true, message: 'Xóa khuyến mãi thành công.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Lỗi server', error: error.message });
    }
};
