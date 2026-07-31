import express from "express";
import { importOrderController } from "../controllers/importOrderController.js";
import { authMiddleware } from "../middlewares/authMiddleware.js";

const router = express.Router();

// GET /api/manager/suppliers - Danh sách nhà cung cấp (Manager dùng để chọn khi lập phiếu)
router.get("/suppliers",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Manager", "Cinema Manager"),
    importOrderController.getSuppliers
);

// GET /api/manager/inventory - Danh sách sản phẩm trong kho của rạp mình
router.get("/inventory",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Manager", "Cinema Manager"),
    importOrderController.getManagerInventory
);

// GET /api/manager/import-orders - Danh sách phiếu nhập của rạp mình
router.get("/import-orders",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Manager", "Cinema Manager"),
    importOrderController.getManagerImportOrders
);

// POST /api/manager/import-orders - Tạo phiếu nhập hàng mới (status = PENDING)
router.post("/import-orders",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Manager", "Cinema Manager"),
    importOrderController.createImportOrder
);

// PUT /api/manager/import-orders/:id/cancel - Manager hủy phiếu đang chờ duyệt
router.put("/import-orders/:id/cancel",
    authMiddleware.isAuthorized,
    authMiddleware.authorizeRoles("Manager", "Cinema Manager"),
    importOrderController.cancelImportOrder
);

export const managerImportOrderRoutes = router;
