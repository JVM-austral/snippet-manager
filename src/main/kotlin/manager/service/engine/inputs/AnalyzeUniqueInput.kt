package manager.service.engine.inputs

import jakarta.validation.constraints.NotBlank
import manager.repository.format.FormatConfig

data class AnalyzeUniqueInput(
    @field:NotBlank val language: String,
    val version: String,
    val config: FormatConfig,
    @field:NotBlank val code: String,
)
