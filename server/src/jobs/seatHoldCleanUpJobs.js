import cron from "node-cron";
import { constants } from "../utils/constants.js";
import { ScheduleSeat } from "../models/scheduleSeatModel.js";
 const startSeatCleanupJob  = async () => {
  const now = new Date();

  await ScheduleSeat.updateMany(
    {
      holdExpiresAt: { $lt: now },
      status: constants.SEAT_STATUS.HOLD,
    },
    {
      $set: {
        status: constants.SEAT_STATUS.AVAILABLE,
        accountId: null,
        holdExpiresAt: null,
      },
    },
  );
};
cron.schedule("*/30 * * * * *", startSeatCleanupJob);

export const seatHoldCleanUpJobs = {
  startSeatCleanupJob,
};