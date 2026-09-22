package es.unizar.webeng.hello.model

import jakarta.persistence.*

@Entity
@Table(name = "app_users")
class UserAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val username: String = "", // Asignamos un valor por defecto

    @Column(nullable = false)
    val passwordHash: String = "", // Asignamos valor por defecto

    @Column(nullable = false)
    var visits: Int = 0 // Aqui es var, porque es variable el valor de visitas, cada vez que entre se va a modificar
)