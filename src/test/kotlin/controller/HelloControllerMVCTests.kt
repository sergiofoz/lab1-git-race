package es.unizar.webeng.hello.controller

import es.unizar.webeng.hello.model.UserAccount
import es.unizar.webeng.hello.repository.UserRepository
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.security.Principal

@WebMvcTest(HelloController::class, HelloApiController::class)
class HelloControllerMVCTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        val dummyDeveloper = UserAccount(id = 1, username = "Developer", passwordHash = "hash", visits = 0)
        `when`(userRepository.findByUsername("Developer")).thenReturn(dummyDeveloper)

        val dummyTestUser = UserAccount(id = 2, username = "Test", passwordHash = "hash", visits = 5)
        `when`(userRepository.findByUsername("Test")).thenReturn(dummyTestUser)
    }

    @Test
    @WithMockUser(username = "Developer")
    fun `should return home page reading name from principal and tracking visit`() {
        // Inyectamos el Principal directamente para evitar el NullPointerException en el controlador aislado
        val mockPrincipal = Principal { "Developer" }

        mockMvc.perform(
            get("/")
                .principal(mockPrincipal)
                .header("Accept", "text/html")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(view().name("welcome"))
            .andExpect(model().attribute("name", equalTo("Developer")))
            .andExpect(model().attribute("visits", equalTo(1)))
            .andExpect(model().attribute("rank", equalTo("Newcomer \uD83D\uDC23")))
    }

    @Test
    @WithMockUser(username = "Test")
    fun `should return API response as JSON without incrementing visits`() {
        val mockPrincipal = Principal { "Test" }

        mockMvc.perform(
            get("/api/hello")
                .principal(mockPrincipal)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.visits", equalTo(5)))
            .andExpect(jsonPath("$.timestamp").exists())
    }
}