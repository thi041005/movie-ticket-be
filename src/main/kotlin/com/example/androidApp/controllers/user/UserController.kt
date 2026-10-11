package com.example.androidApp.controllers.user

import com.example.androidApp.dtos.Message
import com.example.androidApp.dtos.UserDTO
import com.example.androidApp.models.User
import com.example.androidApp.services.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("user")
class UserController(
    private val userService: UserService
){
//    @GetMapping("/info")
//    fun user(): ResponseEntity<Any> {
//        // Lấy thông tin người dùng từ SecurityContextHolder
//        val authentication = SecurityContextHolder.getContext().authentication
//        val email = authentication?.principal as? String
//
//        if (email.isNullOrBlank()) {
//            return ResponseEntity.status(401).body(Message("Unauthenticated"))
//        }
//
//        // Lấy người dùng từ service của bạn
//        val user = userService.findByEmail(email)
//            ?: return ResponseEntity.status(404).body(Message("User not found"))
//
//        // Trả về thông tin người dùng
//        return ResponseEntity.ok(UserDTO(user.id, user.name, user.email, user.phone))
//    }
    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Int): ResponseEntity<User?>{
        if (id == null){
            return ResponseEntity.badRequest().body(null)
        }
        val user = userService.getById(id)
        return if (user!=null){
            ResponseEntity.ok(user)
        }else{
            ResponseEntity.notFound().build()
        }
    }
}