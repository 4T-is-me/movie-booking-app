import mongoose from "mongoose";
import dotenv from "dotenv";
import { Supplier } from "./src/models/supplierModel.js";

dotenv.config();

const run = async () => {
    try {
        const uri = process.env.MONGODB_URI || process.env.MONGO_URI;
        if (!uri) {
            console.error("Error: MONGODB_URI or MONGO_URI environment variable is not defined.");
            return;
        }

        await mongoose.connect(uri);
        console.log("Connected to MongoDB!");

        // Clear existing suppliers
        await Supplier.deleteMany({});
        console.log("Cleared existing suppliers.");

        const seedSuppliers = [
            {
                name: "Coca-Cola Beverages Vietnam Ltd.",
                phone: "1900 555 584",
                email: "order-support@cocacola.com.vn",
                address: "485 Xa lộ Hà Nội, Phường Linh Trung, Thủ Đức, TP. Hồ Chí Minh",
                isActive: true
            },
            {
                name: "Suntory PepsiCo Vietnam Beverage",
                phone: "028 3821 9437",
                email: "customer.service@suntorypepsico.vn",
                address: "Tầng 5, Tòa nhà Sheraton, 88 Đồng Khởi, Quận 1, TP. Hồ Chí Minh",
                isActive: true
            },
            {
                name: "Tan Hiep Phat Beverage Group (THP)",
                phone: "0274 3755 161",
                email: "info@thp.com.vn",
                address: "230 Đại lộ Bình Dương, Lái Thiêu, Thuận An, Bình Dương",
                isActive: true
            },
            {
                name: "Lotte Cinema Concessions Trading",
                phone: "028 3775 2524",
                email: "concessions@lotte.vn",
                address: "Tầng 3, Lotte Mart Nam Sài Gòn, Quận 7, TP. Hồ Chí Minh",
                isActive: true
            },
            {
                name: "Rich Products Vietnam (Popcorn & Butter)",
                phone: "028 3820 6030",
                email: "vietnam.info@rich.com",
                address: "Tòa nhà Sailing Tower, 111A Pasteur, Quận 1, TP. Hồ Chí Minh",
                isActive: true
            }
        ];

        await Supplier.insertMany(seedSuppliers);
        console.log("Successfully seeded 5 movie concessions & beverage suppliers!");

    } catch (e) {
        console.error(e);
    } finally {
        await mongoose.disconnect();
        console.log("Disconnected from MongoDB.");
    }
}

run();
