package manager.entity

enum class Languages(
    val displayName: String,
    val versions: List<String>,
) {
    PRINTSCRIPT("PrintScript", listOf("1.0", "1.1")),
}
