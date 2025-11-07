package manager.repository.format

import manager.entity.FormatConfigEntity
import org.springframework.data.jpa.repository.JpaRepository

interface FormatConfigJpaRepository : JpaRepository<FormatConfigEntity, String> {
    fun findByUserId(userId: String): FormatConfigEntity?
}
