package com.example.movie_booking_app.utils

import com.example.movie_booking_app.data.model.response.ManagerStaffItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoField

data class StaffShiftDraft(
    val staffId: String,
    val staffName: String,
    val position: String,
    val cinemaId: String,
    val phone: String,
    val workDate: String, // "yyyy-MM-dd"
    val shift: String,
    val status: String,
    val isHoliday: Boolean
)

object AutoStaffSchedulingAlgorithm {

    data class ShiftPair(val name: String, val shifts: List<String>)

    private val SHIFT_PAIRS = listOf(
        ShiftPair("Morning - Afternoon", listOf("Morning", "Afternoon")),
        ShiftPair("Evening - Night", listOf("Evening", "Night"))
    )

    /**
     * Hàm kiểm tra trước xem số lượng nhân viên và cấu hình có đủ gánh lịch tuần/tháng không.
     * Chặn đứng tình trạng sinh thiếu ca, trống ca do cạn kiệt nhân sự.
     */
    fun validateSchedulingCapability(
        startDate: LocalDate,
        endDate: LocalDate,
        selectedStaff: List<ManagerStaffItem>,
        baseStaffPerShift: Int,
        holidayDates: List<String> = emptyList()
    ): Pair<Boolean, String> {
        if (startDate.isAfter(endDate)) return Pair(false, "Invalid date range!")
        if (selectedStaff.isEmpty()) return Pair(false, "Please select at least 1 staff member!")

        // 1. Tính tổng số ca rạp CẦN (Nhu cầu thực tế của rạp)
        var totalRequiredShifts = 0
        var tempDate = startDate
        while (!tempDate.isAfter(endDate)) {
            val dateStr = tempDate.toString()
            val isHoliday = holidayDates.contains(dateStr)
            val isWeekend = tempDate.dayOfWeek == DayOfWeek.SATURDAY || tempDate.dayOfWeek == DayOfWeek.SUNDAY

            // 4 ca/ngày, cuối tuần tự cộng thêm 1 người/ca
            val staffPerShiftForDay = if (isHoliday || isWeekend) baseStaffPerShift + 1 else baseStaffPerShift
            totalRequiredShifts += Constants.MAX_SHIFT_OF_DATE * staffPerShiftForDay

            tempDate = tempDate.plusDays(1)
        }

        // 2. Tính số ca TỐI ĐA nhân viên có thể gánh (Sức lao động kịch trần)
        // Luật: workedDaysInWeek < 7 => Mỗi người làm tối đa 6 ngày/tuần
        // Ép cặp ca => Mỗi ngày đi làm nuốt liền 2 ca. Tức là tối đa 12 ca/tuần cho mỗi người.
        val totalDaysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
        val totalWeeks = Math.ceil(totalDaysInPeriod.toDouble() / 7.0).toInt()

        val maxShiftsPerStaff = totalWeeks * 6 * 2 // (Số tuần * 6 ngày * 2 ca)
        val totalMaxCapacityShifts = selectedStaff.size * maxShiftsPerStaff

        // 3. So sánh nhu cầu rạp và khả năng đáp ứng
        if (totalRequiredShifts > totalMaxCapacityShifts) {
            val missingShifts = totalRequiredShifts - totalMaxCapacityShifts
            val recommendedStaffCount = Math.ceil(totalRequiredShifts.toDouble() / maxShiftsPerStaff.toDouble()).toInt()

            val errorMsg = "The cinema needs $totalRequiredShifts shifts, but the ${selectedStaff.size} selected staff can only work a maximum of $totalMaxCapacityShifts shifts (Shortage of $missingShifts shifts).\n\nPlease select at least $recommendedStaffCount staff members or reduce the staff count per shift!"
            return Pair(false, errorMsg)
        }

        return Pair(true, "")
    }

