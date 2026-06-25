

import { ScheduleSeat } from "../models/scheduleSeatModel.js";
import { Booking } from "../models/bookingModel.js";
import { constants } from "../utils/constants.js";
import { Account } from "../models/accountModel.js";
import { Member } from "../models/memberModel.js";
import { Seat } from "../models/seatModel.js"
import { Promotion} from "../models/promotionModel.js"
import { ScoreHistory } from "../models/scoreHistoryModel.js"
import crypto from "crypto";
import mongoose from "mongoose";
import cron from "node-cron";

//handle hold-seats 
const handleHoldSeats = async ({ accountId, showtimeId, seatIds }) => {
  const session = await mongoose.startSession();
  session.startTransaction();
  try {
    const expiredTime = new Date(Date.now() + 10 * 60 * 1000);// expire in 10 minutes
    const now = new Date();
    const updatedSeats = [];
    for (const seatId of seatIds) {

      // handle the case where two people hold the same seat
      const seat = await ScheduleSeat.findOneAndUpdate(
        {
          showtimeId: showtimeId,
          seatId: seatId,
          $or: [
            { status: constants.SEAT_STATUS.AVAILABLE }, // status = AVAILABLE
            {
              status: constants.SEAT_STATUS.HOLD, // status = HOLD(not yet expired)
              holdExpiresAt: { $lte: now },
            },
          ],
        },
        {
          $set: {
            status: constants.SEAT_STATUS.HOLD,
            accountId: accountId,
            holdExpiresAt: expiredTime,
          },
        },
        {
          new: true, session
        },
      ).lean();
      console.log(seat);
      if (!seat) { 
        throw new Error(`Seat ${seatId} is unavailable`);
      }
      updatedSeats.push(seat);
    }

    await session.commitTransaction(); 
    return {expiredAt: expiredTime, seats: updatedSeats};

  } catch (error) {
    await session.abortTransaction();
    throw error;
  } finally {
    session.endSession();
  }
};
// handle generate booking code
const generateBookingCode =() => {
  const randomStr = crypto.randomBytes(4).toString("hex").toUpperCase();
  return `BK-${randomStr.match(/.{1,4}/g).join("-")}`;
}


// handle confirm booking
const handleConfirmBooking = async ({ accountId, showtimeId, seatIds, pointUsed = 0, paymentMethod, promotionId, combos = []}) => {
  const session = await mongoose.startSession();
  session.startTransaction();

  try {
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
    const comboPrice = combos.reduce(
      (total, combo) => total + combo.price * combo.quantity,
      0,
    ); //price combo
    const subTotal = ticketPrice + comboPrice; // vé + combo

    //Promotion (khuyến mãi => nếu có)
    let promotionDiscount = 0;
    let promotion = null;
    // check promotion(nếu có id khuyến mãi)
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
    if (pointUsed > member.rewardPoints) {
      throw new Error("Insufficient points");
    }
    const pointDiscount = pointUsed * 10; // 1 point = 10 VND
    const finalAmount = Math.max(0, afterPromotion - pointDiscount);
    const earnedPoints = Math.floor((finalAmount * 0.01) / 10); // Điểm tích lũy mới là 1% giá trị hóa đơn(giống bhx)
    member.rewardPoints = member.rewardPoints - pointUsed + earnedPoints; // update member points

    // cập nhật hạng thành viên
    if (member.totalSpent >= 20000000) {
      member.membershipRank = "gold";
    } else if (member.totalSpent >= 5000000) {
      member.membershipRank = "silver";
    } else {
      member.membershipRank = "bronze";
    }

    await member.save({ session });
    // create booking
    const booking = await Booking.create( 
      [ {
        memberId: member._id, 
        showtimeId: showtimeId, 
        bookingCode: generateBookingCode(), 
        promotionId: promotionId, 
        seatIds: seatIds, 
        seatNames: seats.map( seat => seat.seatId.seatName ), 
        combos: combos, 
        ticketPrice: ticketPrice, 
        comboPrice: comboPrice, 
        totalAmount: finalAmount, 
        paymentStatus: "paid", 
        paymentMethod: paymentMethod 
        } 
      ], { session } 
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
          earnedPoints: earnedPoints,
          remainingPoints: member.rewardPoints,
        },
      ],
      { session },
    );

    await session.commitTransaction();
    return { 
      bookingId: booking[0]._id, 
      bookingCode: booking[0].bookingCode, 
      ticketPrice, comboPrice, 
      promotionDiscount, 
      pointDiscount, 
      finalAmount, 
      earnedPoints, 
      remainingPoints: member.rewardPoints, 
      membershipRank: member.membershipRank 
    };
  } catch (error) {
    await session.abortTransaction(); // rollback
    throw error;
  } finally {
    session.endSession(); // close
  }
};

// get history
const handleGetHistory = async (accountId) => {
  const member = await Member.findOne({ accountId });
  
  if (!member) {
    throw new Error("Member not found");
  }

  return await Booking.find({memberId: member._id})
    .populate("showtimeId")
    .sort({ createdAt: -1 })
    .lean();
};
        
// get score history
const handleGetScoreHistory = async (accountId) => {
  const member = await Member.findOne({ accountId: accountId });// get member
  if(!member) {
    throw new Error("Member not found");
  }
  return await ScoreHistory.find({memberId: member._id}).sort({createdAt: -1}).populate("bookingId");
};

export const bookingService = {
  handleHoldSeats,
  handleConfirmBooking,
  handleGetHistory,
  handleGetScoreHistory,
};