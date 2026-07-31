import mongoose from "mongoose";

const importOrderItemSchema = new mongoose.Schema({
    itemCode: {
        type: String,
        required: true
    }, // Mã kho: "COCA_CAN", "POPCORN_L"
    itemName: {
        type: String,
        required: true
    }, // Tên hiển thị: "Lon Coca 330ml", "Bắp Cỡ L"
    quantity: {
        type: Number,
        required: true,
        min: 1
    }, // Số lượng nhập thực tế
    purchasePrice: {
        type: Number,
        required: true,
        min: 0
    } // Đơn giá nhập (VNĐ/đơn vị)
}, { _id: false });

const importOrderSchema = new mongoose.Schema({
    importCode: {
        type: String,
        unique: true,
        required: true
    }, // Mã phiếu: "PN-20260731-0001"
    cinemaId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Cinema',
        required: true
    },
    managerId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Manager',
        required: true
    }, // Manager lập phiếu
    supplierId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Supplier',
        required: true
    },
    items: {
        type: [importOrderItemSchema],
        required: true,
        validate: {
            validator: function (v) {
                return v && v.length > 0;
            },
            message: 'Import order must have at least one item'
        }
    },
    totalAmount: {
        type: Number,
        required: true,
        min: 0
    }, // Tổng tiền = sum(quantity * purchasePrice)
    status: {
        type: String,
        enum: ['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'],
        default: 'PENDING'
    },
    // PENDING: Chờ Admin duyệt
    // APPROVED: Admin đã duyệt + kho đã được cộng
    // REJECTED: Admin từ chối
    // CANCELLED: Manager tự hủy (chỉ khi đang PENDING)
    approvedBy: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Account'
    }, // AccountId của Admin đã duyệt/từ chối
    rejectionReason: {
        type: String,
        trim: true
    }, // Lý do từ chối (nếu có)
    notes: {
        type: String,
        trim: true
    } // Ghi chú của Manager khi tạo phiếu
}, {
    versionKey: false,
    timestamps: true,
    collection: 'import_orders'
});

importOrderSchema.index({ cinemaId: 1, createdAt: -1 });
importOrderSchema.index({ status: 1 });
importOrderSchema.index({ importCode: 1 }, { unique: true });

export const ImportOrder = mongoose.model('ImportOrder', importOrderSchema);
