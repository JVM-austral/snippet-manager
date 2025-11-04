package manager.entity

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import manager.repository.lint.LintConfig
import manager.repository.lint.LintConfigConverter

@Entity
@Table(name = "lint_config")
data class LintConfigEntity(
    @Id
    val userId: String,
    @Column(columnDefinition = "jsonb")
    @Convert(converter = LintConfigConverter::class)
    var config: LintConfig,
)
