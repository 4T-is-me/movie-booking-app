import { ScheduleSeat } from "../models/scheduleSeatModel.js";
import { Booking } from "../models/bookingModel.js";
import { constants } from "../utils/constants.js";
import { Member } from "../models/memberModel.js";
import { Promotion } from "../models/promotionModel.js";
import { Showtime } from "../models/showTimeModel.js";
import { Movie } from "../models/movieModel.js";
import { Cinema } from "../models/cinemaModel.js";
import { ScoreHistory } from "../models/scoreHistoryModel.js";
import { ticketUtils } from "../utils/ticketUtils.js";
import { promotionService } from "./promotionService.js";
import { PromotionUsage } from "../models/promotionUsageModel.js";
import crypto from "crypto";
import mongoose from "mongoose";
import { StatusCodes } from "http-status-codes";
import ApiError from "../utils/ApiError.js";
import { formatDate, getShowtimeTimeRange } from "../utils/formatters.js";
import { format } from "path";
import { NotificationService }from "./notificationService.js";

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
          throw new ApiError(StatusCodes.CONFLICT, `Seat ${seatId} is already booked`);
        }
        if (
          seatStatusStr === constants.SEAT_STATUS.HOLD &&
          seat.holdExpiresAt > now &&
          seat.accountId && seat.accountId.toString() !== accountId.toString()
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
const handleConfirmBooking = async ({ accountId, showtimeId, seatIds, useScore = false, paymentMethod, couponCode, promotionId: requestPromotionId, combos = [], scoreToRedeem, memberId }) => {
  const session = await mongoose.startSession();
  session.startTransaction();

  try {
    // find member
    let member;
    if (memberId) {
      member = await Member.findById(memberId).session(session);
    } else {
      member = await Member.findOne({ accountId: accountId }).session(session);
    }

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
      throw new ApiError(StatusCodes.BAD_REQUEST, "Some seats are no longer held by you");
    }

    // ticket price
    let ticketPrice = 0;
    seats.forEach((item) => {
      // check price
      if (!item.seatId.price) {
        throw new ApiError(StatusCodes.BAD_REQUEST, `Price not found for seat ${item.seatId.seatName}`);
      }
      ticketPrice += item.seatId.price;
    });
    const comboPrice = combos.reduce((total, combo) => total + combo.price * combo.quantity, 0,); //price combo
    const subTotal = ticketPrice + comboPrice; // ticket + combo price

    //Promotion(if any)
    let promotionDiscount = 0;
    let promotionId = null;

    let activeCouponCode = couponCode;
    if (!activeCouponCode && requestPromotionId) {
      const promotionDoc = await Promotion.findById(requestPromotionId).session(session);
      if (promotionDoc) {
        activeCouponCode = promotionDoc.couponCode;
      }
    }

    // check promotion
    if (activeCouponCode) {
      const promotionResult = await promotionService.validateAndApplyPromotion(activeCouponCode, accountId, subTotal, session);
      promotionDiscount = promotionResult.discountAmount;
      promotionId = promotionResult.promotionId;
    }
    // Value after applying the promotion code; if none, promotionDiscount = 0
    const afterPromotion = subTotal - promotionDiscount;

    // Processing reward point redemptions
    let pointUsed = 0;
    let pointDiscount = 0;
    let earnedPoints = 0;
    let finalAmount = afterPromotion;

    if (useScore) {
      if (!member.rewardPoints || member.rewardPoints <= 0) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Not enough reward points");
      }
      const maxPointsNeeded = Math.ceil(afterPromotion / 10);
      const targetScore = (scoreToRedeem !== undefined && scoreToRedeem !== null) ? scoreToRedeem : maxPointsNeeded;
      pointUsed = Math.min(member.rewardPoints, targetScore);
      pointDiscount = pointUsed * 10;
      finalAmount = afterPromotion - pointDiscount;
    }

    // ADD_SCORE = 5% * TOTAL_MONEY (finalAmount) in terms of points (1 point = 10 VND)
    earnedPoints = Math.floor((finalAmount * 0.05) / 10);
    member.rewardPoints = member.rewardPoints - pointUsed + earnedPoints;

    const prevTotalSpent = member.totalSpent;
    const prevRewardPoints = member.rewardPoints + pointUsed - earnedPoints;

    member.totalSpent += finalAmount;

    // Update membership rank
    if (member.totalSpent >= 10000000) {
      member.membershipRank = "gold";
    } else if (member.totalSpent >= 5000000) {
      member.membershipRank = "silver";
    } else {
      member.membershipRank = "bronze";
    }

    const paymentStatus = "paid";

    console.log(`[DEBUG] User ID (memberId): ${member._id}`);
    console.log(`[DEBUG] Previous totalSpent: ${prevTotalSpent}`);
    console.log(`[DEBUG] New totalSpent: ${member.totalSpent}`);
    console.log(`[DEBUG] Previous rewardPoints: ${prevRewardPoints}`);
    console.log(`[DEBUG] New rewardPoints: ${member.rewardPoints}`);
    console.log(`[DEBUG] New rewardPoints: ${useScore}`)
    console.log(`[DEBUG] New membershipRank: ${member.membershipRank}`);
    console.log(`[DEBUG] finalAmount to charge: ${finalAmount}`);

    const memberUpdateResult = await Member.updateOne(
      { _id: member._id },
      {
        $set: {
          totalSpent: member.totalSpent,
          rewardPoints: member.rewardPoints,
          membershipRank: member.membershipRank
        }
      },
      { session }
    );

    console.log(`[DEBUG] Result of Member.updateOne(): matchedCount = ${memberUpdateResult.matchedCount}, modifiedCount = ${memberUpdateResult.modifiedCount}`);

    const ticketCode = await ticketUtils.generateTicketCode();
    const qrCodeData = await ticketUtils.generateQRCode(ticketCode);

    console.log(`[DEBUG] Invoking Booking.create() for memberId=${member._id}, showtimeId=${showtimeId}, paymentStatus=${paymentStatus}, finalAmount=${finalAmount}`);

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

    console.log(`[DEBUG] Booking.create() finished. bookingId=${booking[0]._id}, paymentStatus=${booking[0].paymentStatus}`);

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
    );

    // Create PromotionUsage and update usageCount if promotion applied
    if (promotionId) {
      await PromotionUsage.create(
        [
          {
            promotionId: promotionId,
            accountId: member.accountId,
            invoiceId: booking[0]._id,
            discountAmount: promotionDiscount
          }
        ],
        { session }
      );

      await Promotion.updateOne(
        { _id: promotionId },
        { $inc: { usageCount: 1 } },
        { session }
      );
    }

    const result = {
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

    await session.commitTransaction();
    console.log(`[DEBUG] Transaction committed successfully for booking ${booking[0]._id}. commitTransaction() executed.`);

    // Trigger FCM notification asynchronously so it does not delay checkout response
    NotificationService.sendBookingSuccessNotification(member.accountId, booking[0]).catch(err => {
        console.error('Failed to send booking success notification:', err.message)
    })

    return result;
  } catch (error) {
    console.error("[DEBUG] Transaction aborted due to error:", error.message);
    await session.abortTransaction();
    throw error;
  } finally {
    session.endSession();
  };
}

