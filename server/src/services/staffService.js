import { Booking } from "../models/bookingModel.js";
import { Staff } from "../models/staffModel.js";
import { Member } from "../models/memberModel.js";
import ApiError from "../utils/ApiError.js";
import { StatusCodes } from 'http-status-codes';

const checkStaff = async (accountId) => {
    const currentStaff = await Staff.findOne({ accountId });
    if (!currentStaff) {
        throw new ApiError(StatusCodes.FORBIDDEN, "You do not have permission to perform this function!");
    }
    return currentStaff;
};

const checkInTicket = async (accountId, { bookingCode }) => {
    // 1. Verify staff exists
    const staff = await checkStaff(accountId);

    if (!bookingCode) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "Booking code is required.");
    }

    // 2. Find the booking by code and populate showtime, movie, room and cinema details
    const booking = await Booking.findOne({ bookingCode })
        .populate({
            path: 'showtimeId',
            populate: [
                { path: 'movieId', select: 'movieName duration' },
                { path: 'roomId', select: 'roomName screenType' },
                { path: 'cinemaId', select: 'cinemaName' }
            ]
        });

    if (!booking) {
        throw new ApiError(StatusCodes.NOT_FOUND, "Booking not found.");
    }

    // 3. Verify payment status is successful
    if (booking.paymentStatus !== 'successful' && booking.paymentStatus !== 'paid') {
        throw new ApiError(StatusCodes.BAD_REQUEST, `Ticket payment status is not successful: ${booking.paymentStatus}`);
    }

    // 3.5. Compare showtime date with current date (today)
    if (booking.showtimeId && booking.showtimeId.showDate) {
        const showDate = new Date(booking.showtimeId.showDate);
        const today = new Date();

        const isSameDayUTC = showDate.getUTCFullYear() === today.getUTCFullYear() &&
                             showDate.getUTCMonth() === today.getUTCMonth() &&
                             showDate.getUTCDate() === today.getUTCDate();

        const isSameDayLocal = showDate.getFullYear() === today.getFullYear() &&
                               showDate.getMonth() === today.getMonth() &&
                               showDate.getDate() === today.getDate();

        if (!isSameDayUTC && !isSameDayLocal) {
            throw new ApiError(StatusCodes.BAD_REQUEST, "This ticket is not valid for today's show date.");
        }
    }

    // 4. Check if already checked in
    if (booking.checkedIn) {
        throw new ApiError(StatusCodes.BAD_REQUEST, "This ticket has already been checked-in.");
    }

    // 5. Perform check-in
    booking.checkedIn = true;
    booking.checkedInAt = new Date();
    booking.checkedInBy = staff._id;
    await booking.save();

    return {
        bookingId: booking._id,
        bookingCode: booking.bookingCode,
        checkedIn: booking.checkedIn,
        checkedInAt: booking.checkedInAt,
        checkedInBy: booking.checkedInBy,
        movieName: booking.showtimeId?.movieId?.movieName || null,
        duration: booking.showtimeId?.movieId?.duration || null,
        roomName: booking.showtimeId?.roomId?.roomName || null,
        screenType: booking.showtimeId?.roomId?.screenType || null,
        cinemaName: booking.showtimeId?.cinemaId?.cinemaName || null,
        showDate: booking.showtimeId?.showDate || null,
        showTime: booking.showtimeId?.showTime || null,
        seatNames: booking.seatNames || [],
        combos: booking.combos || [],
        finalAmount: booking.finalAmount
    };
};

const getMyRatings = async (accountId) => {
    const staff = await checkStaff(accountId);

    const ratings = await Booking.find({
        checkedInBy: staff._id,
        isStaffRated: true
    })
    .populate({
        path: 'memberId',
        select: 'fullName image',
        populate: {
            path: 'accountId',
            select: 'username'
        }
    })
    .sort({ updatedAt: -1 })
    .lean();

    return ratings.map(item => ({
        _id: item._id.toString(),
        rating: item.staffRating || 0,
        content: item.staffComment || "",
        createdAt: item.updatedAt || item.createdAt,
        accountId: {
            username: item.memberId?.accountId?.username || "Guest",
            memberInfo: {
                fullName: item.memberId?.fullName || "Anonymous",
                image: item.memberId?.image || null
            }
        }
    }));
};

export const staffService = {
    checkInTicket,
    getMyRatings
};
