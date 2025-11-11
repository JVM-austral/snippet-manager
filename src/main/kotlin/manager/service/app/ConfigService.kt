package manager.service.app

import manager.inputs.config.AnalyzeCodeRequest
import manager.inputs.config.FormatCodeRequest
import manager.inputs.config.FormatForEngineRequest
import manager.inputs.config.LintForEngineRequest
import manager.inputs.snippet.FormatUniqueInput
import manager.repository.format.FormatConfig
import manager.repository.format.FormatConfigRepositoryInterface
import manager.repository.lint.LintConfig
import manager.repository.lint.LintConfigRepositoryInterface
import manager.repository.snippet.SnippetRepositoryInterface
import manager.service.engine.EngineServiceInterface
import manager.service.engine.inputs.FormatUniqueInputForEngine
import manager.service.engine.response.LintResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ConfigService(
    private val snippetRepository: SnippetRepositoryInterface,
    private val lintConfigRepository: LintConfigRepositoryInterface,
    private val formatConfigRepository: FormatConfigRepositoryInterface,
    private val engineService: EngineServiceInterface,
) {
    private val log = org.slf4j.LoggerFactory.getLogger(ConfigService::class.java)

    fun saveLintingConfig(
        userId: String,
        request: AnalyzeCodeRequest,
    ) {
        log.info("Saving linting config for userId: $userId")
        try {
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
                log.info("Created new linting config for userId: $userId")
            } else {
                lintConfigRepository.editLintConfigForUser(
                    userId,
                    LintConfig(
                        namingConvention = request.namingConvention,
                        usePrintlnAnalyzer = request.usePrintlnAnalyzer,
                        useReadInputAnalyzer = request.useReadInputAnalyzer,
                    ),
                )
                log.info("Updated linting config for userId: $userId")
            }
        } catch (e: Exception) {
            log.warn("Error saving linting config for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    fun saveFormatConfig(
        userId: String,
        request: FormatCodeRequest,
    ) {
        log.info("Saving format config for userId: $userId")
        try {
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
                log.info("Created new format config for userId: $userId")
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
                log.info("Updated format config for userId: $userId")
            }
        } catch (e: Exception) {
            log.warn("Error saving format config for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    fun getListOfFormatRequests(userId: String): List<FormatForEngineRequest> {
        log.info("Getting list of format requests for userId: $userId")
        try {
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
            log.info("Generated ${requests.size} format requests for userId: $userId")
            return requests
        } catch (e: Exception) {
            log.warn("Error getting format requests for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    fun getListOfLintRequests(userId: String): List<LintForEngineRequest> {
        log.info("Getting list of lint requests for userId: $userId")
        try {
            val snippets = snippetRepository.getAllSnippetsByUserId(userId)
            val lintConfig = getLintConfigForUser(userId)
            val requests =
                snippets.map { snippet ->
                    LintForEngineRequest(
                        config = lintConfig,
                        language = snippet.language.name,
                        version = snippet.version,
                        assetPath = snippet.bucketId,
                        snippetId = snippet.id,
                    )
                }
            log.info("Generated ${requests.size} lint requests for userId: $userId")
            return requests
        } catch (e: Exception) {
            log.warn("Error getting lint requests for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    fun getFormatConfigForUser(userId: String): FormatConfig {
        log.info("Getting format config for userId: $userId")
        val formatConfigEntity = formatConfigRepository.getFormatConfigForUser(userId)
        if (formatConfigEntity == null) {
            log.info("No format config found for userId: $userId, using default")
            val defaultConfig = FormatConfig()
            return defaultConfig
        }
        return formatConfigEntity
    }

    public fun getLintConfigForUser(userId: String): LintConfig {
        log.info("Getting lint config for userId: $userId")
        val lintConfigEntity = lintConfigRepository.getLintConfigForUser(userId)
        if (lintConfigEntity == null) {
            log.info("No lint config found for userId: $userId, using default")
            val defaultConfig = LintConfig()
            return defaultConfig
        }
        return lintConfigEntity
    }

    fun createFormatRequest(
        userId: String,
        request: FormatUniqueInput,
    ): FormatUniqueInputForEngine {
        log.info("Creating format request for userId: $userId, snippetId: ${request.snippetId}")
        try {
            val config = getFormatConfigForUser(userId)
            val snippet = snippetRepository.getSnippetById(request.snippetId)
            if (snippet === null) {
                log.warn("Snippet not found with id: ${request.snippetId} for userId: $userId")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $request.snippetId")
            }
            return FormatUniqueInputForEngine(
                code = request.code,
                language = snippet.language.name,
                version = snippet.version,
                config = config,
            )
        } catch (e: Exception) {
            log.warn("Error creating format request for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    private fun checkLintingRules(config: AnalyzeCodeRequest) {
        if (config.namingConvention != "camelCase" && config.namingConvention != "snake_case" && config.namingConvention != " ") {
            log.warn("Invalid naming convention: ${config.namingConvention}")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid naming convention: ${config.namingConvention}")
        }
    }

    private fun checkFormattingRules(config: FormatCodeRequest) {
        if (config.enforceNoSpacingAroundEquals && config.enforceSpacingAroundEquals) {
            log.warn("Conflicting rules: enforceNoSpacingAroundEquals and enforceSpacingAroundEquals")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Conflicting rules for spacing around equals")
        }
        if (config.mandatorySingleSpaceSeparation && config.mandatorySpaceSurroundingOperations) {
            log.warn("Conflicting rules: mandatorySingleSpaceSeparation and mandatorySpaceSurroundingOperations")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Conflicting rules for spacing around operations")
        }
    }

    fun lintUniqueWithPath(
        userId: String,
        path: String,
        language: String,
        version: String,
    ): LintResponse {
        log.info("Linting unique snippet at path: $path for userId: $userId")
        try {
            val lintConfig = getLintConfigForUser(userId)
            val lintErrors =
                engineService.lintUnique(
                    assetPath = path,
                    config = lintConfig,
                    language = language,
                    version = version,
                )
            log.info("Linting completed for path: $path, found ${lintErrors.lintErrors.size} errors")
            return lintErrors
        } catch (e: Exception) {
            log.warn("Error linting snippet at path: $path for userId: $userId - ${e.message}", e)
            throw e
        }
    }
}
