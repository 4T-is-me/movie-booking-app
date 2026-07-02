import { Router } from 'express';
import { authMiddleware } from '../middlewares/authMiddleware.js';
import { getMemberList, adjustMemberScore, updateMemberStatus } from '../controllers/adminMemberController.js';

const router = Router();

//middleware check login and authorize roles admin
router.use(authMiddleware.isAuthorized, authMiddleware.authorizeRoles('Admin'));

//get list member for admin
router.get('/', getMemberList);

//patch /api/admin/members/:id/score - edit score member
router.patch('/:id/score', adjustMemberScore);

//patch /api/admin/members/:id/status - update status member account
router.patch('/:id/status', updateMemberStatus);

export default router;
