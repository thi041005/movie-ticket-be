package com.example.androidApp.controllers

import com.example.androidApp.dtos.*
import com.example.androidApp.models.User
import com.example.androidApp.models.VerificationToken
import com.example.androidApp.services.EmailService
import com.example.androidApp.services.UserService
import com.example.androidApp.services.VerificationTokenService
import io.jsonwebtoken.Jwts
import org.springframework.http.ResponseEntity
import java.util.*
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import javax.crypto.SecretKey


@RestController
@RequestMapping("auth")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:4200"])

class AuthController(
    private val userService: UserService,
    private val verificationTokenService: VerificationTokenService,
    private val emailService: EmailService
) {

    private val secretKey: SecretKey = Keys.hmacShaKeyFor("your-very-secret-key-your-secret-key-here".toByteArray())



    @PostMapping("signup")
    fun signup(@RequestBody body: RegisterDTO): ResponseEntity<Any> {
        val error = when {
            userService.existsByEmail(body.email) -> "Email đã được sử dụng!"
            userService.existsByPhone(body.phone) -> "Số điện thoại đã được sử dụng!"
            else -> null
        }

        return error?.let { ResponseEntity.badRequest().body(Message(it)) }
            ?: run {

                val user = User().apply {
                    name = body.name
                    email = body.email
                    phone = body.phone
                    password = body.password
                }

                userService.save(user)

                val token = VerificationToken(
                    token = UUID.randomUUID().toString(),
                    user = user,
                    expiryDate = LocalDateTime.now().plusMinutes(10)
                )
                verificationTokenService.save(token)

                emailService.sendVerificationEmail(user.email, token.token)

                ResponseEntity.ok(Message("Thực hiện xác thực tại email của bạn!"))
            }
    }

    @PostMapping("/login")
    fun login(@RequestBody body: LoginDTO, response: HttpServletResponse): ResponseEntity<Any> {
        val user = this.userService.findByEmail(body.email)
            ?: return ResponseEntity.badRequest().body(Message("Không tồn tại người dùng!"))
        if (!user.checkPassword(body.password)) {
            return ResponseEntity.badRequest().body(Message("Mật khẩu không đúng "))
        }
        if (!user.enabled)
            return ResponseEntity.badRequest().body(Message("Không tồn tại người dùng"))
        val role = user.role

        val jwt = Jwts.builder()
            .setSubject(user.id.toString())
            .claim("email", user.email)
            .claim("role", "ROLE_$role")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 20 * 60 * 24))
            .signWith(secretKey)
            .compact()

        var cookie = Cookie("jwt", jwt)
        cookie.isHttpOnly = true

        response.addCookie(cookie)

//        val customToken = FirebaseAuth.getInstance().createCustomToken(user.id.toString())

        return ResponseEntity.ok(AuthDTO(jwt, user.id, role))

    }



    @GetMapping("/verify")
    fun verify(@RequestParam token: String): ResponseEntity<Any> {
        val vToken = verificationTokenService.findByToken(token)
            ?: return ResponseEntity.badRequest().body("Token không hợp lệ")

        // Debug log để kiểm tra token và expiry date
        println("Token : $vToken, Expiry: ${vToken.expiryDate}")

        if (vToken.expiryDate.isBefore(LocalDateTime.now())) {
            userService.delete(vToken.user)
            return ResponseEntity.badRequest().body("Token đã hết hạn, vui lòng đăng ký lại để nhận email mới")
        }

        val user = vToken.user
        if (user.enabled) {
            return ResponseEntity.badRequest().body("Tài khoản đã được xác thực trước đó")
        }

        user.enabled = true
        userService.save(user)

        verificationTokenService.delete(vToken)
        return ResponseEntity.ok("Xác minh thành công! Tài khoản đã được kích hoạt.")
    }

    @PostMapping("/forgot-password")
    fun sendOtp(@RequestBody request: ForgotPasswordRequest): ResponseEntity<String>{
        return try {
            userService.sendOtp(request.email)
            ResponseEntity.ok("Mã OTP đã được gửi, hãy kiểm tra email của bạn.")
        }catch (e: Exception){
            ResponseEntity.status(500).body("Loi ${e.message}")
        }
    }
    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody request: ResetPasswordRequest):  ResponseEntity<String>{
        return try {
            userService.resetPassword(request)
            ResponseEntity.ok("Dat lai mat khau thanh cong")
        }catch (e: Exception){
            ResponseEntity.badRequest().body("Loi ${e.message}")
        }
    }


}

data class ForgotPasswordRequest(
    val email: String
)
data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)
