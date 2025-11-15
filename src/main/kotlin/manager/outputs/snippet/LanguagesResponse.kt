package manager.outputs.snippet

data class LanguagesResponse(
    val displayName: String,
    val versions: List<String>,
    val extension: String,
)
