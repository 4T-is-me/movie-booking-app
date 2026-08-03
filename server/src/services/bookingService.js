import { ScheduleSeat } from "../models/scheduleSeatModel.js";
import { Booking } from "../models/bookingModel.js";
import { constants } from "../utils/constants.js";
import { Member } from "../models/memberModel.js";
import { Promotion } from "../models/promotionModel.js";
import { Showtime } from "../models/showTimeModel.js";
import { Movie } from "../models/movieModel.js";
import { Cinema } from "../models/cinemaModel.js";
import { Staff } from "../models/staffModel.js";
import { ScoreHistory } from "../models/scoreHistoryModel.js";
import { ticketUtils } from "../utils/ticketUtils.js";
import { promotionService } from "./promotionService.js";
import { PromotionUsage } from "../models/promotionUsageModel.js";
import { Inventory } from "../models/inventoryModel.js";
import { FoodBeverage } from "../models/foodBeverageModel.js";
import { InventoryLog } from "../models/inventoryLogModel.js";
import crypto from "crypto";
import mongoose from "mongoose";
import { StatusCodes } from "http-status-codes";
import ApiError from "../utils/ApiError.js";
import { formatDate, getShowtimeTimeRange } from "../utils/formatters.js";
import { NotificationService } from "./notificationService.js";
import { Review } from "../models/reviewModel.js";

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

