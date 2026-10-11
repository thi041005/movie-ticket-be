package com.example.androidApp.controllers

import com.example.androidApp.repositories.BookingSeatRepository
import com.example.androidApp.repositories.MovieRevenueDTO
import com.example.androidApp.services.RevenueService
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

    @PostMapping("/schedule")
    fun schedule(@RequestBody req: ScheduleRequest): ResponseEntity<String> {
        return try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val parsedFromDate = LocalDate.parse(req.fromDate, dateFormatter)
            val parsedToDate = LocalDate.parse(req.toDate, dateFormatter)
            val parsedStartTime = LocalTime.parse(req.dailyStart, timeFormatter)
            val parsedEndTime = LocalTime.parse(req.dailyEnd, timeFormatter)
            showtimeService.scheduleShowtimes(parsedFromDate, parsedToDate, parsedStartTime, parsedEndTime)
            ResponseEntity.ok(" Lên lịch chiếu thành công từ ${req.fromDate} đến ${req.toDate} trong khoảng ${req.dailyStart} - ${req.dailyEnd} mỗi ngày.")
        } catch (e: DateTimeParseException) {
            ResponseEntity.badRequest().body("Lỗi: Định dạng ngày giờ không hợp lệ. Sử dụng 'yyyy-MM-ddTHH:mm'")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Lỗi khi lên lịch: ${e.message}")
        }
    }

    @PostMapping("/schedule-movie")
    fun scheduleByMovie(@RequestBody req: ScheduleMovieRequest): ResponseEntity<String> {
        return try {
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val parsedFromDate = LocalDate.parse(req.fromDate, dateFormatter)
            val parsedToDate = LocalDate.parse(req.toDate, dateFormatter)
            val parsedStartTime = LocalTime.parse(req.dailyStart, timeFormatter)
            val parsedEndTime = LocalTime.parse(req.dailyEnd, timeFormatter)
            showtimeService.scheduleByMovie(req.movieId, parsedFromDate, parsedToDate, parsedStartTime, parsedEndTime)
            ResponseEntity.ok(" Lên lịch chiếu thành công từ ${req.fromDate} đến ${req.toDate} trong khoảng ${req.dailyStart} - ${req.dailyEnd} mỗi ngày.")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Lỗi khi lên lịch: ${e.message}")
        }
    }

    @GetMapping("/count-seat/{showtimeId}")
    fun getPaidSeats(@PathVariable showtimeId: Long): ResponseEntity<Int> {
        return ResponseEntity.ok(bookingSeatRepository.countPaidSeatsByShowtimeId(showtimeId))
    }
}

data class ScheduleRequest(
    val fromDate: String,
    val toDate: String,
    val dailyStart: String,
    val dailyEnd: String
)

data class ScheduleMovieRequest(
    val movieId: Int,
    val fromDate: String,
    val toDate: String,
    val dailyStart: String,
    val dailyEnd: String
)


data class ShowtimeDto(
    val movieTitle: String,
    val auditoriumName: String,
    val startTime: String,
    val endTime: String
)