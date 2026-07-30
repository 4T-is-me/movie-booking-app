import mongoose from "mongoose";

const foodBeverageSchema = new mongoose.Schema({
    name: {
        type: String,
        required: true,
        trim: true,
        unique: true
    }, // Tên hiển thị: "Coca Cola L", "Bắp Phô Mai M", "Combo Solo"
    type: {
        type: String,
        enum: ['single', 'combo'],
        required: true
    }, // 'single' (món đơn) hoặc 'combo' (gói kết hợp)
    category: {
        type: String,
        required: true
    }, // Phân nhóm để lọc (ví dụ: 'drink', 'food', 'combo', 'snack'...)
    price: {
        type: Number,
        required: true,
        min: 0
    }, // Giá bán niêm yết (VNĐ)
    imageUrl: {
        type: String
    }, // Link ảnh sản phẩm lưu trên Cloudinary
    description: {
        type: String,
        trim: true
    }, // Mô tả ngắn thành phần
    items: [
        {
            itemName: { type: String, required: true },
            quantity: { type: Number, required: true }
        }
    ], // Nếu type = 'single' thì mảng rỗng. Nếu 'combo' thì liệt kê thành phần
    inventoryDeductions: [
        {
            itemCode: { type: String, required: true }, // Mã hàng kho gốc: "COCA_CAN", "POPCORN_BAG_L"
            quantity: { type: Number, required: true } // Số lượng hao hụt khi bán ra
        }
    ], // Công thức trừ kho khi bán sản phẩm này
    isActive: {
        type: Boolean,
        default: true
    } // Trạng thái kinh doanh (Bật/Tắt)
}, {
    versionKey: false,
    timestamps: true,
    collection: 'food_beverages'
});

export const FoodBeverage = mongoose.model('FoodBeverage', foodBeverageSchema);
