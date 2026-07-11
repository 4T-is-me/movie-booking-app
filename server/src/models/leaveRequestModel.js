import mongoose from "mongoose";
const leaveRequestSchema = new mongoose.Schema(
  {
    staffId: {type: mongoose.Schema.Types.ObjectId, ref: "Staff", required: true},
    managerId: { type: mongoose.Schema.Types.ObjectId, ref: "Manager" },
    fromDate: { type: Date, required: true},
    toDate: { type: Date, required: true },
    reason: { type: String, required: true},
    status: { type: String,  default: "Pending"}, //"Pending", "Approved", "Rejected"
    approvedAt: { type: Date, default: null},
  },
  {
    versionKey: false,
    collection: "leave_requests",
    timestamps: true,
  },
);
export const LeaveRequest = mongoose.model("LeaveRequest", leaveRequestSchema);
