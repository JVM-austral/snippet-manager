package manager.outputs.snippet

data class RunSnippetResponse(
    val output: List<String>,
    val errors: List<String>,
)
