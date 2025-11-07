package manager.repository.testing

import manager.entity.TestEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TestingJpaRepository : JpaRepository<TestEntity, String> {
    fun findAllBySnippetId(snippetId: String): List<TestEntity>
}
