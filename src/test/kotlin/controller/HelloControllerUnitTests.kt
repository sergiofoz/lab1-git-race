package es.unizar.webeng.hello.controller

import es.unizar.webeng.hello.model.UserAccount
import es.unizar.webeng.hello.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.ui.ConcurrentModel
import java.security.Principal

class HelloControllerUnitTests {

    @Test
    fun `welcome method suma visita y asigna rango Newcomer cuando es peticion html`() {
        // 1. Preparamos los dobles (Mocks)
        val userRepository = mock(UserRepository::class.java)
        val principal = mock(Principal::class.java)
        val request = mock(HttpServletRequest::class.java)

        // 2. Configuramos su comportamiento
        val dummyUser = UserAccount(id = 1, username = "TestUser", passwordHash = "hash", visits = 0)
        `when`(principal.name).thenReturn("TestUser")
        `when`(userRepository.findByUsername("TestUser")).thenReturn(dummyUser)
        `when`(request.getHeader("Accept")).thenReturn("text/html")

        // 3. Ejecutamos
        val controller = HelloController("Msg", userRepository)
        val model = ConcurrentModel()
        val viewName = controller.welcome(model, principal, request)

        // 4. Comprobamos
        assertThat(viewName).isEqualTo("welcome")
        assertThat(model.getAttribute("name")).isEqualTo("TestUser")
        assertThat(model.getAttribute("visits")).isEqualTo(1)
        assertThat(model.getAttribute("rank")).isEqualTo("Newcomer \uD83D\uDC23")
    }

    // --- NUEVOS TESTS DE COBERTURA ---

    @Test
    fun `welcome method asigna rango Regular correctamente al pasar de 5 visitas`() {
        val userRepository = mock(UserRepository::class.java)
        val principal = mock(Principal::class.java)
        val request = mock(HttpServletRequest::class.java)

        // El usuario empieza con 5 visitas
        val dummyUser = UserAccount(id = 1, username = "UserReg", passwordHash = "hash", visits = 5)
        `when`(principal.name).thenReturn("UserReg")
        `when`(userRepository.findByUsername("UserReg")).thenReturn(dummyUser)
        `when`(request.getHeader("Accept")).thenReturn("text/html")

        val controller = HelloController("Msg", userRepository)
        val model = ConcurrentModel()
        controller.welcome(model, principal, request)

        // Al sumarle 1, llega a 6 y debe ser Regular
        assertThat(model.getAttribute("visits")).isEqualTo(6)
        assertThat(model.getAttribute("rank")).isEqualTo("Regular \uD83E\uDD8A")
    }

    @Test
    fun `welcome method asigna rango VIP correctamente al pasar de 15 visitas`() {
        val userRepository = mock(UserRepository::class.java)
        val principal = mock(Principal::class.java)
        val request = mock(HttpServletRequest::class.java)

        val dummyUser = UserAccount(id = 1, username = "UserVIP", passwordHash = "hash", visits = 15)
        `when`(principal.name).thenReturn("UserVIP")
        `when`(userRepository.findByUsername("UserVIP")).thenReturn(dummyUser)
        `when`(request.getHeader("Accept")).thenReturn("text/html")

        val controller = HelloController("Msg", userRepository)
        val model = ConcurrentModel()
        controller.welcome(model, principal, request)

        // Al sumarle 1, llega a 16 y debe ser VIP
        assertThat(model.getAttribute("visits")).isEqualTo(16)
        assertThat(model.getAttribute("rank")).isEqualTo("VIP \uD83D\uDC32")
    }

    @Test
    fun `welcome method NO suma visita si la peticion no pide html (proteccion anti-fantasma)`() {
        val userRepository = mock(UserRepository::class.java)
        val principal = mock(Principal::class.java)
        val request = mock(HttpServletRequest::class.java)

        val dummyUser = UserAccount(id = 1, username = "Bot", passwordHash = "hash", visits = 2)
        `when`(principal.name).thenReturn("Bot")
        `when`(userRepository.findByUsername("Bot")).thenReturn(dummyUser)

        // Simulamos petición de un script JS (no pide text/html explícitamente)
        `when`(request.getHeader("Accept")).thenReturn("application/json")

        val controller = HelloController("Msg", userRepository)
        val model = ConcurrentModel()
        controller.welcome(model, principal, request)

        // La visita no debió sumar, se queda intacta en 2
        assertThat(model.getAttribute("visits")).isEqualTo(2)
    }

    @Test
    fun `api method devuelve estadisticas sin alterar el contador de visitas`() {
        val userRepository = mock(UserRepository::class.java)
        val principal = mock(Principal::class.java)

        val dummyUser = UserAccount(id = 2, username = "ApiUser", passwordHash = "hash", visits = 10)
        `when`(principal.name).thenReturn("ApiUser")
        `when`(userRepository.findByUsername("ApiUser")).thenReturn(dummyUser)

        val apiController = HelloApiController(userRepository)
        val response = apiController.helloApi(principal)

        assertThat(response["message"]).toString().contains("ApiUser")
        assertThat(response["visits"]).isEqualTo(10)
    }
}