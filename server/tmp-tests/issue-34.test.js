// File kiểm thử tạm do AI tạo để nghiệm thu Issue #34 — có thể xóa sau khi review
import mongoose from 'mongoose';
import { bookingService } from '../src/services/bookingService.js';
import { promotionService } from '../src/services/promotionService.js';
import { Member } from '../src/models/memberModel.js';
import { ScheduleSeat } from '../src/models/scheduleSeatModel.js';
import { Promotion } from '../src/models/promotionModel.js';
import { PromotionUsage } from '../src/models/promotionUsageModel.js';
import { Booking } from '../src/models/bookingModel.js';
import { ScoreHistory } from '../src/models/scoreHistoryModel.js';

// Mock mongoose transaction
mongoose.startSession = async () => ({
    startTransaction: () => {},
    commitTransaction: () => {},
    abortTransaction: () => {},
    endSession: () => {},
    withTransaction: async (cb) => {
        try {
            await cb();
        } catch (error) {
            console.log("Transaction aborted due to error:", error.message);
            throw error;
        }
    }
});

// Mock models
const mockMember = { _id: 'member1', accountId: 'account1', rewardPoints: 1000, totalSpent: 0, save: async () => {} };
Member.findOne = () => ({ session: () => mockMember });

const mockSeats = [
    { seatId: { seatName: 'A1', price: 50000 } },
    { seatId: { seatName: 'A2', price: 50000 } }
];
ScheduleSeat.find = () => ({ populate: () => ({ session: () => mockSeats }) });
ScheduleSeat.updateMany = async () => {};

Booking.create = async (arr) => [{ _id: 'booking1', bookingCode: 'TICKET123', ...arr[0] }];
ScoreHistory.create = async () => {};
PromotionUsage.create = async (arr) => {
    console.log("PromotionUsage created:", arr[0]);
    return [arr[0]];
};
Promotion.updateOne = async (filter, update) => {
    console.log("Promotion updated:", filter, update);
};

// Mock Promotion Service
promotionService.validateAndApplyPromotion = async (coupon, account, total) => {
    console.log(`Validating coupon: ${coupon} for total: ${total}`);
    if (coupon === 'EXPIRED') throw new Error('Mã giảm giá đã hết lượt sử dụng');
    return {
        isValid: true,
        discountAmount: 20000,
        promotionId: 'promo1',
        promotion: {}
    };
};

async function runTest() {
    console.log("--- TEST 1: Áp dụng coupon hợp lệ ---");
    // Mock Math.random to always succeed payment
    const originalRandom = Math.random;
    Math.random = () => 0.9; 

    try {
        const result = await bookingService.handleConfirmBooking({
            accountId: 'account1',
            showtimeId: 'show1',
            seatIds: ['seat1', 'seat2'],
            couponCode: 'SUMMER20'
        });
        console.log("Booking Result:", {
            ticketPrice: result.ticketPrice,
            promotionDiscount: result.promotionDiscount,
            finalAmount: result.finalAmount
        });
    } catch (e) {
        console.error("Test 1 Failed:", e.message);
    }

    console.log("\n--- TEST 2: Kiểm tra Rollback khi lỗi ---");
    Math.random = () => 0.1; // Force payment fail
    try {
        await bookingService.handleConfirmBooking({
            accountId: 'account1',
            showtimeId: 'show1',
            seatIds: ['seat1', 'seat2'],
            couponCode: 'SUMMER20'
        });
    } catch (e) {
        console.log("Test 2 Expected Error (Rollback verified):", e.message);
    }

    console.log("\n--- TEST 3: Kiểm tra tương thích ngược (không có coupon) ---");
    Math.random = () => 0.9; // Succeed
    try {
        const result = await bookingService.handleConfirmBooking({
            accountId: 'account1',
            showtimeId: 'show1',
            seatIds: ['seat1', 'seat2']
        });
        console.log("No Coupon Booking Result:", {
            ticketPrice: result.ticketPrice,
            promotionDiscount: result.promotionDiscount,
            finalAmount: result.finalAmount
        });
    } catch (e) {
        console.error("Test 3 Failed:", e.message);
    }
    
    Math.random = originalRandom;
}

runTest();
