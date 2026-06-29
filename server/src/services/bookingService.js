import { ScheduleSeat } from "../models/scheduleSeatModel.js";
import { Booking } from "../models/bookingModel.js";
import { constants } from "../utils/constants.js";
import { Member } from "../models/memberModel.js";
import { Promotion } from "../models/promotionModel.js";
import { ScoreHistory } from "../models/scoreHistoryModel.js";
import crypto from "crypto";
import mongoose from "mongoose";

//handle hold-seats
const handleHoldSeats = async ({ accountId, showtimeId, seatIds }) => {
  const session = await mongoose.startSession();
  session.startTransaction();
  try {
    const expiredTime = new Date(Date.now() + 10 * 60 * 1000); // expire in 10 minutes
    const now = new Date();
    const updatedSeats = [];

    for (const seatId of seatIds) {
      let seat = await ScheduleSeat.findOne({ showtimeId, seatId }).session(session);

      if (seat) {
        if (seat.status.toLocaleLowerCase() === constants.SEAT_STATUS.SOLD) {
          throw new Error(`Seat ${seatId} is already booked`);
        }
        if (
          seat.status.toLocaleLowerCase() === constants.SEAT_STATUS.HOLD &&
          seat.holdExpiresAt > now &&
          seat.accountId.toString() !== accountId.toString()
        ) {
          throw new Error(`Seat ${seatId} is currently held by someone else`);
        }

        // Cập nhật seat
        seat.status = constants.SEAT_STATUS.HOLD;
        seat.accountId = accountId;
        seat.holdExpiresAt = expiredTime;
        await seat.save({ session });
      } else {
        // Tạo mới seat
        seat = new ScheduleSeat({
          showtimeId,
          seatId,
          accountId,
          status: constants.SEAT_STATUS.HOLD,
          holdExpiresAt: expiredTime,
        });
        await seat.save({ session });
      }
      updatedSeats.push(seat.toObject ? seat.toObject() : seat);
    }

    await session.commitTransaction();
    return { expiredAt: expiredTime, seats: updatedSeats };
  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    session.endSession();
  }
};
// handle generate booking code
const generateBookingCode = () => {
  const randomStr = crypto.randomBytes(6).toString("hex").toUpperCase();
  return `BK-${randomStr.match(/.{1,4}/g).join("-")}`;
};

