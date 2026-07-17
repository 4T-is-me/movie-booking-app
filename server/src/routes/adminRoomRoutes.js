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
//GET /api/admin/rooms/:id - get room details
adminRoomRouter.get('/:id',authMiddleware.isAuthorized,authMiddleware.authorizeRoles("Admin"),adminRoomController.getRoomDetails)
//PUT /api/admin/rooms/:id - update a room
adminRoomRouter.put('/:id',authMiddleware.isAuthorized,authMiddleware.authorizeRoles("Admin"),adminRoomController.updateRoom)
//PATCH /api/admin/rooms/:id/status - soft delete / reactivate room
adminRoomRouter.patch('/:id/status',authMiddleware.isAuthorized,authMiddleware.authorizeRoles("Admin"),adminRoomController.toggleRoomStatus)
//DELETE /api/admin/rooms/:id - delete a room
adminRoomRouter.delete('/:id',authMiddleware.isAuthorized,authMiddleware.authorizeRoles("Admin"),adminRoomController.softDeleteRoom)
export const adminRoomRoute = {
    adminRoomRouter
}