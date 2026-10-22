package com.example.androidApp.controllers

import com.example.androidApp.dtos.ScheduleMovieRequestDTO
import com.example.androidApp.repositories.BookingSeatRepository
import com.example.androidApp.repositories.MovieRevenueDTO
import com.example.androidApp.services.RevenueService
import com.example.androidApp.services.ScheduleResult
import com.example.androidApp.services.ShowtimeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("public")
class SchedulingController(
    private val showtimeService: ShowtimeService,
    private val bookingSeatRepository: BookingSeatRepository,
    private val revenueService: RevenueService
) {
    @GetMapping("/revenue-movie")
    fun getRevenueByMovies(): ResponseEntity<List<MovieRevenueDTO>>{
        return ResponseEntity.ok(revenueService.getMovieRevenue())
    }

    @PostMapping("/schedule-movie")
    fun scheduleByMovie(
        @RequestBody req: ScheduleMovieRequestDTO
    ): ResponseEntity<ScheduleResult> {
        return ResponseEntity.ok(
            showtimeService.scheduleByMovie(
                movieId = req.movieId,
                fromDate = req.fromDate,
                toDate = req.toDate,
                dailyStart = req.dailyStart,
                dailyEnd = req.dailyEnd
            )
        )
    }

    @GetMapping("/count-seat/{showtimeId}")
    fun getPaidSeats(@PathVariable showtimeId: Long): ResponseEntity<Int> {
        return ResponseEntity.ok(bookingSeatRepository.countPaidSeatsByShowtimeId(showtimeId))
    }
}



data class ScheduleMovieRequest(
    val movieId: Int,
    val fromDate: String,
    val toDate: String,
    val dailyStart: String,
    val dailyEnd: String
)


