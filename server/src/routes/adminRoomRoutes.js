import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { adminRoomController } from '../controllers/adminRoomController.js';

const adminRoomRouter = Router();

//POST /api/admin/rooms - create a new room
adminRoomRouter.post('/',authMiddleware.isAuthorized,authMiddleware.authorizeRoles("Admin"),adminRoomController.creatRoomWithSeats)
//GET /api/admin/rooms - get all rooms(with role admin)
adminRoomRouter.get('/',authMiddleware.isAuthorized,authMiddleware.authorizeRoles("Admin"),adminRoomController.getAllRooms)
//GET /api/admin/rooms/:id/seats - get seats of a room
adminRoomRouter.get('/:id/seats',authMiddleware.isAuthorized,authMiddleware.authorizeRoles("Admin"),adminRoomController.getRoomSeats)
//PATCH /api/admin/rooms/:id/seats - update seat types of a room
adminRoomRouter.patch('/:id/seats',authMiddleware.isAuthorized,authMiddleware.authorizeRoles("Admin"),adminRoomController.updateSeatTypes)
//DELETE /api/admin/rooms/:id - delete a room
adminRoomRouter.delete('/:id',authMiddleware.isAuthorized,authMiddleware.authorizeRoles("Admin"),adminRoomController.softDeleteRoom)
export const adminRoomRoute = {
    adminRoomRouter
}