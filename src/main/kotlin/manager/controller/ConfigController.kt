package manager.controller

import jakarta.validation.Valid
import manager.inputs.config.AnalyzeCodeRequest
import manager.inputs.config.FormatCodeRequest
import manager.inputs.snippet.FormatUniqueInput
import manager.redis.FormatStreamProducer
import manager.redis.LintStreamProducer
import manager.repository.format.FormatConfig
import manager.repository.lint.LintConfig
import manager.security.CurrentUserId
import manager.service.app.ConfigService
import manager.service.engine.EngineServiceInterface
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/config")
class ConfigController(
    private val configService: ConfigService,
    private val formatProducer: FormatStreamProducer,
    private val lintProducer: LintStreamProducer,
    private val engineService: EngineServiceInterface,
) {
    @PostMapping("/save-linting")
    fun setLintingConfig(
        @CurrentUserId userId: String,
        @Valid @RequestBody request: AnalyzeCodeRequest,
    ): ResponseEntity<String> {
        configService.saveLintingConfig(userId, request)
        val requests = configService.getListOfLintRequests(userId)
        val recordId = lintProducer.emitAll(requests)
        return ResponseEntity.ok("Evento publicado con id: $recordId")
    }

    @PostMapping("/save-formatting")
    fun setFormatConfig(
        @CurrentUserId userId: String,
        @Valid @RequestBody request: FormatCodeRequest,
    ): ResponseEntity<String> {
        configService.saveFormatConfig(userId, request)
        val requests = configService.getListOfFormatRequests(userId)
        val recordId = formatProducer.emitAll(requests)
        return ResponseEntity.ok("Evento publicado con id: $recordId")
    }

    @GetMapping("/format")
    fun getFormatConfig(
        @CurrentUserId userId: String,
    ): ResponseEntity<FormatConfig> {
        val result = configService.getFormatConfigForUser(userId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/linting")
    fun getLintingConfig(
        @CurrentUserId userId: String,
    ): ResponseEntity<LintConfig> {
        val result = configService.getLintConfigForUser(userId)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/format-unique")
    fun formatCode(
        @CurrentUserId userId: String,
        @Valid @RequestBody request: FormatUniqueInput,
    ): ResponseEntity<String> {
        val formatRequest = configService.createFormatRequest(userId = userId, request = request)
        val formatted = engineService.formatUnique(formatRequest)
        return ResponseEntity.ok(formatted)
    }
}
