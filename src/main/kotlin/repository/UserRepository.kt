package es.unizar.webeng.hello.repository

import es.unizar.webeng.hello.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository

// JpaRepository ya incluye métodos como save(), findAll(), etc.
interface UserRepository : JpaRepository<UserAccount, Long> {
    // Spring crea la consulta SQL automáticamente solo con leer el nombre del método
    fun findByUsername(username: String): UserAccount?
}