package manager.repository.format

data class FormatConfig(
    val namingConvention: String = " ",
    val usePrintlnAnalyzer: Boolean = false,
    val useReadInputAnalyzer: Boolean = false,
)
