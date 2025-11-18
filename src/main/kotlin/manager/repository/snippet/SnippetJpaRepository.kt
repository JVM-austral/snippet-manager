package manager.repository.snippet

import manager.entity.Snippet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SnippetJpaRepository : JpaRepository<Snippet, String> {
    fun findAllByUserId(
        userId: String,
        pageable: Pageable,
    ): Page<Snippet>

    fun findAllByUserId(userId: String): List<Snippet>

    @Query(
        """
    SELECT s FROM Snippet s
    WHERE s.userId = :userId
    AND LOWER(s.name) LIKE LOWER(CONCAT('%', :filter, '%'))
    """,
    )
    fun searchByUserIdAndName(
        userId: String,
        filter: String,
        pageable: Pageable,
    ): List<Snippet>
}
