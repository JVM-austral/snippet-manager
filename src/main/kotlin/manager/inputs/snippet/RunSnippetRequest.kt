package manager.inputs.snippet

data class RunSnippetRequest(
    val snippetId: String,
    val varInputs: List<String> = emptyList(),
)