// get active bookings (my tickets)
const handleGetMyTickets = async (accountId, page = 1, limit = 10) => {
  const member = await Member.findOne({ accountId });
  if (!member) {
    throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
  }

  const now = new Date();
  // Query showtimes in the future
  const activeShowtimes = await Showtime.find({ startTime: { $gte: now } }).select('_id');
  const activeShowtimeIds = activeShowtimes.map(s => s._id);

  const bookingQuery = {
    memberId: member._id,
    paymentStatus: { $regex: /^paid$/i },
    showtimeId: { $in: activeShowtimeIds }
  };

  const skip = (page - 1) * limit;
  const totalItems = await Booking.countDocuments(bookingQuery);

  const bookings = await Booking.find(bookingQuery)
    .populate({
      path: "showtimeId",
      populate: [
        { path: "movieId" },
        { path: "cinemaId" },
        { path: "roomId" }
      ]
    })
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit)
    .lean();

  const mappedBookings = bookings.map(booking => {
    const showtime = booking.showtimeId;
    const movie = showtime?.movieId;
    const cinema = showtime?.cinemaId;
    const cinemaRoom = showtime?.roomId;

    return {
      bookingId: booking._id,
      bookingCode: booking.bookingCode,
      movie: movie?.movieName || "",
      poster: movie?.poster || "",
      showDate: showtime?.showDate ? formatDate(showtime.showDate) : "",
      showtime: getShowtimeTimeRange(showtime),
      showTime: getShowtimeTimeRange(showtime),
      cinemaName: cinema?.cinemaName || "",
      roomName: cinemaRoom?.roomName || "",
      seats: booking.seatNames || [],
      finalAmount: booking.finalAmount || 0,
      paymentStatus: booking.paymentStatus || "pending",
      createdAt: booking.createdAt ? formatDate(booking.createdAt, true) : "",
      qrCodeData: booking.qrCodeData || "",
      paymentMethod: booking.paymentMethod || ""
    };
  });

  return mappedBookings;
};

