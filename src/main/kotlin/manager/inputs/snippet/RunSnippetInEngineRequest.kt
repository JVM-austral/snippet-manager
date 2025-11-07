package manager.inputs.snippet

data class RunSnippetInEngineRequest(
    val language: String,
    val version: String,
    val assetPath: String,
    val varInputs: List<String> = emptyList(),
)
