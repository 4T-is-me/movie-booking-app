const SEAT_STATUS = {
    AVAILABLE: "available", 
    HOLD: "holding",
    SOLD: "booked"
}
const SHIFTS = [
    "Morning",
    "Afternoon",
    "Evening",
    "Night"
];

const SHIFT_START_HOURS = {
  Morning: 8,     // 08:00
  Afternoon: 12,  // 12:00
  Evening: 16,    // 16:00
  Night: 20       // 20:00
};

const STAFF_PER_SHIFT = 2;
const MAX_SHIFT_PER_DAY = 2;
const MAX_SHIFT_LEAVE_PER_DAY = 3;


export const constants = {  
    SEAT_STATUS,
    SHIFTS,
    STAFF_PER_SHIFT,
    MAX_SHIFT_PER_DAY,
    SHIFT_START_HOURS,
    MAX_SHIFT_LEAVE_PER_DAY
}