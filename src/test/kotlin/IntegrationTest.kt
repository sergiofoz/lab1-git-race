package es.unizar.webeng.hello

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.util.LinkedMultiValueMap
import java.net.HttpURLConnection
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class IntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private fun url(path: String): String = "http://localhost:$port$path"

    // INTERVENCIÓN DIRECTA EN EL CLIENTE HTTP
    @BeforeEach
    fun setup() {
        // Creamos una fábrica de conexiones a medida que fuerza a 'instanceFollowRedirects' a ser falso
        val factory = object : SimpleClientHttpRequestFactory() {
            override fun prepareConnection(connection: HttpURLConnection, httpMethod: String) {
                super.prepareConnection(connection, httpMethod)
                connection.instanceFollowRedirects = false // <-- Apagado estricto de redirecciones
            }
        }
        // Le inyectamos esta fábrica al TestRestTemplate
        restTemplate.restTemplate.requestFactory = factory
    }

    // SIMULADOR DE NAVEGADOR
    private fun scrapeCsrfAndCookie(path: String): Pair<String, String> {
        val response = restTemplate.getForEntity(url(path), String::class.java)
        val rawCookie = response.headers.getFirst(HttpHeaders.SET_COOKIE) ?: ""
        val cookie = rawCookie.substringBefore(";")
        val csrfToken = Regex("name=\"_csrf\" value=\"([^\"]+)\"").find(response.body ?: "")?.groupValues?.get(1) ?: ""
        return Pair(csrfToken, cookie)
    }

    @Test
    fun `should serve Bootstrap CSS correctly`() {
        val response = restTemplate.getForEntity(url("/webjars/bootstrap/5.3.8/css/bootstrap.min.css"), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("body")
    }

    @Test
    fun `should expose actuator health endpoint`() {
        val response = restTemplate.getForEntity(url("/actuator/health"), String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("UP")
    }

    // --- ESCENARIO 1: Seguridad Perimetral ---
    @Test
    fun `acceder a rutas protegidas sin sesion redirige al login`() {
        val response = restTemplate.getForEntity(url("/"), String::class.java)

        // Ahora sí, capturaremos el 302 FOUND de manera limpia
        assertThat(response.statusCode).isEqualTo(HttpStatus.FOUND)
        assertThat(response.headers.location?.toString()).endsWith("/login")
    }

    // --- ESCENARIO 2: Registro de Usuarios ---
    @Test
    fun `registro exitoso y prevencion de usuarios duplicados`() {
        val uniqueUsername = "user_${UUID.randomUUID()}"

        val (csrf1, cookie1) = scrapeCsrfAndCookie("/register")
        val form1 = LinkedMultiValueMap<String, String>().apply {
            add("username", uniqueUsername)
            add("password", "password123")
            if (csrf1.isNotEmpty()) add("_csrf", csrf1)
        }
        val headers1 = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            if (cookie1.isNotEmpty()) add(HttpHeaders.COOKIE, cookie1)
        }
        val responseSuccess = restTemplate.postForEntity(url("/register"), HttpEntity(form1, headers1), String::class.java)

        assertThat(responseSuccess.statusCode).isEqualTo(HttpStatus.FOUND)
        assertThat(responseSuccess.headers.location?.toString()).endsWith("/login?registered")

        val (csrf2, cookie2) = scrapeCsrfAndCookie("/register")
        val form2 = LinkedMultiValueMap<String, String>().apply {
            add("username", uniqueUsername)
            add("password", "password123")
            if (csrf2.isNotEmpty()) add("_csrf", csrf2)
        }
        val headers2 = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            if (cookie2.isNotEmpty()) add(HttpHeaders.COOKIE, cookie2)
        }
        val responseError = restTemplate.postForEntity(url("/register"), HttpEntity(form2, headers2), String::class.java)

        assertThat(responseError.statusCode).isEqualTo(HttpStatus.FOUND)
        assertThat(responseError.headers.location?.toString()).endsWith("/register?error")
    }

    // --- ESCENARIO 3: Viaje del Usuario Completo ---
    @Test
    fun `viaje completo login, contador de visitas, evolucion de rango y api`() {
        val username = "gamer_${UUID.randomUUID()}"
        val password = "securepassword"

        // 1. Registro
        val (regCsrf, regCookie) = scrapeCsrfAndCookie("/register")
        val registerForm = LinkedMultiValueMap<String, String>().apply {
            add("username", username)
            add("password", password)
            if (regCsrf.isNotEmpty()) add("_csrf", regCsrf)
        }
        val regHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            if (regCookie.isNotEmpty()) add(HttpHeaders.COOKIE, regCookie)
        }
        restTemplate.postForEntity(url("/register"), HttpEntity(registerForm, regHeaders), String::class.java)

        // 2. Login
        val (loginCsrf, loginCookie) = scrapeCsrfAndCookie("/login")
        val loginForm = LinkedMultiValueMap<String, String>().apply {
            add("username", username)
            add("password", password)
            if (loginCsrf.isNotEmpty()) add("_csrf", loginCsrf)
        }
        val loginHeaders = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            if (loginCookie.isNotEmpty()) add(HttpHeaders.COOKIE, loginCookie)
        }
        val loginResponse = restTemplate.postForEntity(url("/login"), HttpEntity(loginForm, loginHeaders), String::class.java)

        // Comprobamos explícitamente que el login fue exitoso y nos redirige
        assertThat(loginResponse.statusCode).isEqualTo(HttpStatus.FOUND)

        // 3. Captura de la Cookie Autenticada
        val rawSessionCookie = loginResponse.headers.getFirst(HttpHeaders.SET_COOKIE) ?: ""
        val sessionCookie = rawSessionCookie.substringBefore(";")
        assertThat(sessionCookie).isNotEmpty()

        // Preparamos las cabeceras inyectando nuestra Cookie autenticada
        val webHeaders = HttpHeaders().apply {
            add(HttpHeaders.COOKIE, sessionCookie)
            add(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
        }
        val webRequest = HttpEntity<Void>(webHeaders)

        // 4. Camino hacia los Logros
        val firstVisit = restTemplate.exchange(url("/"), HttpMethod.GET, webRequest, String::class.java)
        assertThat(firstVisit.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(firstVisit.body).contains(username)
        assertThat(firstVisit.body).contains("1")
        assertThat(firstVisit.body).contains("Newcomer")

        repeat(5) {
            restTemplate.exchange(url("/"), HttpMethod.GET, webRequest, String::class.java)
        }

        val regularVisit = restTemplate.exchange(url("/"), HttpMethod.GET, webRequest, String::class.java)
        assertThat(regularVisit.body).contains("7")
        assertThat(regularVisit.body).contains("Regular")

        repeat(9) {
            restTemplate.exchange(url("/"), HttpMethod.GET, webRequest, String::class.java)
        }

        val vipVisit = restTemplate.exchange(url("/"), HttpMethod.GET, webRequest, String::class.java)
        assertThat(vipVisit.body).contains("17")
        assertThat(vipVisit.body).contains("VIP")

        // 5. Verificación en API
        val apiHeaders = HttpHeaders().apply {
            add(HttpHeaders.COOKIE, sessionCookie)
            add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        }
        val apiRequest = HttpEntity<Void>(apiHeaders)
        val apiResponse = restTemplate.exchange(url("/api/hello"), HttpMethod.GET, apiRequest, Map::class.java)

        assertThat(apiResponse.statusCode).isEqualTo(HttpStatus.OK)

        val responseBody = apiResponse.body as Map<*, *>
        val visits = responseBody["visits"].toString().toInt()

        assertThat(visits).isIn(17, 18)
        assertThat(responseBody["rank"].toString()).contains("VIP")
    }
}