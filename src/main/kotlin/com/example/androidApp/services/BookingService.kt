package com.example.androidApp.services

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.example.androidApp.controllers.user.WebhookPayload
import com.example.androidApp.dtos.BookingRequest
import com.example.androidApp.dtos.BookingResponse
import com.example.androidApp.models.Booking
import com.example.androidApp.models.BookingSeat
import com.example.androidApp.models.Ticket
import com.example.androidApp.repositories.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.LocalDateTime
import com.google.zxing.qrcode.QRCodeWriter
import jakarta.transaction.Transactional
import org.apache.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.imageio.ImageIO

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val showtimeRepository: ShowtimeRepository,
    private val repository: BookingRepository,
    private val seatRepository: SeatRepository,
    private val bookingSeatRepository: BookingSeatRepository,
    private val ticketRepository: TicketRepository,
    private val cloudinary: Cloudinary
) {
    fun Booking.isExpired(): Boolean {
        return this.expiresAt?.isBefore(LocalDateTime.now()) ?: true
    }

    fun cancelBooking(bookingId: Int, userId: Int): Boolean {
        val booking = bookingRepository.findById(bookingId)
            .orElseThrow { Exception("Không tim thấy booking") }

        return if (booking.status==0) {
            bookingRepository.delete(booking)
            println("Hủy thanh toán thành công")
            true
        } else {
            println("Hủy thanh toán thất bại ")
            false
        }
    }

    fun generateVietQRUrl(total: Double, info: String): String {
        val accNumb = "431001060003636"
        val base = "http://img.vietqr.io/image"
        val bankCode = "BAB"
        return "$base/$bankCode-$accNumb-print.png?amount=$total&addInfo=$info"
    }

    fun createBooking(request: BookingRequest): BookingResponse {
        val user = userRepository.findById(request.userId)
            .orElseThrow { Exception("không tìm thấy người dùng") }
        val showtime = showtimeRepository.findById(request.showtimeId)
            .orElseThrow { Exception("không tìm thấy người dùng") }
        val bookingSeats = request.seatIds.map { seatId ->
            val seat = seatRepository.findById(seatId.toInt())
                .orElseThrow { Exception(" không tìm thấy ghế $seatId") }
            BookingSeat(
                seat = seat,
                showtime = showtime,
                booking = null,
                price = seat.seatType.price
            )
        }
        val booking = Booking(
            user = user,
            showtime = showtime,
            status = 0,
            total = request.total,
            qrUrl = "",
            bookingSeats = bookingSeats,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        bookingSeats.forEach { it.booking = booking }

// Lưu lần 1 để có booking.id
        val savedBooking = bookingRepository.saveAndFlush(booking)

// Bây giờ đã có id → sinh QR code
        savedBooking.qrUrl = generateVietQRUrl(request.total, "${savedBooking.id}")

// Lưu lần 2 để cập nhật qrUrl
        val finalBooking = bookingRepository.save(savedBooking)

        return BookingResponse("Đã tạo thanh toán thành công", bookingId = finalBooking.id, qrUrl = finalBooking.qrUrl)
    }

    fun genQRCode(text: String): ByteArray {
        val qrCode = QRCodeWriter()
        val hints = mapOf(EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
            )
        val bitMatrix: BitMatrix = qrCode.encode(text, BarcodeFormat.QR_CODE, 300, 300, hints)

        val width = bitMatrix.width
        val height = bitMatrix.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_BGR)
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, if (bitMatrix[x, y]) 0x000000 else 0xffffff)
            }
        }
        val byteArrOutputStream = ByteArrayOutputStream()
        try {
            ImageIO.write(image, "PNG", byteArrOutputStream)

        } catch (e: Exception) {
            throw RuntimeException(" Không thể ghi dữ liệu hình ảnh vào byte array ${e.message}")
        }
        return byteArrOutputStream.toByteArray()
    }

    @Transactional
    fun processPayment(webhookPayload: WebhookPayload): Boolean {
        val booking = bookingRepository.findById(webhookPayload.content.toInt())
            .orElseThrow { Exception("Không tồn tại mã đẳt vé nào phù hợp") }
        val user = booking.user
        booking.status = 1
        booking.paymentTime = webhookPayload.transactionDate
        val bookingSeats = bookingSeatRepository.findByBookingId(booking.id)
        bookingSeats.forEach { bookingSeat ->
            bookingSeat.seat.state = 3
        }
        seatRepository.saveAll(bookingSeats.map { it.seat })
        bookingRepository.save(booking)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val payTime = LocalDateTime.parse(webhookPayload.transactionDate, formatter)

        val auditoriumName = booking.showtime.auditorium.name
        val seatSum = bookingSeats.joinToString(" ") { "${it.seat.row}${it.seat.col}" }
        val qrContent = " Mã đặt vé:${booking.id}|Ghế: $seatSum|Phong: $auditoriumName"

        val qrBytes = genQRCode(qrContent)
        val base64Image = Base64.getEncoder().encodeToString(qrBytes)
        val dataUri = "data:image/png;base64,$base64Image"

        val uploadResult = cloudinary.uploader().upload(
            dataUri,
            mapOf(
                "folder" to "qrcodes",
                "public_id" to "qr_${booking.id}",
                "overwrite" to true,
                "resource_type" to "image"
            )
        )

        val imageUrl = uploadResult["secure_url"] as String

        ticketRepository.save(
            Ticket(
                booking = booking,
                user = user,
                issuedAt = payTime,
                qrUrl = imageUrl,
                used = false
            )
        )
        return true
    }

    fun getPaidSeats(showtimeId: Long): Int{
        return bookingSeatRepository.countPaidSeatsByShowtimeId(showtimeId)
    }

//    fun getAllTicket(userId: Int):
}

data class SeatInfo(
    val row: String,
    val col: Int
)

