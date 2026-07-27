import mongoose from "mongoose";

const inventorySchema = new mongoose.Schema({
    cinemaId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Cinema',
        required: true
    }, // Khóa ngoại liên kết tới cụm rạp cụ thể
    itemCode: {
        type: String,
        required: true
    }, // Mã hàng gốc: "COCA_CAN", "POPCORN_BAG_L", "WATER_BOTTLE"
    itemName: {
        type: String,
        required: true
    }, // Tên gọi vật lý: "Lon Coca 330ml", "Túi bắp cỡ L"
    quantity: {
        type: Number,
        required: true,
        min: 0,
        default: 0
    }, // Số lượng tồn kho hiện tại
    lowStockAlert: {
        type: Number,
        default: 15
    } // Ngưỡng tối thiểu để cảnh báo quản lý nhập hàng
}, {
    versionKey: false,
    timestamps: true,
    collection: 'inventories'
});

// Compound index đảm bảo mỗi rạp chỉ có 1 bản ghi tồn kho duy nhất cho từng mặt hàng
inventorySchema.index({ cinemaId: 1, itemCode: 1 }, { unique: true });

export const Inventory = mongoose.model('Inventory', inventorySchema);
