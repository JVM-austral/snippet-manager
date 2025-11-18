package manager.outputs.snippet

data class CreateSnippetResponse(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val language: String? = null,
    val version: String? = null,
    val author: String? = null,
    val errorMessage: List<String>,
)
