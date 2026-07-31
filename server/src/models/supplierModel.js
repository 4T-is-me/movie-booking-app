import mongoose from "mongoose";

const supplierSchema = new mongoose.Schema({
    name: {
        type: String,
        required: true,
        trim: true,
        unique: true
    }, // Tên nhà cung cấp: "Coca-Cola Vietnam", "Tân Hiệp Phát"...
    phone: {
        type: String,
        required: true
    },
    email: {
        type: String,
        trim: true
    },
    address: {
        type: String,
        trim: true
    },
    isActive: {
        type: Boolean,
        default: true
    }
}, {
    versionKey: false,
    timestamps: true,
    collection: 'suppliers'
});

export const Supplier = mongoose.model('Supplier', supplierSchema);
