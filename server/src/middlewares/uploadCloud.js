import { v2 as cloudinary } from "cloudinary";
import multer from "multer";
import ApiError from "../utils/ApiError.js";
import { StatusCodes } from "http-status-codes";
cloudinary.config({ 
    cloud_name: process.env.CLOUDINARY_CLOUD_NAME, 
    api_key: process.env.CLOUDINARY_API_KEY, 
    api_secret: process.env.CLOUDINARY_API_SECRET }
);
const storage = multer.memoryStorage();

const upload = multer({
    storage,
    limits: {
        fileSize: 5 * 1024 * 1024 // limit file size to 5MB
    }
});

const uploadToCloudinary = (folder, bodyKey) => {
    return (req, res, next) => {
        if (!req.file) {
            return next();
        }

        cloudinary.uploader.upload_stream(
            { folder, resource_type: "image" },
            (error, result) => {
                if (error) {
                    console.error(">>> CLOUDINARY ERROR DETAILS:", error);
                    return next(new ApiError(StatusCodes.INTERNAL_SERVER_ERROR, "Upload image failed!"));
                }
                req.body[bodyKey] = result.secure_url;
                next();
            }
        ).end(req.file.buffer);
    };
};

export const uploadMiddleware = {
    upload,
    uploadToCloudinary
};