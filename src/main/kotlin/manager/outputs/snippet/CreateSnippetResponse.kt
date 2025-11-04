package manager.outputs.snippet

data class CreateSnippetResponse(
    val snippetId: String,
    val errorMessage: List<String>,
)
