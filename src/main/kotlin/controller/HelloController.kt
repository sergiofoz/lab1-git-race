package es.unizar.webeng.hello.controller

import es.unizar.webeng.hello.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.time.LocalDateTime
import jakarta.servlet.http.HttpServletRequest

/**
 * Helper function to determine the appropriate greeting based on the current time.
 *
 * - From 06:00 to 11:59: "Good morning"
 * - From 12:00 to 19:59: "Good afternoon"
 * - From 20:00 to 05:59: "Good evening"
 *
 * @param currentTime The time to evaluate (defaults to current system time).
 * @return A localized greeting string.
 */
fun obtenerTiempoSaludo(currentTime: LocalDateTime = LocalDateTime.now()): String {
    val hour: Int = currentTime.hour

    return when (hour) {
        in 6..13 -> "Good morning"
        in 13..19 -> "Good afternoon"
        else -> "Good evening"
    }
}

/**
 * Controller for the main web interface.
 */
@Controller
class HelloController(
    @param:Value("\${app.message:Hello World}")
    private val message: String,
    private val userRepository: UserRepository // Inyectamos el repositorio
) {
    /**
     * Handles the root ("/") GET request.
     *
     * Identifies the logged-in user, increments their visit counter,
     * assigns a rank, and returns a personalized greeting.
     *
     * @param model The Spring UI model to bind data to the view.
     * @param principal The authenticated user session provided by Spring Security.
     * @return The name of the Thymeleaf template to render.
     */
    @GetMapping("/")
    fun welcome(
        model: Model,
        principal: Principal,
        request: HttpServletRequest // Añadimos este parámetro para inspeccionar la petición HTTP
    ): String {
        val username = principal.name
        val user = userRepository.findByUsername(username)
            ?: throw RuntimeException("Usuario no encontrado en la sesión")

        // Lógica de Logros "Anti-Fantasmas"
        // Leemos la cabecera "Accept" que envía el navegador
        val acceptHeader = request.getHeader("Accept") ?: ""

        // Solo sumamos visita si quien lo pide es el navegador cargando la página web real (HTML)
        if (acceptHeader.contains("text/html")) {
            user.visits += 1
            userRepository.save(user)
        }

        // El cálculo del rango se queda fuera del 'if' para que siempre evalúe el valor actual real
        val rank = when (user.visits) {
            in 1..5 -> "Newcomer \uD83D\uDC23"
            in 6..15 -> "Regular \uD83E\uDD8A"
            else -> "VIP \uD83D\uDC32"
        }

        val greeting = "${obtenerTiempoSaludo()}, ${user.username}!"

        model.addAttribute("message", greeting)
        model.addAttribute("name", user.username)
        model.addAttribute("visits", user.visits)
        model.addAttribute("rank", rank)

        return "welcome"
    }
}

/**
 * REST Controller for the API endpoints.
 */
@RestController
class HelloApiController(
    private val userRepository: UserRepository // Inyectamos el repositorio también en la API
) {

    /**
     * Handles the "/api/hello" GET request.
     *
     * Returns a JSON response with a time-based greeting, visit stats, and rank
     * for the authenticated user.
     *
     * @param principal The authenticated user session provided by Spring Security.
     * @return A map containing the message, stats, and the current timestamp.
     */
    @GetMapping("/api/hello", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun helloApi(principal: Principal): Map<String, Any> { // Cambiamos a <String, Any> porque enviamos Int y String

        val username = principal.name
        val user = userRepository.findByUsername(username)
            ?: throw RuntimeException("Usuario no encontrado en la sesión")

        // La API también cuenta como visita si acceden directamente a ella
        /*user.visits += 1
        userRepository.save(user)*/

        val rank = when (user.visits) {
            in 1..5 -> "Newcomer \uD83D\uDC23"
            in 6..15 -> "Regular \uD83E\uDD8A"
            else -> "VIP \uD83D\uDC32"
        }

        val greeting = "${obtenerTiempoSaludo()}, ${user.username}!"

        return mapOf(
            "message" to greeting,
            "visits" to user.visits,
            "rank" to rank,
            "timestamp" to java.time.Instant.now().toString()
        )
    }
}