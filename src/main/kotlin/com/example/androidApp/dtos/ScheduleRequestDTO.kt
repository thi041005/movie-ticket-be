package com.example.androidApp.dtos

import java.time.LocalDate
import java.time.LocalTime

data class ScheduleRequest(
    val movieId: Int,
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val dailyStart: LocalTime,
    val dailyEnd: LocalTime
)