package manager.service.engine.response

data class LintResponse(
    val lintErrors: List<LintErrorResponse>,
)
