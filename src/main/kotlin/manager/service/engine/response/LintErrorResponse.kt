package manager.service.engine.response

data class LintErrorResponse(
    val message: String,
    val line: Int,
    val column: Int,
)
