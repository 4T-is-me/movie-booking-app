import express from "express";
import { foodBeverageController } from "../controllers/foodBeverageController.js";
import { authMiddleware } from "../middlewares/authMiddleware.js";
import { uploadMiddleware } from "../middlewares/uploadCloud.js";

const router = express.Router();

// Public / Customer routes
router.get("/", foodBeverageController.getMenu);

// Admin routes
router.get("/admin",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    foodBeverageController.getAdminMenu
);

router.post("/admin",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    uploadMiddleware.uploadIfMultipart,
    uploadMiddleware.normalizeMultipartBody,
    uploadMiddleware.uploadToCloudinary('food_beverages', 'imageUrl'),
    foodBeverageController.addProduct
);

router.put("/admin/:id",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    uploadMiddleware.uploadIfMultipart,
    uploadMiddleware.normalizeMultipartBody,
    uploadMiddleware.uploadToCloudinary('food_beverages', 'imageUrl'),
    foodBeverageController.updateProduct
);

router.delete("/admin/:id",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    foodBeverageController.deleteProduct
);

export const foodBeverageRoutes = router;
