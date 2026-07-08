import mongoose from "mongoose";
import dotenv from "dotenv";
dotenv.config();

const run = async () => {
    try {
        await mongoose.connect(process.env.MONGO_URI);
        const db = mongoose.connection.db;
        const cinemas = await db.collection("cinemas").find({}).toArray();
        console.log("Cinemas list:");
        cinemas.forEach(c => {
            console.log(`ID: ${c._id}, Name: ${c.cinemaName}`);
        });
    } catch (e) {
        console.error(e);
    } finally {
        await mongoose.disconnect();
    }
}
run();
