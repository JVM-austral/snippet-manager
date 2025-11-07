package manager.repository.lint

import manager.entity.LintConfigEntity
import org.springframework.stereotype.Repository

@Repository
class LintConfigRepositoryImpl(
    private val jpaRepository: LintConfigJpaRepository,
) : LintConfigRepositoryInterface {
    override fun saveLintConfigForUser(
        userId: String,
        config: LintConfig,
    ): String {
        val lintConfigEntity =
            LintConfigEntity(
                userId = userId,
                config = config,
            )
        jpaRepository.save(lintConfigEntity)
        return lintConfigEntity.userId
    }

    override fun getLintConfigForUser(userId: String): LintConfig? {
        val entity = jpaRepository.findByUserId(userId) ?: return null
        return entity.config
    }

    override fun editLintConfigForUser(
        userId: String,
        config: LintConfig,
    ): LintConfig? {
        var entity = jpaRepository.findByUserId(userId) ?: return null
        entity.config = config
        jpaRepository.save(entity)
        return entity.config
    }
}
