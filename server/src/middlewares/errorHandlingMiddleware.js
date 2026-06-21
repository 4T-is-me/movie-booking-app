

/* eslint-disable no-unused-vars */
import { StatusCodes } from 'http-status-codes'
import { env } from '~/config/environment'

// Middleware xử lý lỗi tập trung trong ứng dụng Back-end NodeJS (ExpressJS)
export const errorHandlingMiddleware = (err, req, res, next) => {
    let statusCode = err.statusCode || StatusCodes.INTERNAL_SERVER_ERROR;
    let message = err.message || StatusCodes[statusCode];
    let errors = [];

    // Bắt lỗi trùng lặp từ MongoDB (E11000)
    if (err.code === 11000) {
        statusCode = StatusCodes.CONFLICT;
        message = 'Data duplication error';
        
        // Trích xuất tên field bị trùng từ err.keyValue
        const field = Object.keys(err.keyValue)[0];
        const value = err.keyValue[field];
        errors.push({
            field: field,
            message: `The ${field} '${value}' already exists.`
        });
    } else if (statusCode === StatusCodes.BAD_REQUEST || statusCode === StatusCodes.CONFLICT) {
        // Áp dụng format lỗi chuẩn
        errors.push({ field: 'general', message: message });
        message = 'Validation or Conflict Error';
    }

    const responseError = {
        success: false,
        message: message,
        errors: errors.length > 0 ? errors : undefined
    };

    // Chỉ khi môi trường là DEV thì mới trả về Stack Trace
    if (env.BUILD_MODE === 'dev' && statusCode === StatusCodes.INTERNAL_SERVER_ERROR) {
        responseError.stack = err.stack;
    }

    // Trả responseError về phía Front-end
    res.status(statusCode).json(responseError);
}