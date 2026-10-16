package com.example.androidApp.services

import com.example.androidApp.dtos.MovieDTO
import com.example.androidApp.dtos.MovieUpdateDTO
import com.example.androidApp.dtos.isEntity
import com.example.androidApp.dtos.toEntity
import com.example.androidApp.models.Movie
import com.example.androidApp.repositories.CateRepository
import com.example.androidApp.repositories.GenreRepo
import com.example.androidApp.repositories.MovieRepository
import com.example.androidApp.repositories.RateRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class MovieService(
    private val movieRepository: MovieRepository,
    private val cateRepository: CateRepository,
    private val genreRepo: GenreRepo,
    private val rateRepository: RateRepository
) {
    fun getAllMovies(): List<Movie> {
        val today = LocalDate.now()
        return movieRepository.findAll()
            .filter { movie ->
                !movie.deleted && (movie.endDate?.let { !it.isBefore(today) } ?: true)
            }
            .map { movie ->
                movie.avg = rateRepository.findAverageRatingByMovieId(movie.id) ?: 0.0
                movie
            }
    }

    fun getMoviesByCate(id: Int): List<Movie> {
        return movieRepository.getMoviesByCateId(id)
            .filterNot { it.deleted }
            .ifEmpty { emptyList() }

    }

    fun getMovieById(id: Int): Movie? {
        return movieRepository.findById(id).orElse(null)
    }

    fun delete(id: Int): Boolean {
        val movie = movieRepository.findById(id)
            .orElseThrow { Exception("can not found movie") }
        movie.deleted = true
        movieRepository.save(movie)
        return true
    }

    fun update(id: Int, dto: MovieUpdateDTO): Boolean {
        val movie = movieRepository.findById(id)
            .orElseThrow { Exception("can not found movie") }
        movie.apply {
            dto.title?.let { title = it }
            dto.des?.let { des = it }
            dto.cast?.let { cast = it }
            dto.duration?.let { duration = it }
            dto.director?.let { director = it }
//            dto.genreId?.let { genreId = it }

        }
        movieRepository.save(movie)
        return true
    }

    fun createMovie(movie: MovieDTO): Boolean {
        val cateCheck = movie.category?.let {
            cateRepository.findById(it.id).orElseThrow { Exception("Category does not exist") }
        } ?: throw IllegalArgumentException("Category must not be null")

        val genreCheck = movie.genre?.let {
            genreRepo.findById(it.id).orElseThrow { Exception("Genre does not exist") }
        } ?: throw IllegalArgumentException("Genre must not be null")

        return if (movieRepository.existsMovieByTitle(movie.title)) {
            false
        } else {
            movieRepository.save(
                Movie().apply {
                    title = movie.title
                    des = movie.des
                    duration = movie.duration
                    poster = movie.poster
                    cast = movie.cast
                    trailer = movie.trailer
                    director = movie.director
                    startDate = movie.startDate
                    endDate = movie.endDate
                    genre = genreCheck
                    cate = cateCheck
                    deleted = false
                }
            )
            true
        }
    }

    fun search(query: String): List<Movie> {
        return movieRepository.findByTitleContainingIgnoreCaseOrDirectorContainingIgnoreCaseOrCastContainingIgnoreCase(
            query,
            query,
            query
        )

    }

}

data class MovieSearch(
    val id: Int,
    val title: String,
    val cast: String,
    val poster: String
)