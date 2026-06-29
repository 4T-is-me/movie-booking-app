import mongoose from "mongoose";
import dotenv from "dotenv";
dotenv.config();

const run = async () => {
    try {
        await mongoose.connect(process.env.MONGO_URI);
        const db = mongoose.connection.db;
        const movies = await db.collection("movies").find({}, {projection: {movieName: 1, status: 1}}).toArray();
        console.log("Movies:", JSON.stringify(movies, null, 2));
    } catch (e) {
        console.error(e);
    } finally {
        await mongoose.disconnect();
    }
}
run();
