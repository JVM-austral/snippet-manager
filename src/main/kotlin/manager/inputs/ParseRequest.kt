package manager.inputs

data class ParseRequest(
    val code: String,
    val language: String,
    val version: String,
)
