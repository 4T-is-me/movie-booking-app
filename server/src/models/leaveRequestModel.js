import mongoose from "mongoose";

const leaveRequestSchema = new mongoose.Schema(
  {
    staffId: { type: mongoose.Schema.Types.ObjectId, ref: "Staff", required: true },
    managerId: { type: mongoose.Schema.Types.ObjectId, ref: "Manager" },
    workDate: { type: Date, required: true },//Nghỉ đích danh ngày nào, ca nào
    shift: { type: String, required: true }, // 'Morning', 'Afternoon', 'Evening', 'Night'
    reason: { type: String, required: true },
    status: { type: String, default: "Pending" }, // "Pending", "Approved", "Rejected"
    approvedAt: { type: Date, default: null },
  },
  {
    versionKey: false,
    collection: "leave_requests",
    timestamps: true,
  }
);

// Thêm index để tối ưu việc check trùng lịch xin nghỉ
leaveRequestSchema.index({ staffId: 1, workDate: 1, shift: 1 });

export const LeaveRequest = mongoose.model("LeaveRequest", leaveRequestSchema);