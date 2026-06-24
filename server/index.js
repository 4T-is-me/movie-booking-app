// Cú pháp ES Modules (ES6+) để import thư viện
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { env } from './src/config/environment.js';
import connectDB from './src/config/mongoDB.js';
import {seatHoldCleanUpJobs} from './src/jobs/seatHoldCleanUpJobs.js';
import movieRoutes from './src/routes/movieRoutes.js';
import showtimeRoutes from './src/routes/showtimeRoutes.js';
import { accountRoute } from './src/routes/accountRoute.js';
import { bookingRoute } from './src/routes/bookingRoute.js';

const app = express();
const PORT = env.PORT || 5000;

app.use(helmet());
app.use(morgan('dev'));
app.use(cors());
app.use(express.json());

// API mẫu viết bằng ES6
app.get('/api/movies', (req, res) => {
    res.json({ message: "API danh sách phim chạy bằng chuẩn ES Modules cực mượt!" });
});

// Routes
app.use('/api/movies', movieRoutes);
app.use('/api/showtimes', showtimeRoutes);
app.use('/api/auth', accountRoute.authRouter);
app.use('/api/user', accountRoute.accountRouter);
app.use('/api/booking', bookingRoute.bookingRouter);

const startServer = async () => {
    await connectDB();
    seatHoldCleanUpJobs.startSeatCleanupJob();
    app.listen(PORT, () => {
        console.log(`🚀 Server ES Modules đang chạy tại: http://localhost:${PORT}`);
    });
}

startServer();  