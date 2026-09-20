package es.unizar.webeng.hello.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

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
    private val message: String
) {
    /**
     * Handles the root ("/") GET request.
     *
     * If a name is provided, it returns a personalized greeting.
     * If no name is provided, it returns a time-based greeting (e.g., "Good morning").
     *
     * @param model The Spring UI model to bind data to the view.
     * @param name Optional query parameter for personalization.
     * @return The name of the Thymeleaf template to render.
     */

    @GetMapping("/")
    fun welcome(
        model: Model,
        @RequestParam(defaultValue = "") name: String
    ): String {
        val greeting = if (name.isNotBlank()) {
            "Hello, $name!"
        } else {
            obtenerTiempoSaludo()
        }
        model.addAttribute("message", greeting)
        model.addAttribute("name", name)
        return "welcome"
    }
}

/**
 * REST Controller for the API endpoints.
 */

@RestController
class HelloApiController {

    /**
     * Handles the "/api/hello" GET request.
     *
     * Returns a JSON response with a time-based greeting or a personalized one.
     *
     * @param name Optional query parameter for personalization.
     * @return A map containing the message and the current timestamp.
     */

    @GetMapping("/api/hello", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun helloApi(@RequestParam(defaultValue = "World") name: String): Map<String, String> {

        val greeting = if (name.isNotBlank()) {
            "Hello, $name!"
        } else {
            obtenerTiempoSaludo()
        }

        return mapOf(
            "message" to greeting,
            "timestamp" to java.time.Instant.now().toString()
        )
    }
}
