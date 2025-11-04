package manager.repository.format

interface FormatConfigRepositoryInterface {
    fun saveFormatConfigForUser(
        userId: String,
        config: FormatConfig,
    ): String

    fun getFormatConfigForUser(userId: String): FormatConfig?

    fun editFormatConfigForUser(
        userId: String,
        config: FormatConfig,
    ): FormatConfig?
}
