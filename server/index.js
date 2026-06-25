// Cú pháp ES Modules (ES6+) để import thư viện
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { env } from './src/config/environment.js';
import connectDB from './src/config/mongoDB.js';
import movieRoutes from './src/routes/movieRoutes.js';
import showtimeRoutes from './src/routes/showtimeRoutes.js';
import { accountRoute } from './src/routes/accountRoute.js';

const app = express();
const PORT = env.PORT || 5000;

app.use(helmet());
app.use(morgan('dev'));
app.use(cors());
app.use(express.json());


// Routes
app.use('/api/movies', movieRoutes);
app.use('/api/showtimes', showtimeRoutes);
app.use('/api/auth', accountRoute.authRouter);
app.use('/api/user', accountRoute.accountRouter);

const startServer = async () => {
    await connectDB();
    app.listen(PORT, () => {
        console.log(`🚀 Server ES Modules đang chạy tại: http://localhost:${PORT}`);
    });
}

startServer();  