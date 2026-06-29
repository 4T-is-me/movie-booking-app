import mongoose from "mongoose";

const bookingSchema = new mongoose.Schema({
    memberId: { type: mongoose.Schema.Types.ObjectId, ref: 'Member', required: true },
    showtimeId: { type: mongoose.Schema.Types.ObjectId, ref: 'Showtime', required: true },
    promotionId: { type: mongoose.Schema.Types.ObjectId, ref: 'Promotion' },
    bookingCode: { type: String, required: true, unique: true },
    seatIds: [{ type: mongoose.Schema.Types.ObjectId, ref: 'Seat' }],
    seatNames: [{ type: String }],
    combos: [{
        name: String,
        quantity: Number,
        price: Number
    }],
    ticketPrice: { type: Number, required: true },
    comboPrice: { type: Number, default: 0 },
    totalAmount: { type: Number, required: true },
    paymentStatus: { type: String, default: 'pending' }, // 'pending', 'successful', 'cancel'
    paymentMethod: { type: String, required: true },
    createdAt: { type: Date, default: Date.now }
}, {
    // Cấu hình timestamps chỉ tạo createdAt, tắt updatedAt nếu DB không dùng tới
    timestamps: { createdAt: true, updatedAt: false },
    versionKey: false,
    collection: 'bookings'
});

export const Booking = mongoose.model('Booking', bookingSchema);