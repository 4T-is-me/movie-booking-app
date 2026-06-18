// Cú pháp ES Modules (ES6+) để import thư viện
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import dotenv from 'dotenv';

// Khởi tạo dotenv để đọc file .env
dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

app.use(helmet());
app.use(morgan('dev'));
app.use(cors());
app.use(express.json());

// API mẫu viết bằng ES6
app.get('/api/movies', (req, res) => {
    res.json({ message: "API danh sách phim chạy bằng chuẩn ES Modules cực mượt!" });
});

app.listen(PORT, () => {
    console.log(`🚀 Server ES Modules đang chạy tại: http://localhost:${PORT}`);
});