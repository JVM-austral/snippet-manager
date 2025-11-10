package manager.service.engine.response

data class LintResponse(
    val message: String,
    val line: Int,
    val column: Int,
)
