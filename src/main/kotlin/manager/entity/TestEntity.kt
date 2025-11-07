package manager.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "test")
class TestEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),
    var snippetId: String,
    var name: String,
    var input: List<String>,
    var output: List<String>,
)
