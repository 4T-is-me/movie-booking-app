import mongoose from 'mongoose';

const staffShiftSchema = new mongoose.Schema({
    staffId: {type: mongoose.Schema.Types.ObjectId, ref: 'Staff',required: true},
    cinemaId: {type: mongoose.Schema.Types.ObjectId, ref: 'Cinema', required: true},
    workDate: { type: Date, required: true },
    shift: { type: String, required: true },//'Morning', 'Afternoon', 'Evening', 'Night'
    status: { type: String, default: 'Assigned' } //'Assigned', 'Completed', 'Absent',
}, {
    timestamps: true,
    versionKey: false,
    collection: 'staff_shifts'
});
staffShiftSchema.index( { staffId: 1, workDate: 1, shift: 1 },{ unique: true });

export const StaffShift = mongoose.model('StaffShift', staffShiftSchema);