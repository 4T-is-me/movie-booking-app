import mongoose from "mongoose";

const leaveRequestSchema = new mongoose.Schema(
  {
    staffId: { type: mongoose.Schema.Types.ObjectId,   ref: "Staff",  required: true },
    staffShiftId: {  type: mongoose.Schema.Types.ObjectId,  ref: "StaffShift",  required: true  },
    managerId: {  type: mongoose.Schema.Types.ObjectId,  ref: "Manager"  },
    reason: { type: String, required: true },
    status: { type: String, default: "Pending" },
    approvedAt: { type: Date, default: null },
    rejectReason: { type: String, default: null },
    managerId: { type: mongoose.Schema.Types.ObjectId, ref: "Manager", default: null },
  },
  {
    versionKey: false,
    collection: "leave_requests",
    timestamps: true,
  }
);


// Index tối ưu performance cho việc query đơn nghỉ theo ca và nhân viên
leaveRequestSchema.index({ staffShiftId: 1 });
leaveRequestSchema.index({ staffId: 1, status: 1 });

export const LeaveRequest = mongoose.model("LeaveRequest", leaveRequestSchema);