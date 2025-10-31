package manager.outputs

data class SnippetPermisesResponse(
    val id: String,
    val snippetId: String,
    val userId: String,
    val permission: String,
)
