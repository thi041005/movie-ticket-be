package com.example.androidApp.services

import com.cloudinary.Cloudinary
import com.example.androidApp.controllers.user.SepayWebhookPayload
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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.imageio.ImageIO

@Service
class BookingService(
    private val bookingRepository: BookingRepository,
    private val userRepository: UserRepository,
    private val showtimeRepository: ShowtimeRepository,
    private val seatRepository: SeatRepository,
    private val bookingSeatRepository: BookingSeatRepository,
    private val ticketRepository: TicketRepository,
    private val cloudinary: Cloudinary,
    private val bookingCode: BookingCode,
    @Value("\${payment.bank-code}") private val bankCode: String,
    @Value("\${payment.account-number}") private val accountNumber: String,

    ) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun Booking.isExpired(): Boolean {
        return this.expiresAt?.isBefore(LocalDateTime.now()) ?: true
    }

    fun cancelBooking(bookingId: Int, userId: Int): Boolean {
        val booking = bookingRepository.findById(bookingId).orElseThrow { Exception("Không tim thấy booking") }
        if (booking.user.id != userId) throw Exception("Không tim thấy booking")

        return if (booking.status == 0) {
            bookingRepository.delete(booking)
            println("Hủy thanh toán thành công")
            true
        } else {
            println("Hủy thanh toán thất bại ")
            false
        }
    }

    fun generateVietQRUrl(total: Double, info: String): String {
        val base = "https://img.vietqr.io/image"
        val addInfo = URLEncoder.encode(info, "UTF-8").replace("+", "%20")   // dấu cách thành %20 cho chắc
        return "$base/$bankCode-$accountNumber-print.png?amount=${total.toLong()}&addInfo=$addInfo"
    }

    @Transactional
    fun createBooking(request: BookingRequest): BookingResponse {
        val user = userRepository.findById(request.userId).orElseThrow { Exception("không tìm thấy người dùng") }

        // Khóa suất chiếu: các yêu cầu đặt cùng suất chạy tuần tự nên không thể đặt trùng ghế
        val showtime =
            showtimeRepository.findByIdForUpdate(request.showtimeId) ?: throw Exception("không tìm thấy suất chiếu")

        val seatIds = request.seatIds.map { it.toInt() }.distinct()

        if (bookingSeatRepository.findTakenSeatIds(request.showtimeId, seatIds, LocalDateTime.now())
                .isNotEmpty()
        ) throw Exception("Ghế đã có người đặt")

        val bookingSeats = seatIds.map { seatId ->
            val seat = seatRepository.findById(seatId).orElseThrow { Exception(" không tìm thấy ghế $seatId") }
            BookingSeat(seat = seat, showtime = showtime, booking = null, price = seat.seatType.price)
        }

        val total = bookingSeats.sumOf { it.price.toDouble() }    // server tự tính, bỏ qua request.total

        val booking = Booking(
            user = user,
            showtime = showtime,
            status = 0,
            total = total,
            qrUrl = "",
            bookingSeats = bookingSeats,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        bookingSeats.forEach { it.booking = booking }

        // trong createBooking, thay 2 dòng tạo qrUrl cũ
        val savedBooking = bookingRepository.saveAndFlush(booking)

        val transferContent = bookingCode.of(savedBooking.id)

        savedBooking.qrUrl = generateVietQRUrl(total, transferContent)

        val finalBooking = bookingRepository.save(savedBooking)

        return BookingResponse("Đã tạo thanh toán thành công", bookingId = finalBooking.id, qrUrl = finalBooking.qrUrl)
    }

    fun genQRCode(text: String): ByteArray {
        val qrCode = QRCodeWriter()
        val hints = mapOf(
            EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8"
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
    fun processPayment(p: SepayWebhookPayload): Boolean {
        if (p.transferType != null && p.transferType != "in") {
            log.info("Sepay {}: bỏ qua giao dịch transferType={}", p.id, p.transferType)
            return false
        }
        val bookingId = bookingCode.parse(p.code)
            ?: bookingCode.parse(p.content)
        if (bookingId == null) {
            log.warn("Sepay {}: không có mã đặt vé trong '{}'", p.id, p.content)
            return false
        }

        // Khóa dòng đơn: hai webhook trùng đến cùng lúc sẽ xử lý tuần tự, cái sau thấy status = 1
        val booking = bookingRepository.findByIdForUpdate(bookingId)
        if (booking == null) {
            log.warn("Sepay {}: không tồn tại booking {}", p.id, bookingId)
            return false
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val payTime = p.transactionDate?.let { runCatching { LocalDateTime.parse(it, formatter) }.getOrNull() }
            ?: LocalDateTime.now()
        val bookingSeats = bookingSeatRepository.findByBookingId(booking.id)

        // Webhook gọi lại: không xử lý lần hai, nhưng nếu lần trước tạo vé lỗi thì tạo bù
        if (booking.status == 1) {
            if (!ticketRepository.existsByBookingId(booking.id)) issueTicket(booking, bookingSeats, payTime)
            return true
        }
        if (p.transferAmount < booking.total) {
            log.warn("Booking {} trả thiếu: {} < {}", bookingId, p.transferAmount, booking.total)
            return false
        }
        if (booking.isExpired()) {
            log.warn("Booking {} đã hết hạn giữ ghế, cần đối soát/hoàn tiền", bookingId)
            return false
        }

        booking.status = 1
        booking.paymentTime = p.transactionDate ?: payTime.format(formatter)
        bookingSeats.forEach { bookingSeat ->
            bookingSeat.seat.state = 3
        }
        seatRepository.saveAll(bookingSeats.map { it.seat })
        bookingRepository.save(booking)

        issueTicket(booking, bookingSeats, payTime)
        return true
    }

    // Lỗi tạo vé (Cloudinary...) chỉ ghi log, không làm mất việc xác nhận thanh toán
    private fun issueTicket(booking: Booking, bookingSeats: List<BookingSeat>, payTime: LocalDateTime) {
        try {
            val seatSum = bookingSeats.joinToString(" ") { "${it.seat.row}${it.seat.col}" }
            val qrContent = " Mã đặt vé:${booking.id}|Ghế: $seatSum|Phong: ${booking.showtime.auditorium.name}"

            val dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(genQRCode(qrContent))
            val uploadResult = cloudinary.uploader().upload(
                dataUri, mapOf(
                    "folder" to "qrcodes",
                    "public_id" to "qr_${booking.id}",
                    "overwrite" to true,
                    "resource_type" to "image"
                )
            )
            ticketRepository.save(
                Ticket(
                    booking = booking,
                    user = booking.user,
                    issuedAt = payTime,
                    qrUrl = uploadResult["secure_url"] as String,
                    used = false
                )
            )
        } catch (e: Exception) {
            log.error("Booking {} đã thanh toán nhưng tạo vé lỗi, webhook gọi lại sẽ tạo bù", booking.id, e)
        }
    }

    fun getPaidSeats(showtimeId: Long): Int {
        return bookingSeatRepository.countPaidSeatsByShowtimeId(showtimeId)
    }

//    fun getAllTicket(userId: Int):
}

data class SeatInfo(
    val row: String, val col: Int
)
@Component
class BookingCode(
    @Value("\${payment.content-prefix:SEVQR}")
    private val prefix: String
) {
    init {
        require(prefix.matches(Regex("[A-Za-z]{2,5}"))) {
            "payment.content-prefix phải có từ 2 đến 5 chữ cái"
        }
    }

    private val regex = Regex(
        "${Regex.escape(prefix)}(\\d{1,12})",
        RegexOption.IGNORE_CASE
    )

    fun of(bookingId: Int): String = "$prefix$bookingId"

    fun parse(content: String?): Int? =
        content?.let { regex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
}