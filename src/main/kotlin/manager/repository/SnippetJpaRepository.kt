package manager.repository
import manager.entity.Snippet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SnippetJpaRepository : JpaRepository<Snippet, String> {
    fun findAllByUserId(userId: String): List<Snippet>
}
