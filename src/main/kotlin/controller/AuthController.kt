package es.unizar.webeng.hello.controller

import es.unizar.webeng.hello.model.UserAccount
import es.unizar.webeng.hello.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class AuthController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @GetMapping("/login")
    fun login() = "login"

    @GetMapping("/register")
    fun register() = "register"

    @PostMapping("/register")
    fun registerPost(
        @RequestParam username: String,
        @RequestParam password: String
    ): String {
        // Comprobamos si el usuario ya existe
        if (userRepository.findByUsername(username) != null) {
            return "redirect:/register?error" // Redirigimos con un aviso de error
        }

        // Encriptamos la contraseña y guardamos
        val newUser = UserAccount(
            username = username, // Ya es un String no nulo gracias al @RequestParam
            passwordHash = passwordEncoder.encode(password) ?: "" // Si el encoder devuelve null, ponemos cadena vacía
        )
        userRepository.save(newUser)

        // Si todo va bien, le mandamos al login para que entre
        return "redirect:/login?registered"
    }
}