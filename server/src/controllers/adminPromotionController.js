import * as adminPromotionService from '../services/adminPromotionService.js';

export const createPromotion = async (req, res) => {
    try {
        const { title, detail, discountLevel, startTime, endTime } = req.body;

        if (!title || !detail || discountLevel === undefined || !startTime || !endTime) {
            return res.status(400).json({ success: false, message: 'Missing required promotion details.' });
        }

        if (new Date(startTime) >= new Date(endTime)) {
            return res.status(400).json({ success: false, message: 'End time must be after start time.' });
        }

        const promotion = await adminPromotionService.createPromotion(req.body);
        return res.status(201).json({ success: true, data: promotion, message: 'Promotion created successfully.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

export const updatePromotion = async (req, res) => {
    try {
        const { id } = req.params;
        const promotion = await adminPromotionService.updatePromotion(id, req.body);
        
        if (!promotion) return res.status(404).json({ success: false, message: 'Promotion not found.' });
        
        return res.status(200).json({ success: true, data: promotion, message: 'Promotion updated successfully.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

export const deletePromotion = async (req, res) => {
    try {
        const { id } = req.params;
        const promotion = await adminPromotionService.deletePromotion(id);
        
        if (!promotion) return res.status(404).json({ success: false, message: 'Promotion not found.' });
        
        return res.status(200).json({ success: true, message: 'Promotion deleted successfully.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};
