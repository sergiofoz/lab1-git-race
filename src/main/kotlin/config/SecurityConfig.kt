package es.unizar.webeng.hello.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // 1. REGLAS DE LAS URLs
            .authorizeHttpRequests { auth ->
                auth
                    // Dejamos pasar recursos estáticos (CSS, JS, imágenes de Bootstrap)
                    .requestMatchers("/css/**", "/js/**", "/assets/**", "/webjars/**").permitAll()
                    // Dejamos pasar la página de login, registro y el health-check del Actuator
                    .requestMatchers("/login", "/register", "/actuator/health").permitAll()
                    // CUALQUIER otra petición (como "/" o "/api/hello") requiere estar logueado
                    .anyRequest().authenticated()
            }

            // 2. CONFIGURACIÓN DEL LOGIN
            .formLogin { form ->
                form
                    .loginPage("/login") // Le decimos que usaremos nuestro propio HTML, no el feo por defecto
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/", true) // Si el login va bien, llévame al saludo
                    .permitAll()
            }

            // 3. CONFIGURACIÓN DE LA SESIÓN Y COOKIES
            .sessionManagement { session ->
                // Evitamos ataques de fijación de sesión creando una nueva id al loguearse.
                // Nota: Spring Security ya marca la cookie JSESSIONID como HttpOnly por defecto.
                session.sessionFixation().migrateSession()
            }

            // 4. CONFIGURACIÓN DEL LOGOUT
            .logout { logout ->
                logout
                    .logoutSuccessUrl("/login?logout") // Al salir, llévame al login con un aviso
                    .permitAll()
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): org.springframework.security.crypto.password.PasswordEncoder {
        // BCrypt es el estándar seguro para encriptar contraseñas
        return org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
    }
}