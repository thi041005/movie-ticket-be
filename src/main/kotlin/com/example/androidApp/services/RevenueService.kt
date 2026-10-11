package com.example.androidApp.services

import com.example.androidApp.repositories.BookingRepository
import com.example.androidApp.repositories.MovieRevenueDTO
import com.example.androidApp.repositories.RevenueDTO
import org.springframework.stereotype.Service
import org.threeten.bp.LocalDateTime
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@Service
class RevenueService(private val bookingRepo: BookingRepository) {
    fun getDailyRevenue(from: LocalDate, to: LocalDate): List<RevenueDTO>{
        val startDate = from.atStartOfDay()
        val endDateTime = to.atTime(LocalTime.MAX)

        return bookingRepo.getRevenueByDay(startDate, endDateTime).map {
            val sqlDate = it[0] as java.sql.Date
            val date = sqlDate.toLocalDate()
            val total = it[1] as Double
            RevenueDTO(date.toString(), total)
        }

    }
    fun getMovieRevenue(): List<MovieRevenueDTO>{
        val rawResult = bookingRepo.getRevenueByMovies()
        return rawResult.map {
            MovieRevenueDTO(
                movieId = (it[0] as Int),
                title = it[1] as String,
                total = it[2] as Double
            )
        }
    }
}