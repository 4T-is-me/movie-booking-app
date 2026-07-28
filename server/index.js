// ES Modules syntax (ES6+) to import libraries
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { env } from './src/config/environment.js';
import connectDB from './src/config/mongoDB.js';
import { CleanUpJobs } from './src/jobs/cleanUpJobs.js';
import { Cinema } from './src/models/cinemaModel.js';
import movieRoutes from './src/routes/movieRoutes.js';
import showtimeRoutes from './src/routes/showtimeRoutes.js';
import { accountRoute } from './src/routes/accountRoute.js';
import { bookingRoute } from './src/routes/bookingRoute.js';
import { memberRoute } from './src/routes/memberRoute.js';
import adminMovieRoutes from './src/routes/adminMovieRoutes.js';
import adminShowtimeRoutes from './src/routes/adminShowtimeRoutes.js';
import { adminRoomRoute } from './src/routes/adminRoomRoutes.js';
import { adminCinemaRoute } from './src/routes/adminCinemaRoutes.js';
import adminStaffRoutes from './src/routes/adminStaffRoutes.js';
import adminMemberRoutes from './src/routes/adminMemberRoutes.js';
import promotionRoute from './src/routes/promotionRoute.js';
import adminStatsRoutes from './src/routes/adminStatsRoutes.js';
import adminPromotionRoutes from './src/routes/adminPromotionRoutes.js';
import adminShowDateRoutes from './src/routes/adminShowDateRoutes.js';
import managerRoomRoutes from './src/routes/managerRoomRoutes.js';
import managerDashboardRoutes from './src/routes/managerDashboardRoute.js';
import managerReportRoutes from './src/routes/managerReportRoute.js';
import managerStaffShiftRoutes from './src/routes/managerStaffShiftRoutes.js';
import maintenanceRoutes from './src/routes/maintenanceRoutes.js';
import notificationRoutes from './src/routes/notificationRoutes.js';
import staffLeaveRequestRoutes from './src/routes/staffLeaveRequestRoutes.js';
import { staffRoute } from './src/routes/staffRoutes.js';
import { errorHandlingMiddleware } from './src/middlewares/errorHandlingMiddleware.js'
import './src/config/firebaseAdmin.js'  // Initialize Firebase Admin SDK at startup;
import { ShowtimeReminderJob } from './src/jobs/showtimeReminderJob.js';
import payrollRoutes from './src/routes/payrollRoutes.js';
import payrollConfigRoutes from './src/routes/payrollConfigRoutes.js';
// Added by User - ChatBot AI feature - 2026-07-26
import chatRoutes from './Chat_Bot_AI/routes/chatRoutes.js';


const app = express();
const PORT = env.PORT || 5000;

app.use(helmet());
app.use(morgan('dev'));
app.use(cors());
app.use(express.json());
app.use('/uploads', express.static('uploads'));


// Routes
app.use('/api/movies', movieRoutes);
app.use('/api/showtimes', showtimeRoutes);
app.use('/api/auth', accountRoute.authRouter);
app.use('/api/users', accountRoute.accountRouter);
app.use('/api/accounts', accountRoute.accountRouter);
app.use('/api/members', memberRoute.memberRouter);
app.use('/api/booking', bookingRoute.bookingRouter);
app.use('/api/promotions', promotionRoute);
app.use('/api/admin/showtimes', adminShowtimeRoutes);
app.use('/api/admin/movies', adminMovieRoutes);
app.use('/api/admin/rooms', adminRoomRoute.adminRoomRouter);
app.use('/api/admin/cinemas', adminCinemaRoute.adminCinemaRouter);
app.use('/api/admin/staff', adminStaffRoutes);
app.use('/api/admin/members', adminMemberRoutes);
app.use('/api/admin/promotions', adminPromotionRoutes);
app.use('/api/admin/show-dates', adminShowDateRoutes);
app.use('/api/admin/stats', adminStatsRoutes);
app.use('/api/manager/rooms', managerRoomRoutes);
app.use('/api/manager/dashboard', managerDashboardRoutes);
app.use('/api/manager/reports', managerReportRoutes);
app.use('/api/manager/staff-shifts', managerStaffShiftRoutes);
app.use('/api/staffs/leave-requests', staffLeaveRequestRoutes);
app.use('/api/staff', staffRoute.staffRouter);
app.use('/api/maintenance', maintenanceRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/payrolls', payrollRoutes);
app.use('/api/admin/payroll-config', payrollConfigRoutes);
// Added by User - ChatBot AI feature - 2026-07-26
app.use('/api/chat', chatRoutes);



app.get('/api/cinemas', async (req, res) => {
    try {
        const cinemas = await Cinema.find({});
        res.json({ success: true, data: cinemas });
    } catch (e) {
        res.status(500).json({ success: false, message: e.message });
    }
});

// Centralized error handling middleware
app.use(errorHandlingMiddleware);

const startServer = async () => {
    await connectDB();
    await CleanUpJobs.startSeatCleanupJob();
    await CleanUpJobs.updateMovieStatus();
    await CleanUpJobs.updateStaffShiftStatus();
    await CleanUpJobs.autoRejectAndAbsentJob();
    await ShowtimeReminderJob.startShowtimeReminderJob();

    app.listen(PORT, () => {
        console.log(`🚀 ES Modules Server is running at: http://localhost:${PORT}`);
    });
}

startServer();  