import express from "express";
import { importOrderController } from "../controllers/importOrderController.js";
import { authMiddleware } from "../middlewares/authMiddleware.js";

const router = express.Router();

// POST /api/admin/suppliers - Thêm nhà cung cấp mới
router.post("/suppliers",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    importOrderController.createSupplier
);

// GET /api/admin/import-orders - Xem toàn bộ phiếu nhập (có thể lọc theo status, cinemaId)
router.get("/import-orders",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    importOrderController.getAllImportOrders
);

// PUT /api/admin/import-orders/:id/approve - Duyệt phiếu + cộng kho (Transaction)
router.put("/import-orders/:id/approve",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    importOrderController.approveImportOrder
);

// PUT /api/admin/import-orders/:id/reject - Từ chối phiếu
router.put("/import-orders/:id/reject",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Admin"),
    importOrderController.rejectImportOrder
);

export const adminImportOrderRoutes = router;
