package com.example.androidApp.controllers.user

import com.example.androidApp.dtos.BookingRequest
import com.example.androidApp.dtos.BookingResponse
import com.example.androidApp.models.Booking
import com.example.androidApp.repositories.BookingRepository
import com.example.androidApp.services.BookingService
import com.example.app02.dto.SeatBookingRequest
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.io.encoding.Base64

@RestController
@RequestMapping("user")
class BookingController(
    private val bookingService: BookingService,
    private val bookingRepository: BookingRepository
) {

    @PostMapping("/book")
    fun bookSeat(@RequestBody request: BookingRequest): BookingResponse {
        return bookingService.createBooking(request)
    }

    @PostMapping("/cancel-book")
    fun cancelBook(@RequestBody request: CancelBookReq): ResponseEntity<String> {
        return if (bookingService.cancelBooking(request.bookingId, request.userId)) {
            ResponseEntity.ok("Hủy thanh toán thành công")
        } else
            ResponseEntity.badRequest().body("Hủy thanh toán thất bại")
    }

    @GetMapping("/booking/{id}/state")
    fun getBookingState(@PathVariable id: Int): ResponseEntity<Int>{
        val booking = bookingRepository.findById(id)
            .orElseThrow { Exception("Khong tim thay booking") }
        return ResponseEntity.ok(booking.status)
    }
}

data class CancelBookReq(
    val bookingId: Int,
    val userId: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SepayWebhookPayload(
    val id: Long,                         // bắt buộc: dùng chống xử lý trùng
    val transferAmount: Long,             // bắt buộc: Long thay vì Int cho an toàn
    val gateway: String? = null,
    val transactionDate: String? = null,  // "yyyy-MM-dd HH:mm:ss", giờ Việt Nam
    val accountNumber: String? = null,
    val code: String? = null,             // mã thanh toán SePay tự nhận diện theo prefix
    val content: String? = null,
    val transferType: String? = null,     // "in" hoặc "out"
    val accumulated: Long? = null,
    val subAccount: String? = null,
    val referenceCode: String? = null,
    val description: String? = null
)