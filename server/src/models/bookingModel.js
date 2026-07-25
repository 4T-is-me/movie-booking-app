import mongoose from "mongoose";

const bookingSchema = new mongoose.Schema({
    memberId: { type: mongoose.Schema.Types.ObjectId, ref: 'Member', required: true },
    showtimeId: { type: mongoose.Schema.Types.ObjectId, ref: 'Showtime', required: true },
    promotionId: { type: mongoose.Schema.Types.ObjectId, ref: 'Promotion' },
    bookingCode: { type: String, required: true, unique: true },
    seatIds: [{ type: mongoose.Schema.Types.ObjectId, ref: 'Seat' }],
    seatNames: [{ type: String }],
    combos: [{ name: String, quantity: Number, price: Number }],
    ticketPrice: { type: Number, required: true },
    comboPrice: { type: Number, default: 0 },
    subTotalAmount: { type: Number, required: true }, // before discount
    paymentStatus: { type: String, default: 'pending' }, // 'pending', 'successful', 'cancel'
    paymentMethod: { type: String, required: true },
    qrCodeData: { type: String },
    checkedIn: { type: Boolean, default: false },
    checkedInAt: { type: Date },
    checkedInBy: { type: mongoose.Schema.Types.ObjectId, ref: 'Staff' },
    isStaffRated: { type: Boolean, default: false },
    staffRating: { type: Number },
    disCountAmount: { type: Number, default: 0 },
    finalAmount: { type: Number, required: true }, // after discount
    reminded30M: { type: Boolean, default: false },
    createdAt: { type: Date, default: Date.now }
}, {
    // Timestamps configuration: only create createdAt, disable updatedAt if not used in DB
    timestamps: { createdAt: true, updatedAt: false },
    versionKey: false,
    collection: 'bookings'
});

export const Booking = mongoose.model('Booking', bookingSchema);