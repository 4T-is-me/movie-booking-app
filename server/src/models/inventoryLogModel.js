import mongoose from "mongoose";

const inventoryLogSchema = new mongoose.Schema({
    cinemaId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Cinema',
        required: true
    },
    itemCode: {
        type: String,
        required: true
    },
    type: {
        type: String,
        enum: ['SALE', 'REFILL', 'ADJUST', 'REFUND'],
        required: true
    }, // SALE: bán hàng, REFILL: nhập kho, ADJUST: điều chỉnh, REFUND: hoàn trả
    changeQuantity: {
        type: Number,
        required: true
    }, // Số lượng thay đổi (âm nếu trừ, dương nếu cộng)
    oldQuantity: {
        type: Number,
        required: true
    },
    newQuantity: {
        type: Number,
        required: true
    },
    referenceId: {
        type: mongoose.Schema.Types.ObjectId,
        required: false
    }, // Có thể là bookingId hoặc staffId thực hiện
    reason: {
        type: String,
        trim: true
    }
}, {
    versionKey: false,
    timestamps: { createdAt: true, updatedAt: false },
    collection: 'inventory_logs'
});

inventoryLogSchema.index({ cinemaId: 1, createdAt: -1 });
inventoryLogSchema.index({ itemCode: 1 });

export const InventoryLog = mongoose.model('InventoryLog', inventoryLogSchema);