const handleConfirmBooking = async ({ accountId, showtimeId, seatIds, useScore = false, paymentMethod, couponCode, promotionId: requestPromotionId, combos = [], scoreToRedeem, memberId, isGuestCheckout = false }) => {
  const session = await mongoose.startSession();
  session.startTransaction();

  try {
    // find member
    let member = null;
    if (memberId) {
      // Staff provided a specific member ID (verified member)
      member = await Member.findById(memberId).session(session);
      if (!member) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
      }
    } else if (!isGuestCheckout) {
      // Regular member app flow: find member by accountId
      member = await Member.findOne({ accountId: accountId }).session(session);
      if (!member) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");
      }
    }
    // else: isGuestCheckout = true and no memberId → walk-in guest, member remains null

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

    // --- AUTOMATIC INVENTORY DEDUCTION ---
    if (combos && combos.length > 0) {
      // 1. Get cinemaId from showtime
      const showtime = await Showtime.findById(showtimeId).session(session);
      if (!showtime) throw new ApiError(StatusCodes.NOT_FOUND, "Showtime not found");
      const cinemaId = showtime.cinemaId;

      // 2. Consolidate deductions
      const totalDeductions = {};
      for (const comboReq of combos) {
        const fbItem = await FoodBeverage.findById(comboReq.comboId).session(session);
        if (!fbItem || !fbItem.isActive) {
          throw new ApiError(StatusCodes.BAD_REQUEST, `F&B item ${comboReq.name} is no longer active`);
        }
        const deductions = fbItem.inventoryDeductions || [];
        if (deductions.length > 0) {
          deductions.forEach(deduction => {
            const needed = (deduction.quantity || 0) * (comboReq.quantity || 0);
            totalDeductions[deduction.itemCode] = (totalDeductions[deduction.itemCode] || 0) + needed;
          });
        }
      }

      // 3. Optimized Validation and Deduction (bulkWrite + Logging)
      const itemCodes = Object.keys(totalDeductions);
      if (itemCodes.length > 0) {
        // Fetch current stock for all items at once
        const currentInventories = await Inventory.find({
          cinemaId: new mongoose.Types.ObjectId(cinemaId),
          itemCode: { $in: itemCodes }
        }).session(session);

        const bulkOps = [];
        const logEntries = [];

        for (const itemCode of itemCodes) {
          const neededQty = totalDeductions[itemCode];
          if (neededQty <= 0) continue;

          const inv = currentInventories.find(i => i.itemCode === itemCode);
          if (!inv || inv.quantity < neededQty) {
            throw new ApiError(StatusCodes.BAD_REQUEST, `Sorry, some F&B items (Code: ${itemCode}) are out of stock.`);
          }

          // Prepare update operation
          bulkOps.push({
            updateOne: {
              filter: { _id: inv._id, quantity: { $gte: neededQty } },
              update: { $inc: { quantity: -neededQty } }
            }
          });

          // Prepare log entry
          logEntries.push({
            cinemaId: cinemaId,
            itemCode: itemCode,
            type: constants.INVENTORY_LOG_TYPE.SALE,
            changeQuantity: -neededQty,
            oldQuantity: inv.quantity,
            newQuantity: inv.quantity - neededQty,
            referenceId: null, // Will update with bookingId after creation
            reason: `Order checkout`
          });
        }

        if (bulkOps.length > 0) {
          const bulkResult = await Inventory.bulkWrite(bulkOps, { session });
          if (bulkResult.modifiedCount !== bulkOps.length) {
            throw new ApiError(StatusCodes.CONFLICT, "Inventory conflict detected. Please try again.");
          }
          // Note: Logs will be inserted after booking ID is generated to have proper reference
          session.userData = session.userData || {};
          session.userData.pendingLogs = logEntries;
        }
      }
    }
    // --- END INVENTORY DEDUCTION ---

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

    // Processing reward point redemptions (only if a member is linked)
    let pointUsed = 0;
    let pointDiscount = 0;
    let earnedPoints = 0;
    let finalAmount = afterPromotion;

    if (member) {
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

      await Member.updateOne(
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
    }
    // else: guest checkout — no points logic, finalAmount stays as afterPromotion

    const ticketCode = await ticketUtils.generateTicketCode();
    const qrCodeData = await ticketUtils.generateQRCode(ticketCode);

    // create booking
    const booking = await Booking.create(
      [
        {
          memberId: member ? member._id : null,
          showtimeId: showtimeId,
          bookingCode: ticketCode,
          promotionId: promotionId,
          seatIds: seatIds,
          seatNames: seats.map((seat) => seat.seatId.seatName),
          combos: combos,
          ticketPrice: ticketPrice,
          comboPrice: comboPrice,
          subTotalAmount: subTotal,
          paymentStatus: "paid",
          paymentMethod: paymentMethod,
          qrCodeData: qrCodeData,
          disCountAmount: promotionDiscount + pointDiscount,
          finalAmount: finalAmount,
        },
      ],
      { session },
    );

    // create score history only if a member is linked
    if (member) {
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
    }

    // Create Inventory Logs if any
    if (session.userData && session.userData.pendingLogs) {
      const logsToInsert = session.userData.pendingLogs.map(log => ({
        ...log,
        referenceId: booking[0]._id
      }));
      await InventoryLog.insertMany(logsToInsert, { session });
    }

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

    // Create PromotionUsage and update usageCount if promotion applied
    if (promotionId) {
      await PromotionUsage.create(
        [
          {
            promotionId: promotionId,
            accountId: member ? member.accountId : accountId,
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
      remainingPoints: member ? member.rewardPoints : null,
      membershipRank: member ? member.membershipRank : null,
    };

    await session.commitTransaction();

    // Trigger FCM notification asynchronously so it does not delay checkout response
    if (member) {
      NotificationService.sendBookingSuccessNotification(member.accountId, booking[0]).catch(() => {});
    }

    return result;
  } catch (error) {
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
    paymentStatus: { $in: ['paid', 'confirmed_transfer', 'successful'] },
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
      paymentMethod: booking.paymentMethod || "",
      isStaffRated: booking.isStaffRated || false,
      checkedIn: booking.checkedIn || false
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
    paymentStatus: { $in: ['paid', 'confirmed_transfer', 'successful'] },
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

  const reviews = await Review.find({ accountId }).lean();
  const reviewedMovieIds = new Set(reviews.map(r => r.movieId.toString()));

  const mappedBookings = bookings.map(booking => {
    const showtime = booking.showtimeId;
    const movie = showtime?.movieId;
    const cinema = showtime?.cinemaId;
    const cinemaRoom = showtime?.roomId;

    return {
      bookingId: booking._id,
      bookingCode: booking.bookingCode,
      movie: movie?.movieName || "",
      movieId: movie?._id || "",
      movieGenre: movie?.movieGenre || [],
      isMovieRated: movie ? reviewedMovieIds.has(movie._id.toString()) : false,
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
      paymentMethod: booking.paymentMethod || "",
      isStaffRated: booking.isStaffRated || false,
      checkedIn: booking.checkedIn || false
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

const handleRateStaff = async (bookingId, accountId, rating, comment) => {
  const member = await Member.findOne({ accountId });
  if (!member) throw new ApiError(StatusCodes.NOT_FOUND, "Member not found");

  const booking = await Booking.findOne({ _id: bookingId, memberId: member._id });
  if (!booking) throw new ApiError(StatusCodes.NOT_FOUND, "Booking not found");

  if (booking.isStaffRated) {
    throw new ApiError(StatusCodes.BAD_REQUEST, "Staff already rated for this booking");
  }

  if (!booking.checkedInBy) {
    throw new ApiError(StatusCodes.BAD_REQUEST, "Cannot rate staff before check-in");
  }

  const staff = await Staff.findById(booking.checkedInBy);
  if (!staff) throw new ApiError(StatusCodes.NOT_FOUND, "Staff member not found");

  // Update staff rating
  const newTotalStars = (staff.totalStars || 0) + rating;
  const newTotalRatings = (staff.totalRatings || 0) + 1;
  const newRating = newTotalStars / newTotalRatings;

  await Staff.findByIdAndUpdate(staff._id, {
    $set: {
      totalStars: newTotalStars,
      totalRatings: newTotalRatings,
      rating: newRating
    }
  });

  // Update booking
  booking.isStaffRated = true;
  booking.staffRating = rating;
  booking.staffComment = comment?.trim() || "";
  await booking.save();

  return { rating: newRating, totalRatings: newTotalRatings };
}

export const bookingService = {
  handleHoldSeats,
  handleConfirmBooking,
  handleGetHistory,
  handleGetScoreHistory,
  handleReleaseBooking,
  handleGetbookingDetail,
  handleGetMyTickets,
  handleRateStaff
};
