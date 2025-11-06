package manager.outputs.snippet

data class SnippetResponse(
    val id: String,
    val name: String,
    val description: String,
    val snippet: String,
    val language: String,
    val version: String,
    val compliance: String,
    val author: String,
)
