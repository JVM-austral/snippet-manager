package manager.repository.lint

data class LintConfig(
    var namingConvention: String = " ",
    var usePrintlnAnalyzer: Boolean = false,
    var useReadInputAnalyzer: Boolean = false,
)
