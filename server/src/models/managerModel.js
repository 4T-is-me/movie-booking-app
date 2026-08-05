import mongoose from "mongoose";

const managerSchema = new mongoose.Schema({
    accountId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Account',
        required: true,
        unique: true
    },
    cinemaId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Cinema',
        required: true,
        unique: true
    },
    fullName: {
        type: String,
        required: true
    },
    phoneNumber: {
        type: String,
        required: true,
        unique: true
    },
    identityCard: {
        type: String,
        required: true,
        unique: true
    },
    kpiPoints: {
        type: Number,
        default: 0
    },
    revenueTarget: {
        type: Number,
        required: true
    },
    startDate: {
        type: Date,
        required: true
    },
    isDeleted: {
        type: Boolean,
        default: false
    }
}, {
    versionKey: false,
    timestamps: true,
    collection: 'managers'
});

export const Manager = mongoose.model('Manager', managerSchema);
