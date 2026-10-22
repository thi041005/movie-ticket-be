package com.example.androidApp.repositories

import com.example.androidApp.models.Booking
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository

interface BookingRepository : JpaRepository<Booking, Int> {
    fun findByStatusAndExpiresAtBefore(state: Int, expire: LocalDateTime): List<Booking>
    fun findByStatus(status: Int): List<Booking>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    fun findByIdForUpdate(@Param("id") id: Int): Booking?

    @Query(
        """SELECT DATE(b.createdAt) as date, SUM(b.total) as total 
            FROM Booking b
            WHERE b.status = 1 AND b.createdAt BETWEEN :start AND :end
            GROUP BY DATE(b.createdAt)
            ORDER BY DATE(b.createdAt) ASC 
         """
    )
    fun getRevenueByDay(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,

    ): List<Array<Any>>


    @Query("""
    SELECT m.id, m.title, SUM(b.total)
    FROM Movie m
    JOIN Showtime s ON s.movie.id = m.id
    JOIN Booking b ON b.showtime.id = s.id
    WHERE b.status = 1
    GROUP BY m.id, m.title
    ORDER BY SUM(b.total) DESC
""")
    fun getRevenueByMovies(): List<Array<Any>>


}
data class RevenueDTO(
    val date: String,
    val total: Double
)
data class MovieRevenueDTO(
    val movieId: Int,
    val title: String,
    val total: Double
)
