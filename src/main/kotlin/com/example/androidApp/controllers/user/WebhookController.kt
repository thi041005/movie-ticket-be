package com.example.androidApp.controllers.user

import com.example.androidApp.services.BookingService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest

@RestController
@RequestMapping("/sepay")
class WebhookController(
    private val bookingService: BookingService,
    @Value("\${sepay.api-key}") private val apiKey: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/webhook")
    fun receiveWebhook(
        @RequestHeader("Authorization", required = false) auth: String?,
        @RequestBody webhookPayload: SepayWebhookPayload
    ): ResponseEntity<Map<String, Any>> {

        // SePay gửi header: Authorization: Apikey <key>
        val expected = "Apikey $apiKey".toByteArray()
        if (auth == null || !MessageDigest.isEqual(auth.toByteArray(), expected)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf("success" to false, "error" to "Unauthorized")
            )
        }

        return try {
            val processed = bookingService.processPayment(webhookPayload)
            ResponseEntity.ok(
                mapOf(
                    "success" to true,
                    "message" to if (processed) "Payment processed successfully" else "Received, no matching booking"
                )
            )
        } catch (e: IllegalStateException) {
            log.warn("Sepay {} bad request: {}", webhookPayload.id, e.message)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                mapOf("success" to false, "error" to (e.message ?: "Bad request"))
            )
        } catch (ex: Exception) {
            log.error("Sepay {} lỗi hệ thống", webhookPayload.id, ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("success" to false, "error" to "Internal server error")    // không trả ex.message ra ngoài
            )
        }
    }
}