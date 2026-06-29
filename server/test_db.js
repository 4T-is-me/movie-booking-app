import mongoose from "mongoose";
import dotenv from "dotenv";
dotenv.config();

const run = async () => {
    try {
        await mongoose.connect(process.env.MONGO_URI);
        console.log("Connected to MongoDB");
        
        const db = mongoose.connection.db;
        const roles = await db.collection("roles").find({}).toArray();
        console.log("Roles:", JSON.stringify(roles, null, 2));
        
        const accounts = await db.collection("accounts").find({username: "thanh"}).toArray();
        console.log("Thanh account roleId:", accounts[0]?.roleId);
        
    } catch (e) {
        console.error(e);
    } finally {
        await mongoose.disconnect();
    }
}
run();
