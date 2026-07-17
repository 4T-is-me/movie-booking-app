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

const MAX_SHIFT_LEAVE_PER_DAY = 3;


export const constants = {  
    SEAT_STATUS,
    SHIFTS,
    SHIFT_START_HOURS,
    MAX_SHIFT_LEAVE_PER_DAY
}