// get completed bookings (purchase history)
const handleGetHistory = async (accountId, page = 1, limit = 10) => {
  const member = await Member.findOne({ accountId });
  if (!member) {
    throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
  }

  const now = new Date();
  // Query showtimes in the past
  const pastShowtimes = await Showtime.find({ startTime: { $lt: now } }).select('_id');
  const pastShowtimeIds = pastShowtimes.map(s => s._id);

  const bookingQuery = {
    memberId: member._id,
    paymentStatus: { $regex: /^paid$/i },
    showtimeId: { $in: pastShowtimeIds }
  };

  const skip = (page - 1) * limit;
  const totalItems = await Booking.countDocuments(bookingQuery);

  const bookings = await Booking.find(bookingQuery)
    .populate({
      path: "showtimeId",
      populate: [
        { path: "movieId" },
        { path: "cinemaId" },
        { path: "roomId" }
      ]
    })
    .sort({ createdAt: -1 })
    .skip(skip)
    .limit(limit)
    .lean();

  const mappedBookings = bookings.map(booking => {
    const showtime = booking.showtimeId;
    const movie = showtime?.movieId;
    const cinema = showtime?.cinemaId;
    const cinemaRoom = showtime?.roomId;

    return {
      bookingId: booking._id,
      bookingCode: booking.bookingCode,
      movie: movie?.movieName || "",
      poster: movie?.poster || "",
      showDate: showtime?.showDate ? formatDate(showtime.showDate) : "",
      showtime: getShowtimeTimeRange(showtime),
      showTime: getShowtimeTimeRange(showtime),
      cinemaName: cinema?.cinemaName || "",
      roomName: cinemaRoom?.roomName || "",
      seats: booking.seatNames || [],
      finalAmount: booking.finalAmount || 0,
      paymentStatus: booking.paymentStatus || "pending",
      createdAt: booking.createdAt ? formatDate(booking.createdAt, true) : "",
      qrCodeData: booking.qrCodeData || "",
      paymentMethod: booking.paymentMethod || ""
    };
  });

  return mappedBookings;
};

// get score history
const handleGetScoreHistory = async (accountId) => {
  const member = await Member.findOne({ accountId: accountId }); // get member
  if (!member) {
    throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
  }

  return await ScoreHistory.find({ memberId: member._id })
    .sort({ createdAt: -1 })
    .populate("bookingId")
    .populate("memberId")
    .lean();
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
  const member = await Member.findOne({ accountId: accountId });
  //validate member
  if (!member) {
    throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
  }
  //Query booking
  const booking = await Booking.findOne({ _id: bookingId, memberId: member._id })
    .populate([{
      path: "showtimeId",
      populate: [
        { path: "movieId" }, // get movie from showtime
        { path: "cinemaId" }, // get cinema room from showtime,
        { path: "roomId", select: "roomName" } // get room name from cinema room
      ]
    },
    {
      path: "seatIds", // get lisst seat from booking
      select: "seatName seatType price"
    }])

  //validate booking
  if (!booking) {
    throw new ApiError(StatusCodes.NOT_FOUND, "Booking not found");
  }
  // get showtime, movie, cinema room to easy map
  const showtime = booking.showtimeId;
  const movie = showtime.movieId;
  const cinema = showtime.cinemaId;
  const cinemaRoom = showtime.roomId;

  // Find score history for earned points
  const scoreHistory = await ScoreHistory.findOne({ bookingId: booking._id });
  const earnedPoints = scoreHistory ? scoreHistory.earnedPoints : Math.floor(((booking.finalAmount || 0) * 0.05) / 10);

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
      showDate: showtime.showDate ? formatDate(showtime.showDate) : "",
      showtime: getShowtimeTimeRange(showtime),
      showTime: getShowtimeTimeRange(showtime)
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
    combos: (booking.combos || []).map(combo => ({
      name: combo.name || "",
      quantity: combo.quantity || 0,
      price: combo.price || 0,
      subtotal: (combo.price || 0) * (combo.quantity || 0)
    })),
    payment: {
      ticketPrice: booking.ticketPrice || 0,
      comboPrice: booking.comboPrice || 0,
      subTotalAmount: booking.subTotalAmount || 0,
      disCountAmount: booking.disCountAmount || 0,
      finalAmount: booking.finalAmount || 0,
      paymentMethod: booking.paymentMethod || ""
    },
    earnedPoints: earnedPoints,
    membershipRank: member.membershipRank || "bronze",
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
  handleGetbookingDetail,
  handleGetMyTickets
};
