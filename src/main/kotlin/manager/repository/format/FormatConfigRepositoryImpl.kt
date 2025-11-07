package manager.repository.format

import manager.entity.FormatConfigEntity
import org.springframework.stereotype.Repository

@Repository
class FormatConfigRepositoryImpl(
    private val jpaRepository: FormatConfigJpaRepository,
) : FormatConfigRepositoryInterface {
    override fun saveFormatConfigForUser(
        userId: String,
        config: FormatConfig,
    ): String {
        val formatConfigEntity =
            FormatConfigEntity(
                userId = userId,
                config = config,
            )
        jpaRepository.save(formatConfigEntity)
        return formatConfigEntity.userId
    }

    override fun getFormatConfigForUser(userId: String): FormatConfig? {
        val entity = jpaRepository.findByUserId(userId) ?: return null
        return entity.config
    }

    override fun editFormatConfigForUser(
        userId: String,
        config: FormatConfig,
    ): FormatConfig? {
        var entity = jpaRepository.findByUserId(userId) ?: return null
        entity.config = config
        jpaRepository.save(entity)
        return entity.config
    }
}
