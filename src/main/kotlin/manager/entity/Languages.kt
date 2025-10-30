package manager.entity

enum class Languages(
    val displayName: String,
    val versions: List<String>,
) {
    PRINTSCRIPT("PrintScript", listOf("V1", "V2")),
}
