// Cú pháp ES Modules (ES6+) để import thư viện
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { env } from './src/config/environment.js';
import connectDB from './src/config/mongoDB.js';
import { seatHoldCleanUpJobs } from './src/jobs/seatHoldCleanUpJobs.js';
import movieRoutes from './src/routes/movieRoutes.js';
import showtimeRoutes from './src/routes/showtimeRoutes.js';
import { accountRoute } from './src/routes/accountRoute.js';
import { bookingRoute } from './src/routes/bookingRoute.js';
import { memberRoute } from './src/routes/memberRoute.js';
import adminMovieRoutes from './src/routes/adminMovieRoutes.js';
import adminShowtimeRoutes from './src/routes/adminShowtimeRoutes.js';
import promotionRoute from './src/routes/promotionRoute.js';

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

const startServer = async () => {
    await connectDB();
    seatHoldCleanUpJobs.startSeatCleanupJob();
    app.listen(PORT, () => {
        console.log(`🚀 Server ES Modules đang chạy tại: http://localhost:${PORT}`);
    });
}

startServer();  