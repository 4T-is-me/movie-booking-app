import express from "express";
import { adminInventoryController } from "../controllers/adminInventoryController.js";
import { authMiddleware } from "../middlewares/authMiddleware.js";

const router = express.Router();

// Only admin can access inventory management according to latest lead instructions
router.get("/:cinemaId",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    adminInventoryController.getInventory
);

router.post("/refill",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    adminInventoryController.refill
);

export const adminInventoryRoutes = router;
