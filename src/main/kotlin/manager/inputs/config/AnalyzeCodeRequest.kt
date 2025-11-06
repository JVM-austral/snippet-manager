package manager.inputs.config

data class AnalyzeCodeRequest(
    val namingConvention: String = " ",
    val usePrintlnAnalyzer: Boolean = false,
    val useReadInputAnalyzer: Boolean = false,
)
