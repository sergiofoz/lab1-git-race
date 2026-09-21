package es.unizar.webeng.hello.service

import es.unizar.webeng.hello.repository.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        // 1. Buscamos el usuario en nuestra base de datos
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("Usuario no encontrado: $username")

        // 2. Se lo pasamos a Spring Security en el formato que él entiende
        return User.builder()
            .username(user.username)
            .password(user.passwordHash) // Spring Security comparará este hash con la contraseña que introduzca el usuario
            .roles("USER")
            .build()
    }
}