package manager.inputs

data class AnalyzeCodeRequest(
    val namingConvention: String = " ",
    val usePrintlnAnalyzer: Boolean = false,
    val useReadInputAnalyzer: Boolean = false,
)
