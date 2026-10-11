package com.example.androidApp.controllers.user

import com.example.androidApp.models.Showtime
import com.example.androidApp.repositories.ShowtimeRepository
import com.example.androidApp.services.ShowtimeService
import org.aspectj.apache.bcel.Repository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RestController
@RequestMapping("/user")
class ShowtimeController(
    private val showtimeRepository: ShowtimeRepository,
    private val showtimeService: ShowtimeService
) {

    @GetMapping("/showtime/dates")
    fun getUpComingDate(@RequestParam movieId: Int): ResponseEntity<Map<String, Any>> {
        println("Received request for movieId: $movieId") // Debug log
        val dates = showtimeService.getUpComingShowtimeDates(movieId)
        return ResponseEntity.ok(mapOf("movieId" to movieId, "availDate" to dates))
    }
    @GetMapping("/showtime/{movieId}/{date}")
    fun getShowtime(@PathVariable movieId: Int, @PathVariable date: String): ResponseEntity<List<Showtime>> {
        val localDate = LocalDate.parse(date)
        val startOfDay = localDate.atStartOfDay()
        val endOfDay = localDate.atTime(LocalTime.MAX)

        val responseEntity = showtimeService.getShowtimesByMovieAndDate(movieId, startOfDay, endOfDay)
        val showtimes: List<Showtime> = responseEntity.body
            ?.filter { it.startTime.isAfter(LocalDateTime.now()) }?: emptyList()

        return ResponseEntity.ok(showtimes)
    }

    @GetMapping("/showtime/{showtimeId}")
    fun getShowtimeById(@PathVariable showtimeId: Int): ResponseEntity<Showtime>{
        val showtime = showtimeService.getShowtimeById(showtimeId)
        return ResponseEntity.ok(showtime)
    }
}
