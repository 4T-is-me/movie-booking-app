

/* eslint-disable no-unused-vars */
import { StatusCodes } from 'http-status-codes'
import { env } from '~/config/environment'

// Centralized error handling middleware in Node.js Back-end application (ExpressJS)
export const errorHandlingMiddleware = (err, req, res, next) => {
    let statusCode = err.statusCode || StatusCodes.INTERNAL_SERVER_ERROR;
    let message = err.message || StatusCodes[statusCode];
    let errors = [];

    // Catch duplicate key error from MongoDB (E11000)
    if (err.code === 11000) {
        statusCode = StatusCodes.CONFLICT;
        message = 'Data duplication error';
        
        // Extract duplicate field name from err.keyValue
        const field = Object.keys(err.keyValue)[0];
        const value = err.keyValue[field];
        errors.push({
            field: field,
            message: `The ${field} '${value}' already exists.`
        });
    } else if (statusCode === StatusCodes.BAD_REQUEST || statusCode === StatusCodes.CONFLICT) {
        // Apply standard error format
        errors.push({ field: 'general', message: message });
        message = 'Validation or Conflict Error';
    }

    const responseError = {
        success: false,
        message: message,
        errors: errors.length > 0 ? errors : undefined
    };

    // Return Stack Trace only if the environment is DEV
    if (env.BUILD_MODE === 'dev' && statusCode === StatusCodes.INTERNAL_SERVER_ERROR) {
        responseError.stack = err.stack;
    }

    // Return responseError to Front-end
    res.status(statusCode).json(responseError);
}