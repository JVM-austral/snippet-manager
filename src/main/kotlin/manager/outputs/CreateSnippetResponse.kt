package manager.outputs

data class CreateSnippetResponse(
    val snippetId: String,
    val errorMessage: List<String>,
)
