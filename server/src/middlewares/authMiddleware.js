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

export const authMiddleware = {
    isAuthorized
}
