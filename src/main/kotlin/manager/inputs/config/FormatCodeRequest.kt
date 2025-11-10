package manager.inputs.config

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class FormatCodeRequest(
    val enforceNoSpacingAroundEquals: Boolean = false,
    val enforceSpacingAroundEquals: Boolean = false,
    val enforceSpacingAfterColonInDeclaration: Boolean = false,
    val enforceSpacingBeforeColonInDeclaration: Boolean = false,
    val mandatorySingleSpaceSeparation: Boolean = false,
    val mandatorySpaceSurroundingOperations: Boolean = false,
    val mandatoryLineBreakAfterStatement: Boolean = false,
    @field:Min(-1)
    @field:Max(3)
    val lineBreakAfterPrintLn: Int = -1,
    val ifBraceSameLine: Boolean = false,
    val ifBraceBelowLine: Boolean = false,
    @field:Min(-1)
    val indentInsideIf: Int = -1,
)
