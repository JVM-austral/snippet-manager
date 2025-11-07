package manager.inputs.snippet

class RunSnippetRequest(
    val snippetId: String,
    val varInputs: List<String> = emptyList(),
)
