import * as adminPromotionService from '../services/adminPromotionService.js';
import { NotificationService } from '../services/notificationService.js';

export const getPromotions = async (req, res) => {
    try {
        const promotions = await adminPromotionService.getPromotions(req.query);
        return res.status(200).json({ success: true, data: promotions });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

export const createPromotion = async (req, res) => {
    try {
        const { title, detail, couponCode, discountType, discountValue, startDate, endDate } = req.body;

        if (!title || !detail || !couponCode || !discountType || discountValue === undefined || !startDate || !endDate) {
            return res.status(400).json({ success: false, message: 'Missing required promotion details.' });
        }

        if (new Date(startDate) >= new Date(endDate)) {
            return res.status(400).json({ success: false, message: 'End date must be after start date.' });
        }

        req.body.startTime = new Date(startDate);
        req.body.endTime = new Date(endDate);

        const promotion = await adminPromotionService.createPromotion(req.body);

        // Broadcast to 'promotions' topic asynchronously
        const formattedDate = new Date(endDate).toLocaleDateString('en-US');
        const notifTitle = `New Promotion: ${title}!`;
        const notifBody = `Get amazing offers: ${detail}. Enter coupon code "${couponCode}" when booking. Valid until ${formattedDate}.`;
        
        NotificationService.sendToTopic('promotions', notifTitle, notifBody, {
            type: 'promotion',
            referenceId: promotion._id.toString()
        }).catch(err => {
            console.error('Failed to broadcast promotion notification:', err.message);
        });

        return res.status(201).json({ success: true, data: promotion, message: 'Promotion created successfully.' });
    } catch (error) {
        if (error.code === 11000) { // MongoDB duplicate key error
            return res.status(400).json({ success: false, message: 'Coupon code already exists.' });
        }
        return res.status(500).json({ success: false, message: 'Internal server error.', error: error.message });
    }
};

export const updatePromotion = async (req, res) => {
    try {
        const { id } = req.params;
        const { startDate, endDate } = req.body;

        if (startDate && endDate && new Date(startDate) >= new Date(endDate)) {
            return res.status(400).json({ success: false, message: 'End date must be after start date.' });
        }

        if (startDate) {
            req.body.startTime = new Date(startDate);
        }
        if (endDate) {
            req.body.endTime = new Date(endDate);
        }

        const promotion = await adminPromotionService.updatePromotion(id, req.body);
        
        if (!promotion) return res.status(404).json({ success: false, message: 'Promotion not found.' });
        
        return res.status(200).json({ success: true, data: promotion, message: 'Promotion updated successfully.' });
    } catch (error) {
        if (error.code === 11000) {
            return res.status(400).json({ success: false, message: 'Coupon code already exists.' });
        }
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

export const broadcastPushNotification = async (req, res) => {
    try {
        const { title, body, type, referenceId } = req.body;

        if (!title || !body) {
            return res.status(400).json({ success: false, message: 'Title and body are required for broadcasting.' });
        }

        const dataPayload = {};
        if (type) dataPayload.type = type;
        if (referenceId) dataPayload.referenceId = referenceId;

        // Send to topic promotions so everyone subscribed gets it
        await NotificationService.sendToTopic('promotions', title, body, dataPayload);

        return res.status(200).json({ success: true, message: 'Custom broadcast notification sent successfully.' });
    } catch (error) {
        return res.status(500).json({ success: false, message: 'Failed to send broadcast.', error: error.message });
    }
};