    /**
     * Hàm tự động sắp xếp lịch xoay vòng bọc lót nhau, đảm bảo đi liền ca
     */
    fun generateStaffSchedule(
        startDate: LocalDate,
        endDate: LocalDate,
        selectedStaff: List<ManagerStaffItem>,
        cinemaId: String,
        baseStaffPerShift: Int = 2,
        holidayDates: List<String> = emptyList(),
        approvedLeaveRequests: List<Pair<String, String>> = emptyList()
    ): List<StaffShiftDraft> {

        if (startDate.isAfter(endDate)) return emptyList()

        val result = mutableListOf<StaffShiftDraft>()

        // Khởi tạo bộ đếm tổng thể
        val overallStaffCount = mutableMapOf<String, Int>()
        val weeklyWorkMap = mutableMapOf<String, Int>()
        val shiftOccupancy = mutableMapOf<String, MutableList<String>>()
        val staffDailyCount = mutableMapOf<String, Int>()

        selectedStaff.forEach { staff ->
            overallStaffCount[staff.id] = 0
        }

        val dateList = mutableListOf<LocalDate>()
        var tempDate = startDate
        while (!tempDate.isAfter(endDate)) {
            dateList.add(tempDate)
            tempDate = tempDate.plusDays(1)
        }

        // Sắp xếp thứ tự ưu tiên duyệt ngày: Ngày Lễ/Cuối tuần lên đầu, Ngày thường xuống sau
        val sortedDates = dateList.sortedWith(compareByDescending<LocalDate> { date ->
            holidayDates.contains(date.toString()) || date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        }.thenBy { date -> date })

        val todayStr = LocalDate.now().toString()

        // Biến đếm dùng để đảo thứ tự ưu tiên cặp ca theo từng ngày
        var dayCounter = 0

        for (currentDate in sortedDates) {
            val dateStr = currentDate.toString()
            val dayOfWeek = currentDate.dayOfWeek

            val weekYear = currentDate.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
            val year = currentDate.year

            val isHoliday = holidayDates.contains(dateStr)
            val isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

            val currentRequiredStaff = if (isHoliday || isWeekend) {
                baseStaffPerShift + 1
            } else {
                baseStaffPerShift
            }

            val isToday = dateStr == todayStr
            val initialStatus = if (isToday) "Active" else "Assigned"

            val availableStaffForDay = selectedStaff.filter { staff ->
                val hasLeaveApproved = approvedLeaveRequests.any { it.first == staff.id && it.second == dateStr }
                !hasLeaveApproved && staff.status == 1
            }

            // Xoay vòng cặp ca liên tục qua các ngày để Ngày và Đêm công bằng như nhau
            val rotatedPairs = if (dayCounter % 2 == 0) {
                SHIFT_PAIRS
            } else {
                SHIFT_PAIRS.reversed()
            }
            dayCounter++

            for (pair in rotatedPairs) {
                val shift1 = pair.shifts[0]
                val shift2 = pair.shifts[1]

                val keyOcc1 = "${dateStr}_$shift1"
                val keyOcc2 = "${dateStr}_$shift2"

                val occ1 = shiftOccupancy[keyOcc1]?.size ?: 0
                val occ2 = shiftOccupancy[keyOcc2]?.size ?: 0

                val neededPairs = Math.min(currentRequiredStaff - occ1, currentRequiredStaff - occ2)
                if (neededPairs <= 0) continue

                for (p in 0 until neededPairs) {
                    val eligibleForPair = availableStaffForDay.filter { staff ->
                        val id = staff.id
                        val dailyCount = staffDailyCount["${dateStr}_$id"] ?: 0
                        val weekKey = "${year}_${weekYear}_$id"
                        val workedDaysInWeek = weeklyWorkMap[weekKey] ?: 0

                        val isFree1 = !(shiftOccupancy[keyOcc1] ?: emptyList()).contains(id)
                        val isFree2 = !(shiftOccupancy[keyOcc2] ?: emptyList()).contains(id)

                        //  Đã làm một cặp ca trong ngày (dailyCount > 0) thì loại, không xếp trùng nữa
                        dailyCount == 0 && isFree1 && isFree2 && workedDaysInWeek < 7
                    }.toMutableList()

                    if (eligibleForPair.isEmpty()) {
                        break
                    }

                    // Sắp xếp ưu tiên: Ai làm ít ngày trong tuần hơn, hoặc tổng ca ít hơn thì đẩy lên trước
                    eligibleForPair.sortWith(compareBy<ManagerStaffItem> { staff ->
                        val weekKey = "${year}_${weekYear}_${staff.id}"
                        weeklyWorkMap[weekKey] ?: 0
                    }.thenBy { staff ->
                        overallStaffCount[staff.id] ?: 0
                    })

                    val chosenStaff = eligibleForPair.first()
                    val sId = chosenStaff.id
                    val weekKey = "${year}_${weekYear}_$sId"

                    // Gán nhân viên vào CẢ HAI CA trong cặp để đi liền nhau (Morning-Afternoon hoặc Evening-Night)
                    pair.shifts.forEach { sName ->
                        val key = "${dateStr}_$sName"
                        if (shiftOccupancy[key] == null) shiftOccupancy[key] = mutableListOf()
                        shiftOccupancy[key]!!.add(sId)

                        result.add(
                            StaffShiftDraft(
                                staffId = sId,
                                staffName = chosenStaff.fullName,
                                position = chosenStaff.position,
                                cinemaId = cinemaId,
                                phone = chosenStaff.phone,
                                workDate = dateStr,
                                shift = sName,
                                status = initialStatus,
                                isHoliday = isHoliday
                            )
                        )
                    }

                    val dailyKey = "${dateStr}_$sId"
                    if ((staffDailyCount[dailyKey] ?: 0) == 0) {
                        weeklyWorkMap[weekKey] = (weeklyWorkMap[weekKey] ?: 0) + 1
                    }
                    staffDailyCount[dailyKey] = (staffDailyCount[dailyKey] ?: 0) + 2
                    overallStaffCount[sId] = (overallStaffCount[sId] ?: 0) + 2
                }
            }
        }


        return result.sortedWith(compareBy<StaffShiftDraft> { it.workDate }.thenBy { it.shift })
    }
}