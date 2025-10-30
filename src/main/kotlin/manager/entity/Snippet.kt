package manager.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "snippets")
data class Snippet(
    @Id
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var description: String,
    @Enumerated(EnumType.STRING)
    var language: Languages,
    var version: String,
    var bucketId: String,
    var userId: String,
    var creationDate: LocalDateTime = LocalDateTime.now(),
)
