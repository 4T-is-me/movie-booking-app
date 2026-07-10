import cron from "node-cron";
import { constants } from "../utils/constants.js";
import { Movie } from "../models/movieModel.js";
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




const updateMovieStatus = async () => {
    const now = new Date();

    // Coming Soon -> Showing
    await Movie.updateMany(
        {
            releaseDate: { $lte: now },
            endDate: { $gte: now },
        status: "coming_soon"
        },
        {
            $set: { status: "showing" }
        }
    );

    // Showing -> Ended
    await Movie.updateMany(
        {
            endDate: { $lt: now },
            status: "showing"
        },
        {
            $set: { status: "ended" }
        }
    );
};
//update every day(00:00)
cron.schedule("0 0 * * *", updateMovieStatus);
export const CleanUpJobs = {
  startSeatCleanupJob,
  updateMovieStatus
};