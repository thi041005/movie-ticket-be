package com.example.androidApp.controllers.user

import com.example.androidApp.services.BookingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("sepay")
@RestController
class WebhookController(
    private val bookingService: BookingService
) {
    @PostMapping("/webhook")
    fun receiveWebhook(@RequestBody webhookPayload: WebhookPayload): ResponseEntity<Any> {
        return try {
            println("Webhook nhận được: $webhookPayload")
            ResponseEntity.ok(bookingService.processPayment(webhookPayload))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        } catch (ex: Exception) {
            ex.printStackTrace() // in chi tiết lỗi
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (ex.message ?: "Unknown error")))
        }
    }
}