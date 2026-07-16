import mongoose from 'mongoose';
import { MaintenanceService } from './src/services/maintenanceService.js';
import { CinemaRoom } from './src/models/cinemaRoomModel.js';
import dotenv from 'dotenv';
dotenv.config();

async function test() {
  await mongoose.connect(process.env.MONGODB_URI);
  console.log("Connected to DB");
  
  // Find a room to test
  const room = await CinemaRoom.findOne({ status: 'active' });
  if (!room) {
    console.log("No active room found");
    process.exit(1);
  }
  
  console.log("Testing on room:", room._id);
  try {
    const res = await MaintenanceService.handleRoomMaintenance(room._id);
    console.log("Result:", res);
  } catch (err) {
    console.error("Error:", err);
  }
  
  process.exit(0);
}
test();
