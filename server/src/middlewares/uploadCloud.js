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

const isMultipartRequest = (req) => {
    const contentType = req.headers['content-type'];
    return typeof contentType === 'string' && contentType.startsWith('multipart/form-data');
};

const uploadIfMultipart = (req, res, next) => {
    if (!isMultipartRequest(req)) {
        return next();
    }
    return upload.any()(req, res, (err) => {
        if (err) return next(err);
        if (req.files && req.files.length > 0) {
            req.file = req.files[0];
        }
        next();
    });
};

const normalizeMultipartBody = (req, res, next) => {
    if (!req.body || typeof req.body !== 'object') {
        return next();
    }

    const normalizedBody = { ...req.body };

    Object.entries(normalizedBody).forEach(([key, value]) => {
        if (typeof value !== 'string') {
            return;
        }

        const trimmedValue = value.trim();
        if (trimmedValue === '' || trimmedValue === 'null' || trimmedValue === 'undefined') {
            normalizedBody[key] = null;
            return;
        }

        if (['movieGenre', 'director', 'actors'].includes(key)) {
            normalizedBody[key] = trimmedValue
                .split(',')
                .map((item) => item.trim())
                .filter(Boolean);
            return;
        }

        if (key === 'inventoryDeductions') {
            try {
                normalizedBody[key] = JSON.parse(trimmedValue);
                return;
            } catch (e) {
                normalizedBody[key] = [];
                return;
            }
        }

        if (['duration', 'rating', 'totalReviews', 'totalStarts', 'tier'].includes(key)) {
            const parsedNumber = Number(trimmedValue);
            if (!Number.isNaN(parsedNumber)) {
                normalizedBody[key] = parsedNumber;
            }
        }
    });

    req.body = normalizedBody;
    next();
};

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
    uploadIfMultipart,
    normalizeMultipartBody,
    uploadToCloudinary
};