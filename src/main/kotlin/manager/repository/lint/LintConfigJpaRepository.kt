package manager.repository.lint

import manager.entity.LintConfigEntity
import org.springframework.data.jpa.repository.JpaRepository

interface LintConfigJpaRepository : JpaRepository<LintConfigEntity, String> {
    fun findByUserId(userId: String): LintConfigEntity?
}
