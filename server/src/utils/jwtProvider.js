import jwt from 'jsonwebtoken'

const generateAccessToken = (payload) => {
  if (!process.env.ACCESS_TOKEN_SECRET_KEY) {
    throw new Error('ACCESS_TOKEN_SECRET_KEY is missing in environment variables')
  }
  return jwt.sign(payload, process.env.ACCESS_TOKEN_SECRET_KEY, { expiresIn: process.env.ACCESS_TOKEN_LIFE })
}

const verifyAccessToken = (token) => {
  if (!process.env.ACCESS_TOKEN_SECRET_KEY) {
    throw new Error('ACCESS_TOKEN_SECRET_KEY is missing in environment variables')
  }
  return jwt.verify(token, process.env.ACCESS_TOKEN_SECRET_KEY)
}

const generateRefreshToken = (payload) => {
  if (!process.env.REFRESH_TOKEN_SECRET_KEY) {
    throw new Error('REFRESH_TOKEN_SECRET_KEY is missing in environment variables')
  }
  return jwt.sign(payload, process.env.REFRESH_TOKEN_SECRET_KEY, { expiresIn: process.env.REFRESH_TOKEN_LIFE })
}

const verifyRefreshToken = (token) => {
  if (!process.env.REFRESH_TOKEN_SECRET_KEY) {
    throw new Error('REFRESH_TOKEN_SECRET_KEY is missing in environment variables')
  }
  return jwt.verify(token, process.env.REFRESH_TOKEN_SECRET_KEY)
}

export const jwtProvider = {
  generateAccessToken,
  verifyAccessToken,
  generateRefreshToken,
  verifyRefreshToken
}
