package com.example.androidApp.dtos

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.LocalTime

data class ScheduleMovieRequestDTO(
    val movieId: Int,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val fromDate: LocalDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val toDate: LocalDate,

    @JsonFormat(pattern = "HH:mm")
    val dailyStart: LocalTime,

    @JsonFormat(pattern = "HH:mm")
    val dailyEnd: LocalTime
)