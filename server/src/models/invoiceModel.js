import mongoose from "mongoose";

const invoiceSchema = new mongoose.Schema({
    accountId: { type: mongoose.Schema.Types.ObjectId, ref: 'Account', required: true },
    movieId: { type: mongoose.Schema.Types.ObjectId, ref: 'Movie', required: true },
    addScore: { type: Number, default: 0 },
    bookingDate: { type: Date, default: Date.now },
    movieName: { type: String, required: true, trim: true },
    scheduleShow: { type: Date, required: true },
    scheduleShowTime: { type: String, required: true, trim: true },
    status: { type: Number, default: 1 },
    totalMoney: { type: Number, required: true },
    useScore: { type: Number, default: 0 },
    seat: { type: String, required: true, trim: true }
}, {
    versionKey: false,
    collection: 'invoices',
    timestamps: { createdAt: true, updatedAt: false }
});

invoiceSchema.index({ createdAt: -1 });
invoiceSchema.index({ movieId: 1, createdAt: -1 });

export const Invoice = mongoose.model('Invoice', invoiceSchema);
