package manager.inputs.config

import manager.repository.lint.LintConfig

data class LintForEngineRequest(
    val config: LintConfig,
    val language: String,
    val version: String,
    val assetPath: String,
)
