package manager.service.engine.inputs

import jakarta.validation.constraints.NotBlank
import manager.repository.lint.LintConfig

data class LintUniqueInputForEngine(
    @field:NotBlank val language: String,
    val version: String,
    val config: LintConfig,
    @field:NotBlank val assetPath: String,
)
