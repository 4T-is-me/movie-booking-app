

/**
 * Custom ApiError class extending the built-in Error class (Best Practice for custom error handling)
 */
class ApiError extends Error {
    constructor(statusCode, message) {
        // Call the parent class Error constructor to enable using 'this'
        // The parent (Error) already has the message property, so pass it to super
        super(message)

        // Custom name for this error, defaults to "Error" if not set
        this.name = 'ApiError'

        // Assign HTTP status code
        this.statusCode = statusCode

        // Capture stack trace for easier debugging
        Error.captureStackTrace(this, this.constructor)
    }
}

export default ApiError