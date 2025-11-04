package manager.inputs.snippet

data class ParseRequest(
    val code: String,
    val language: String,
    val version: String,
)
