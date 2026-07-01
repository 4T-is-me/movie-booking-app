import mongoose from "mongoose";

const scoreHistorySchema = new mongoose.Schema({
    memberId: { type: mongoose.Schema.Types.ObjectId, ref: "Member", required: true },
    bookingId: { type: mongoose.Schema.Types.ObjectId, ref: "Booking" },
    usedPoints: { type: Number, default: 0 },
    earnedPoints: { type: Number, default: 0 },
    remainingPoints: { type: Number, required: true },
    reason: { type: String, required: true }
}, {
    //only createdAt, no updatedAt  
    timestamps: { createdAt: true, updatedAt: false },
    versionKey: false,
    collection: "score_histories"
});

export const ScoreHistory = mongoose.model('ScoreHistory', scoreHistorySchema);