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
        // req.user is assigned from the isAuthorized function above, now containing the role in the new format from the payload ({ accountId, role, username })
        const userRole = req.user.role;

        // Check if the user's role is in the allowed list
        if (!userRole || !allowedRoles.includes(userRole)) {
            return next(new ApiError(StatusCodes.FORBIDDEN, 'Access Denied: You do not have permission to access this resource!'));
        }

        next();
    };
};

const optionalAuthorized = async (req, res, next) => {
    try {
        const authorizationHeader = req.headers.authorization

        if (authorizationHeader && authorizationHeader.startsWith('Bearer ')) {
            const accessToken = authorizationHeader.replace('Bearer ', '').trim()
            const decoded = jwtProvider.verifyAccessToken(accessToken)
            req.user = decoded
            req.jwtDecoded = decoded
        }
        next()
    } catch (error) {
        next()
    }
}

export const authMiddleware = {
    isAuthorized,
    optionalAuthorized,
    authorizeRoles
}
