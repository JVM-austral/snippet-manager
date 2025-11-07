package manager.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "deleted_snippets")
data class DeletedSnippet(
    @Id
    val id: String,
    var name: String,
    var description: String,
    @Enumerated(EnumType.STRING)
    var language: Languages,
    var version: String,
    var bucketId: String,
    var userId: String,
    var creationDate: LocalDateTime,
    var state: CompilantState = CompilantState.PENDING,
)
