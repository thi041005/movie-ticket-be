package com.example.androidApp.controllers.admin

import com.example.androidApp.repositories.MovieRevenueDTO
import com.example.androidApp.repositories.RevenueDTO
import com.example.androidApp.services.RevenueService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("admin/revenue")
class RevenueController(private val revenueService: RevenueService) {
    @GetMapping("/daily/{from}/{to}")
    fun getDailyRevenue(
        @PathVariable from: String,
        @PathVariable to: String
    ): ResponseEntity<List<RevenueDTO>>{
        val fromDate = LocalDate.parse(from)
        val toDate = LocalDate.parse(to)
        return ResponseEntity.ok(revenueService.getDailyRevenue(fromDate, toDate))
    }
    @GetMapping("/revenue-movie")
    fun getRevenueByMovies(): ResponseEntity<List<MovieRevenueDTO>>{
        return ResponseEntity.ok(revenueService.getMovieRevenue())
    }
}