package com.example.androidApp.repositories

import com.example.androidApp.models.Auditorium
import com.example.androidApp.models.Showtime
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface ShowtimeRepository: JpaRepository<Showtime, Int> {

    fun findByAuditoriumAndStartTimeBetween(
        auditorium: Auditorium,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<Showtime>

    fun findByMovieIdAndStartTimeBetween(movieId: Int, startTime: LocalDateTime, endTime: LocalDateTime): List<Showtime>

    @Query(value = "SELECT DISTINCT DATE(start_time) FROM showtime WHERE movie_id = :movieId AND start_time BETWEEN :start AND :end", nativeQuery = true)
    fun findUpcomingShowtimeDates(movieId: Int, start: LocalDate, end: LocalDate): List<java.sql.Date>

    fun findByAuditorium(auditorium: Auditorium): List<Showtime>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Showtime s WHERE s.id = :id")
    fun findByIdForUpdate(@Param("id") id: Int): Showtime?
}