package manager.inputs.config

import manager.repository.format.FormatConfig

data class FormatForEngineRequest(
    val config: FormatConfig,
    val language: String,
    val version: String,
    val assetPath: String,
)
