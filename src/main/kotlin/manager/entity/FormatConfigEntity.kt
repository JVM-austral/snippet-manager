package manager.entity

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import manager.repository.format.FormatConfig
import manager.repository.format.FormatConfigConverter

@Entity
@Table(name = "format_config")
data class FormatConfigEntity(
    @Id
    val userId: String,
    @Column(columnDefinition = "jsonb")
    @Convert(converter = FormatConfigConverter::class)
    var config: FormatConfig,
)
