package com.example.androidApp.services

import com.example.androidApp.repositories.BookingRepository
import com.example.androidApp.repositories.BookingSeatRepository
import com.example.androidApp.repositories.TicketRepository
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val bookingRepository: BookingRepository,
    private val bookingSeatRepository: BookingSeatRepository,
    private val userService: UserService
) {
    fun getTicket(bookingId: Int): TicketDTO {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { Exception("no exist booking") }
        val ticket = ticketRepository.findByBookingId(bookingId)
            ?: throw Exception("chua thanh toan cho ${bookingId} ")
        val bookingSeats = bookingSeatRepository.findByBookingId(bookingId)
        val seatInfo =
            bookingSeats.map { bookingSeat ->
                SeatInfo(
                    row = bookingSeat.seat.row,
                    col = bookingSeat.seat.col

                )
            }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val formattedPayTime = ticket.issuedAt.format(formatter)

        return TicketDTO(
            audName = booking.showtime.auditorium.name,
            movieName = booking.showtime.movie.title,
            ticketId = ticket.id,
            payTime = formattedPayTime, // <-- CHUYỂN THÀNH STRING RÕ RÀNG
            total = booking.total,
            qr = ticket.qrUrl,
            seat = seatInfo
        )


    }

    fun getAllTicket(id: Int): List<TicketDTO> {
        val tickets = ticketRepository.findByUserIdOrderByIssuedAtDesc(id)
        if (tickets.isEmpty()) {
            throw Exception("Không tìm thấy vé nào cho người dùng có ID: $id")
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        return tickets.map {
            val formattedPayTime = it.issuedAt.format(formatter)
            val bookingId = it.booking.id
            val bookingSeats = bookingSeatRepository.findByBookingId(bookingId)
            val seatInfo =
                bookingSeats.map { bookingSeat ->
                    SeatInfo(
                        row = bookingSeat.seat.row,
                        col = bookingSeat.seat.col

                    )
                }
            TicketDTO(
                movieName = it.booking.showtime.movie.title,
                audName = it.booking.showtime.auditorium.name,
                ticketId = it.id,
                payTime = formattedPayTime,
                total = it.booking.total,
                qr = it.qrUrl,
                seatInfo
            )
        }
    }
}


data class TicketDTO(
    val movieName: String,
    val audName: String,
    val ticketId: Int,
    val payTime: String,
    val total: Double,
    val qr: String,
    val seat: List<SeatInfo>
)

