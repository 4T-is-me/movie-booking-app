import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { managerRoomController } from '../controllers/managerRoomController.js';

const managerRoomRouter = Router();

//GET /api/manager/rooms - get rooms for manager
managerRoomRouter.get('/', authMiddleware.isAuthorized, authMiddleware.authorizeRoles("Manager", "Cinema Manager"), managerRoomController.getManagerRooms);

//GET /api/manager/rooms/:id/seats - get seats of a room for manager
managerRoomRouter.get('/:id/seats', authMiddleware.isAuthorized, authMiddleware.authorizeRoles("Manager", "Cinema Manager"), managerRoomController.getRoomSeats);

//PUT /api/manager/rooms/:id - update room info and seat pricing
managerRoomRouter.put('/:id', authMiddleware.isAuthorized, authMiddleware.authorizeRoles("Manager", "Cinema Manager"), managerRoomController.updateRoom);

export default managerRoomRouter;
