package manager.service.app

import manager.inputs.config.AnalyzeCodeRequest
import manager.inputs.config.FormatCodeRequest
import manager.inputs.config.FormatForEngineRequest
import manager.inputs.snippet.FormatUniqueInput
import manager.repository.format.FormatConfig
import manager.repository.format.FormatConfigRepositoryInterface
import manager.repository.lint.LintConfig
import manager.repository.lint.LintConfigRepositoryInterface
import manager.repository.snippet.SnippetRepositoryInterface
import manager.service.engine.inputs.FormatUniqueInputForEngine
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

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
        checkLintingRules(request)
        val lintConfigEntity = lintConfigRepository.getLintConfigForUser(userId)
        if (lintConfigEntity == null) {
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
        checkFormattingRules(request)
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

    fun getFormatConfigForUser(userId: String): FormatConfig {
        val formatConfigEntity = formatConfigRepository.getFormatConfigForUser(userId)
        if (formatConfigEntity == null) {
            val defaultConfig = FormatConfig()
            return defaultConfig
        }
        return formatConfigEntity
    }

    public fun getLintConfigForUser(userId: String): LintConfig {
        val lintConfigEntity = lintConfigRepository.getLintConfigForUser(userId)
        if (lintConfigEntity == null) {
            val defaultConfig = LintConfig()
            return defaultConfig
        }
        return lintConfigEntity
    }

    fun createFormatRequest(
        userId: String,
        request: FormatUniqueInput,
    ): FormatUniqueInputForEngine {
        val config =
            try {
                getFormatConfigForUser(userId)
            } catch (e: Exception) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Format config for user: $userId not found")
            }
        val snippet = snippetRepository.getSnippetById(request.snippetId)
        if (snippet === null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $request.snippetId")
        }
        return FormatUniqueInputForEngine(
            code = request.code,
            language = snippet.language.name,
            version = snippet.version,
            config = config,
        )
    }

    private fun checkLintingRules(config: AnalyzeCodeRequest) {
        if (config.namingConvention != "camelCase" && config.namingConvention != "snake_case" && config.namingConvention != " ") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid naming convention: ${config.namingConvention}")
        }
    }

    private fun checkFormattingRules(config: FormatCodeRequest) {
        if (config.enforceNoSpacingAroundEquals && config.enforceSpacingAroundEquals) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Conflicting rules for spacing around equals")
        }
        if (config.mandatorySingleSpaceSeparation && config.mandatorySpaceSurroundingOperations) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Conflicting rules for spacing around operations")
        }
    }
}
