package manager.repository.format

data class FormatConfig(
    val enforceNoSpacingAroundEquals: Boolean = false,
    val enforceSpacingAroundEquals: Boolean = false,
    val enforceSpacingAfterColonInDeclaration: Boolean = false,
    val enforceSpacingBeforeColonInDeclaration: Boolean = false,
    val mandatorySingleSpaceSeparation: Boolean = false,
    val mandatorySpaceSurroundingOperations: Boolean = false,
    val mandatoryLineBreakAfterStatement: Boolean = false,
    val lineBreakAfterPrintLn: Int = -1,
    val ifBraceSameLine: Boolean = false,
    val ifBraceBelowLine: Boolean = false,
    val indentInsideIf: Int = -1,
)
