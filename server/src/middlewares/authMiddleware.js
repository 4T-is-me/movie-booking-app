import { StatusCodes } from 'http-status-codes'
import ApiError from '../utils/ApiError.js'
import { jwtProvider } from '../utils/jwtProvider.js'

const isAuthorized = async (req, res, next) => {
    try {
        const authorizationHeader = req.headers.authorization

        if (!authorizationHeader || !authorizationHeader.startsWith('Bearer ')) {
            throw new ApiError(StatusCodes.UNAUTHORIZED, 'Access token is missing')
        }

        const accessToken = authorizationHeader.replace('Bearer ', '').trim()
        const decoded = jwtProvider.verifyAccessToken(accessToken)
        req.user = decoded
        req.jwtDecoded = decoded

        next()
    } catch (error) {
        if (error.name === 'TokenExpiredError') {
            next(new ApiError(StatusCodes.UNAUTHORIZED, 'Access token expired'))
            return
        }

        if (error.name === 'JsonWebTokenError') {
            next(new ApiError(StatusCodes.UNAUTHORIZED, 'Access token is invalid'))
            return
        }

        next(error)
    }
}

const authorizeRoles = (...allowedRoles) => {
    return (req, res, next) => {
        // req.user được gán từ hàm isAuthorized ở trên
        const userRole = req.user.roleId;

        // Kiểm tra xem role của user có nằm trong danh sách được phép không
        if (!allowedRoles.includes(userRole)) {
            return next(new ApiError(StatusCodes.FORBIDDEN, 'Bạn không có quyền truy cập tài nguyên này!'));
        }

        next();
    };
};

export const authMiddleware = {
    isAuthorized,
    authorizeRoles
}
