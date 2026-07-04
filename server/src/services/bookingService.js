import { ScheduleSeat } from "../models/scheduleSeatModel.js";
import { Booking } from "../models/bookingModel.js";
import { constants } from "../utils/constants.js";
import { Member } from "../models/memberModel.js";
import { Promotion } from "../models/promotionModel.js";
import { ScoreHistory } from "../models/scoreHistoryModel.js";
import { ticketUtils } from "../utils/ticketUtils.js";
import crypto from "crypto";
import mongoose from "mongoose";
import { StatusCodes } from "http-status-codes";
import ApiError from "../utils/ApiError.js";
import { formatDate } from "../utils/formatters.js";
import { format } from "path";

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
        const seatStatusStr = String(seat.status).toLowerCase();
        if (seatStatusStr === constants.SEAT_STATUS.SOLD) {
           throw new ApiError(StatusCodes.CONFLICT,`Seat ${seatId} is already booked`);
        }
        if (
          seatStatusStr === constants.SEAT_STATUS.HOLD &&
          seat.holdExpiresAt > now &&
          seat.accountId.toString() !== accountId.toString()
        ) {
          throw new ApiError(StatusCodes.CONFLICT, `Seat ${seatId} is currently held by someone else`);
        }

        // update existing seat
        seat.status = constants.SEAT_STATUS.HOLD;
        seat.accountId = accountId;
        seat.holdExpiresAt = expiredTime;
        await seat.save({ session });
      } else {
        // create new seat
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
        throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
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
        throw new ApiError(StatusCodes.BAD_REQUEST,"Some seats are no longer held by you");
      }

      // ticket price
      let ticketPrice = 0;
      seats.forEach((item) => {
        // check price
        if (!item.seatId.price) {
          throw new ApiError(StatusCodes.BAD_REQUEST,`Price not found for seat ${item.seatId.seatName}`);
        }
        ticketPrice += item.seatId.price;
      });
      const comboPrice = combos.reduce((total, combo) => total + combo.price * combo.quantity,0,); //price combo
      const subTotal = ticketPrice + comboPrice; // ticket + combo price

      //Promotion(if any)
      let promotionDiscount = 0;
      let promotion = null;
      // check promotion
      if (promotionId) {
        promotion = await Promotion.findById(promotionId).session(session);
        // validate promotion
        if (!promotion) {
          throw new ApiError( StatusCodes.NOT_FOUND,"Promotion not found");
        }
        promotionDiscount = Math.floor(
          subTotal * (promotion.discountLevel / 100),
        );
      }
      // Value after applying the promotion code; if none, promotionDiscount = 0
      const afterPromotion = subTotal - promotionDiscount; 

      // Processing reward point redemptions
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

      // Update membership rank
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
          throw new ApiError(StatusCodes.BAD_REQUEST, "Payment failed");
      }

      await member.save({ session });

      const ticketCode = await ticketUtils.generateTicketCode();
      const qrCodeData = await ticketUtils.generateQRCode(ticketCode);
      
      // create booking
      const booking = await Booking.create(
        [
          {
            memberId: member._id,
            showtimeId: showtimeId,
            bookingCode: ticketCode,
            promotionId: promotionId,
            seatIds: seatIds,
            seatNames: seats.map((seat) => seat.seatId.seatName),
            combos: combos,
            ticketPrice: ticketPrice,
            comboPrice: comboPrice,
            subTotalAmount: subTotal,
            paymentStatus: paymentStatus,
            paymentMethod: paymentMethod,
            qrCodeData: qrCodeData,
            disCountAmount: promotionDiscount + pointDiscount,
            finalAmount: finalAmount,
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
        throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
    }

    const skip = (page - 1) * limit;
    const totalItems = await Booking.countDocuments({
        memberId: member._id
    });

    const bookings = await Booking.find({
        memberId: member._id
    }).populate({
            path: "showtimeId",
            populate:[
              {path: "movieId"},
              {path: "cinemaId"},
              {path: "roomId"}
            ]
      })
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(limit)
      .lean();

    const mappedBoookings = bookings.map(booking => {
        const showtime = booking.showtimeId
        const movie = showtime?.movieId;
        console.log("showtime", showtime);
        const cinema = showtime?.cinemaId;
        console.log("cinema", cinema);
        const cinemaRoom = showtime?.roomId;
        console.log("cinemaRoom", cinemaRoom);  

        return {
          bookingId: booking._id,
          bookingCode: booking.bookingCode,
          movie: movie?.movieName || "",
          poster: movie?.poster || "",
          showDate: formatDate(showtime?.showDate) || "",
          showtime: showtime?.showTime || "",
          cinemaName: cinema?.cinemaName || "",
          roomName: cinemaRoom?.roomName || "",
          seats: booking.seatNames || [],
          finalAmount: booking.finalAmount || 0,
          paymentStatus: booking.paymentStatus || "pending",
          createdAt: formatDate(booking.createdAt, true) || new Date()
        }
    })


    return {
        data: mappedBoookings,
        totalItems,
        totalPages: Math.ceil(totalItems / limit)
    };
};

// get score history
const handleGetScoreHistory = async (accountId) => {
  const member = await Member.findOne({ accountId: accountId }); // get member
  if (!member) {
    throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
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

// get booking detail - can also be used to display My tickets
const handleGetbookingDetail = async (bookingId, accountId) => {
    //find member
    const member = await Member.findOne({accountId: accountId});
    //validate member
    if(!member) {
      throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
    }
    //Query booking
    const booking = await Booking.findOne({ _id: bookingId, memberId: member._id })
    .populate([{
      path: "showtimeId",
      populate: [
            {path: "movieId"}, // get movie from showtime
            {path: "cinemaId"}, // get cinema room from showtime,
            {path: "roomId", select: "roomName"} // get room name from cinema room
        ]
    },
    { 
      path: "seatIds", // get lisst seat from booking
      select: "seatName seatType price"
    }])

    //validate booking
    if(!booking) {
      throw new ApiError(StatusCodes.NOT_FOUND, "Booking not found");
    }
    // get showtime, movie, cinema room to easy map
    const showtime = booking.showtimeId;
    const movie = showtime.movieId;
    const cinema = showtime.cinemaId;
    const cinemaRoom = showtime.roomId;

    //map data
    const responseBookingDetail = {
      bookingId: booking._id,
      bookingCode: booking.bookingCode,
      qrCodeData: booking.qrCodeData,
      movie: {
        movieName: movie.movieName || "",
        duration: movie.duration || 0,
        poster: movie.poster || "",
        movieGenre: movie.movieGenre || []
      },
      showtime: {
        showDate: showtime.showDate || "",
        showtime: showtime.showTime || ""
      },
      cinema: {
        cinemaName: cinema.cinemaName || "",
      },
      cinemaRoom: {
        roomName: cinemaRoom.roomName || "",
      },
      seats: booking.seatIds.map(seat => ({
        seatName: seat.seatName || "",
        seatType: seat.seatType || "",
        price: seat.price || 0
      })),
      payment: {
        ticketPrice: booking.ticketPrice || 0,
        comboPrice: booking.comboPrice || 0,
        subTotalAmount: booking.subTotalAmount || 0,
        disCountAmount: booking.disCountAmount || 0,
        finalAmount: booking.finalAmount || 0,
        paymentMethod: booking.paymentMethod || ""
      },
      status: booking.paymentStatus || ""
    }
    return responseBookingDetail;
}

export const bookingService = {
  handleHoldSeats,
  handleConfirmBooking,
  handleGetHistory,
  handleGetScoreHistory,
  handleReleaseBooking,
  handleGetbookingDetail
};