// handle confirm booking
const handleConfirmBooking = async ({accountId, showtimeId,seatIds,useScore = false,paymentMethod,promotionId,combos = []}) => {
  
  const session = await mongoose.startSession();

  try {
    let result;

    await session.withTransaction(async () => {
      // find member
      const member = await Member.findOne({ accountId: accountId }).session(
        session,
      );

      // validate member
      if (!member) {
        throw new Error("Member not found");
      }

      // find seat
      const seats = await ScheduleSeat.find({
        showtimeId: showtimeId,
        seatId: { $in: seatIds },
        status: constants.SEAT_STATUS.HOLD,
        accountId: accountId,
        holdExpiresAt: { $gte: new Date() },
      })
        .populate("seatId")
        .session(session);

      //validate seats
      if (seats.length !== seatIds.length) {
        throw new Error("Some seats are no longer held by you");
      }

      // ticket price
      let ticketPrice = 0;
      seats.forEach((item) => {
        // check price
        if (!item.seatId.price) {
          throw new Error(`Price not found for seat ${item.seatId.seatName}`);
        }
        ticketPrice += item.seatId.price;
      });
      const comboPrice = combos.reduce((total, combo) => total + combo.price * combo.quantity,0,); //price combo
      const subTotal = ticketPrice + comboPrice; // vé + combo

      //Promotion(if any)
      let promotionDiscount = 0;
      let promotion = null;
      // check promotion
      if (promotionId) {
        promotion = await Promotion.findById(promotionId).session(session);
        // validate promotion
        if (!promotion) {
          throw new Error("Promotion not found");
        }
        promotionDiscount = Math.floor(
          subTotal * (promotion.discountLevel / 100),
        );
      }
      const afterPromotion = subTotal - promotionDiscount; // giá trị sau khi áp mã khuyến mãi, ko có thì promotionDiscount = 0

      // xử lí đổi điểm thưởng
      let pointUsed = 0;
      let pointDiscount = 0;
      let earnedPoints = 0;
      let finalAmount = afterPromotion;

      if (useScore) {
        pointUsed = Math.min(member.rewardPoints,Math.ceil(afterPromotion / 10));
        pointDiscount = pointUsed * 10;
        finalAmount = afterPromotion - pointDiscount;
        member.rewardPoints -= pointUsed;
      } else {
          earnedPoints = Math.floor((afterPromotion * 0.01) / 10);
          member.rewardPoints += earnedPoints;
      }

      member.totalSpent += finalAmount;
      // cập nhật hạng thành viên
      if (member.totalSpent >= 20000000) {
        member.membershipRank = "gold";
      } else if (member.totalSpent >= 5000000) {
        member.membershipRank = "silver";
      } else {
        member.membershipRank = "bronze";
      }
      
      // 80% success annd 20% fail
      const paymentStatus = Math.random() < 0.8 ? "paid" : "failed";
      if (paymentStatus === "failed") {
          throw new Error("Payment failed");
      }

      await member.save({ session });
      // create booking
      const booking = await Booking.create(
        [
          {
            memberId: member._id,
            showtimeId: showtimeId,
            bookingCode: generateBookingCode(),
            promotionId: promotionId,
            seatIds: seatIds,
            seatNames: seats.map((seat) => seat.seatId.seatName),
            combos: combos,
            ticketPrice: ticketPrice,
            comboPrice: comboPrice,
            totalAmount: finalAmount,
            paymentStatus: paymentStatus,
            paymentMethod: paymentMethod,
          },
        ],
        { session },
      );
      
      // update seat
      await ScheduleSeat.updateMany(
        {
          showtimeId: showtimeId,
          seatId: { $in: seatIds },
          status: constants.SEAT_STATUS.HOLD,
          accountId: accountId,
          holdExpiresAt: { $gte: new Date() },
        },
        {
          $set: {
            status: constants.SEAT_STATUS.SOLD,
            holdExpiresAt: null,
          },
        },
        { session },
      );

      // create score history
      await ScoreHistory.create(
        [
          {
              memberId: member._id,
              bookingId: booking[0]._id,
              usedPoints: pointUsed,
              earnedPoints,
              remainingPoints: member.rewardPoints,
              reason: useScore ? "USE_SCORE" : "BUY_TICKET"
          }
        ],
        { session }
      )

      result = {
        bookingId: booking[0]._id,
        bookingCode: booking[0].bookingCode,
        ticketPrice,
        comboPrice,
        promotionDiscount,
        pointDiscount,
        finalAmount,
        earnedPoints,
        remainingPoints: member.rewardPoints,
        membershipRank: member.membershipRank,
      };
    });

    return result;
  } finally {
    await session.endSession();
  };
}

// get history
const handleGetHistory = async (accountId, page = 1, limit = 10) => {

    const member = await Member.findOne({ accountId });

    if (!member) {
        throw new Error("Member not found");
    }

    const skip = (page - 1) * limit;
    const totalItems = await Booking.countDocuments({
        memberId: member._id
    });

    const bookings = await Booking.find({
        memberId: member._id
    }).populate({
            path: "showtimeId",
            populate: {
                path: "movieId"
            }
      })
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit)
      .lean();

    return {
        data: bookings,
        totalItems,
        totalPages: Math.ceil(totalItems / limit)
    };
};

// get score history
const handleGetScoreHistory = async (accountId) => {
  const member = await Member.findOne({ accountId: accountId }); // get member
  if (!member) {
    throw new Error("Member not found");
  }
  return await ScoreHistory.find({ memberId: member._id })
    .sort({ createdAt: -1 })
    .populate("bookingId");
};

// release seats currently held by the authenticated account for a showtime
const handleReleaseBooking = async ({ accountId, showtimeId }) => {
  const result = await ScheduleSeat.deleteMany({
    showtimeId,
    accountId,
    status: constants.SEAT_STATUS.HOLD,
  });

  return {
    showtimeId,
    releasedCount: result.deletedCount,
  };
};

export const bookingService = {
  handleHoldSeats,
  handleConfirmBooking,
  handleGetHistory,
  handleGetScoreHistory,
  handleReleaseBooking,
};
