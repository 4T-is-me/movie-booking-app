import mongoose from 'mongoose';
import { env } from './environment.js';

const connectDB = async () => {
    try{
        await mongoose.connect(env.MONGO_URI);
        console.log('MongoDB connected successfully!');
    }catch(error){
        console.error('MongoDB connection error:', error);
        process.exit(1); // Exit process with failure
    }
}
export default connectDB;
