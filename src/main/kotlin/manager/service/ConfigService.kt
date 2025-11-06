package manager.service

import manager.inputs.config.AnalyzeCodeRequest
import manager.inputs.config.FormatCodeRequest
import manager.inputs.config.FormatForEngineRequest
import manager.repository.format.FormatConfig
import manager.repository.format.FormatConfigRepositoryInterface
import manager.repository.lint.LintConfig
import manager.repository.lint.LintConfigRepositoryInterface
import manager.repository.snippet.SnippetRepositoryInterface
import org.springframework.stereotype.Service

@Service
class ConfigService(
    private val snippetRepository: SnippetRepositoryInterface,
    private val lintConfigRepository: LintConfigRepositoryInterface,
    private val formatConfigRepository: FormatConfigRepositoryInterface,
) {
    fun saveLintingConfig(
        userId: String,
        request: AnalyzeCodeRequest,
    ) {
        val lintConfigEntity = lintConfigRepository.getLintConfigForUser(userId)
        if (lintConfigEntity == null) {
            val newConfig =
                lintConfigRepository.saveLintConfigForUser(
                    userId,
                    LintConfig(
                        namingConvention = request.namingConvention,
                        usePrintlnAnalyzer = request.usePrintlnAnalyzer,
                        useReadInputAnalyzer = request.useReadInputAnalyzer,
                    ),
                )
        } else {
            lintConfigRepository.editLintConfigForUser(
                userId,
                LintConfig(
                    namingConvention = request.namingConvention,
                    usePrintlnAnalyzer = request.usePrintlnAnalyzer,
                    useReadInputAnalyzer = request.useReadInputAnalyzer,
                ),
            )
        }
    }

    fun saveFormatConfig(
        userId: String,
        request: FormatCodeRequest,
    ) {
        val formatConfigEntity = formatConfigRepository.getFormatConfigForUser(userId)
        if (formatConfigEntity == null) {
            val newConfig =
                formatConfigRepository.saveFormatConfigForUser(
                    userId,
                    FormatConfig(
                        enforceNoSpacingAroundEquals = request.enforceNoSpacingAroundEquals,
                        enforceSpacingAroundEquals = request.enforceSpacingAroundEquals,
                        enforceSpacingAfterColonInDeclaration = request.enforceSpacingAfterColonInDeclaration,
                        enforceSpacingBeforeColonInDeclaration = request.enforceSpacingBeforeColonInDeclaration,
                        mandatorySingleSpaceSeparation = request.mandatorySingleSpaceSeparation,
                        mandatorySpaceSurroundingOperations = request.mandatorySpaceSurroundingOperations,
                        mandatoryLineBreakAfterStatement = request.mandatoryLineBreakAfterStatement,
                        lineBreakAfterPrintLn = request.lineBreakAfterPrintLn,
                        ifBraceSameLine = request.ifBraceSameLine,
                        ifBraceBelowLine = request.ifBraceBelowLine,
                        indentInsideIf = request.indentInsideIf,
                    ),
                )
        } else {
            formatConfigRepository.editFormatConfigForUser(
                userId,
                FormatConfig(
                    enforceNoSpacingAroundEquals = request.enforceNoSpacingAroundEquals,
                    enforceSpacingAroundEquals = request.enforceSpacingAroundEquals,
                    enforceSpacingAfterColonInDeclaration = request.enforceSpacingAfterColonInDeclaration,
                    enforceSpacingBeforeColonInDeclaration = request.enforceSpacingBeforeColonInDeclaration,
                    mandatorySingleSpaceSeparation = request.mandatorySingleSpaceSeparation,
                    mandatorySpaceSurroundingOperations = request.mandatorySpaceSurroundingOperations,
                    mandatoryLineBreakAfterStatement = request.mandatoryLineBreakAfterStatement,
                    lineBreakAfterPrintLn = request.lineBreakAfterPrintLn,
                    ifBraceSameLine = request.ifBraceSameLine,
                    ifBraceBelowLine = request.ifBraceBelowLine,
                    indentInsideIf = request.indentInsideIf,
                ),
            )
        }
    }

    fun getListOfFormatRequests(userId: String): List<FormatForEngineRequest> {
        val snippets = snippetRepository.getAllSnippetsByUserId(userId)
        val formatConfig = getFormatConfigForUser(userId)
        val requests =
            snippets.map { snippet ->
                FormatForEngineRequest(
                    config = formatConfig,
                    language = snippet.language.name,
                    version = snippet.version,
                    assetPath = snippet.bucketId,
                )
            }
        return requests
    }

    private fun getFormatConfigForUser(userId: String): FormatConfig {
        val formatConfigEntity = formatConfigRepository.getFormatConfigForUser(userId)
        if (formatConfigEntity == null) {
            val defaultConfig = FormatConfig()
            return defaultConfig
        }
        return formatConfigEntity
    }

    private fun getLintConfigForUser(userId: String): LintConfig {
        val lintConfigEntity = lintConfigRepository.getLintConfigForUser(userId)
        if (lintConfigEntity == null) {
            val defaultConfig = LintConfig()
            return defaultConfig
        }
        return lintConfigEntity
    }
}
