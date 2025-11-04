package manager.repository.lint

interface LintConfigRepositoryInterface {
    fun saveLintConfigForUser(
        userId: String,
        config: LintConfig,
    ): String

    fun getLintConfigForUser(userId: String): LintConfig?

    fun editLintConfigForUser(
        userId: String,
        config: LintConfig,
    ): LintConfig?
}
