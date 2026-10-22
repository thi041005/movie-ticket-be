package com.example.androidApp.services

import com.example.androidApp.models.Showtime
import com.example.androidApp.repositories.AuditoriumRepository
import com.example.androidApp.repositories.MovieRepository
import com.example.androidApp.repositories.ShowtimeRepository
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ScheduleResult(
    val movieId: Int,
    val createdCount: Int,
    val skippedRoomDays: Int
)

@Service
class ShowtimeService(
    private val movieRepository: MovieRepository,
    private val auditoriumRepository: AuditoriumRepository,
    private val showtimeRepository: ShowtimeRepository
) {
    private val BREAK_TIME_MINUTES = 15L

    @Transactional
    fun scheduleByMovie(
        movieId: Int,
        fromDate: LocalDate,
        toDate: LocalDate,
        dailyStart: LocalTime,
        dailyEnd: LocalTime
    ): ScheduleResult {
        validateScheduleRequest(
            fromDate, toDate, dailyStart, dailyEnd
        )

        val movie = movieRepository.findById(movieId)
            .orElseThrow {
                IllegalArgumentException("Không tồn tại phim")
            }

        require(movie.duration > 0) {
            "Thời lượng phim không hợp lệ"
        }

        val auditoriums = auditoriumRepository.findAll()

        require(auditoriums.isNotEmpty()) {
            "Không có phòng chiếu"
        }

        var createdCount = 0
        var skippedRoomDays = 0

        for (date in fromDate.datesUntil(toDate.plusDays(1))) {
            val dayStart = LocalDateTime.of(date, dailyStart)
            val dayEnd = LocalDateTime.of(date, dailyEnd)

            for (auditorium in auditoriums) {
                // Lấy các suất có thể ảnh hưởng đến ngày đang lập lịch.
                val existing = showtimeRepository
                    .findByAuditorium(auditorium)
                    .filter { showtime ->
                        showtime.startTime < dayEnd &&
                                showtime.endTime > dayStart
                    }
                    .toMutableList()

                var cursor = dayStart
                var createdInRoom = 0

                while (true) {
                    val movieEnd = cursor.plusMinutes(
                        movie.duration.toLong()
                    )

                    // Suất chiếu phải kết thúc trong khung giờ hoạt động.
                    if (movieEnd.isAfter(dayEnd)) break

                    val conflicts = existing.filter { showtime ->
                        hasConflict(
                            startTime = cursor,
                            endTime = movieEnd,
                            existing = showtime
                        )
                    }

                    if (conflicts.isNotEmpty()) {
                        // Thử lại sau suất chiếu gây xung đột gần nhất
                        // theo thời điểm kết thúc.
                        cursor = conflicts.maxOf { it.endTime }
                            .plusMinutes(BREAK_TIME_MINUTES)
                        continue
                    }

                    val showtime = Showtime(
                        movie = movie,
                        auditorium = auditorium,
                        startTime = cursor,
                        endTime = movieEnd
                    )

                    val saved = showtimeRepository.save(showtime)

                    // Cập nhật ngay để các lượt tiếp theo kiểm tra
                    // cả suất chiếu vừa tạo.
                    existing.add(saved)
                    createdCount++
                    createdInRoom++

                    cursor = movieEnd.plusMinutes(BREAK_TIME_MINUTES)
                }

                if (createdInRoom == 0) {
                    skippedRoomDays++
                }
            }
        }

        return ScheduleResult(
            movieId = movieId,
            createdCount = createdCount,
            skippedRoomDays = skippedRoomDays
        )
    }

    /**
     * Hai suất chiếu phải cách nhau ít nhất 15 phút.
     * endTime là thời điểm kết thúc phim thực tế.
     */
    private fun hasConflict(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        existing: Showtime
    ): Boolean {
        val existingAvailableFrom = existing.endTime
            .plusMinutes(BREAK_TIME_MINUTES)

        val newAvailableFrom = endTime
            .plusMinutes(BREAK_TIME_MINUTES)

        return startTime < existingAvailableFrom &&
                newAvailableFrom > existing.startTime
    }

    private fun validateScheduleRequest(
        fromDate: LocalDate,
        toDate: LocalDate,
        dailyStart: LocalTime,
        dailyEnd: LocalTime
    ) {
        require(!fromDate.isAfter(toDate)) {
            "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc"
        }

        require(dailyStart.isBefore(dailyEnd)) {
            "Giờ bắt đầu phải trước giờ kết thúc"
        }
    }

    fun getShowtimesByMovieAndDate(
        movieId: Int,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): ResponseEntity<List<Showtime>> {
        val showtimes = showtimeRepository.findByMovieIdAndStartTimeBetween(movieId, startTime, endTime)
        return ResponseEntity.ok(showtimes)

    }


    fun getUpComingShowtimeDates(movieId: Int): List<LocalDate> {
        val today = LocalDate.now()
        val threeDaysLater = today.plusDays(4)

        return showtimeRepository.findUpcomingShowtimeDates(movieId, today, threeDaysLater)
            .map { it.toLocalDate() }
    }

    fun getShowtimeById(showtimeId: Int): Showtime? {
        return showtimeRepository.findById(showtimeId).orElse(null)
    }

}