import cron from "node-cron";
import { constants } from "../utils/constants.js";
import { ScheduleSeat } from "../models/scheduleSeatModel.js";
 const startSeatCleanupJob  = async () => {
  const now = new Date();

  await ScheduleSeat.deleteMany(
    {
      holdExpiresAt: { $lt: now },
      status: "holding",
    }
  );
};
cron.schedule("*/30 * * * * *", startSeatCleanupJob);

export const seatHoldCleanUpJobs = {
  startSeatCleanupJob,
};