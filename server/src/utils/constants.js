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

const SHIFT_RATES = {
  Morning: 150000,
  Afternoon: 150000,
  Evening: 150000,
  Night: 200000,
};

const HOLIDAY_MULTIPLIER = 1.5;
const MANAGER_REVENUE_BONUS = 2000000;
const SHIFT_START_HOURS = {
  Morning: 8,     // 08:00
  Afternoon: 12,  // 12:00
  Evening: 16,    // 16:00
  Night: 20       // 20:00
};

const MAX_SHIFT_LEAVE_PER_DAY = 3;


const ROLES = {
    ADMIN: 'Admin',
    MANAGER: 'Manager',
    STAFF: 'Staff',
    EMPLOYEE: 'Employee',
    MEMBER: 'Member',
    CUSTOMER: 'Customer',
    CINEMA_MANAGER: 'Cinema Manager'
};

const IMPORT_ORDER_STATUS = {
    PENDING: 'PENDING',
    APPROVED: 'APPROVED',
    REJECTED: 'REJECTED',
    CANCELLED: 'CANCELLED'
};

const INVENTORY_LOG_TYPE = {
    REFILL: 'REFILL',
    SALE: 'SALE',
    ADJUST: 'ADJUST'
};

const ORDER_PREFIX = {
    IMPORT: 'PN-'
};

export const constants = {  
    SEAT_STATUS,
    SHIFTS,
    SHIFT_START_HOURS,
    MAX_SHIFT_LEAVE_PER_DAY,
    SHIFT_RATES,
    HOLIDAY_MULTIPLIER,
    MANAGER_REVENUE_BONUS,
    ROLES,
    IMPORT_ORDER_STATUS,
    INVENTORY_LOG_TYPE,
    ORDER_PREFIX
}