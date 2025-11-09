package manager.controller

import jakarta.validation.Valid
import manager.inputs.config.AnalyzeCodeRequest
import manager.inputs.config.FormatCodeRequest
import manager.inputs.snippet.FormatUniqueInput
import manager.redis.FormatStreamProducer
import manager.repository.format.FormatConfig
import manager.security.CurrentUserId
import manager.service.app.ConfigService
import manager.service.engine.EngineService
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
    private val producer: FormatStreamProducer,
    private val engineService: EngineService,
) {
    @PostMapping("/save-linting")
    fun setLintingConfig(
        @CurrentUserId userId: String,
        @Valid @RequestBody request: AnalyzeCodeRequest,
    ): ResponseEntity<String> {
        configService.saveLintingConfig(userId, request)
        return ResponseEntity.ok("")
    }

    @PostMapping("/save-formatting")
    fun setFormatConfig(
        @CurrentUserId userId: String,
        @Valid @RequestBody request: FormatCodeRequest,
    ): ResponseEntity<String> {
        configService.saveFormatConfig(userId, request)
        return ResponseEntity.ok("")
    }

    @PostMapping("/format")
    fun createProduct(
        @CurrentUserId userId: String,
    ): ResponseEntity<String> {
        val requests = configService.getListOfFormatRequests(userId)
        val recordId = producer.emitAll(requests)
        return ResponseEntity.ok("Evento publicado con id: $recordId")
    }

    @GetMapping
    fun getFormatConfig(
        @CurrentUserId userId: String,
    ): ResponseEntity<FormatConfig> {
        val result = configService.getFormatConfigForUser(userId)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/format-unique")
    fun formatCode(
        @CurrentUserId userId: String,
        @Valid @RequestBody request: FormatUniqueInput,
    ): ResponseEntity<String> {
        val formatRequest = configService.createFormatRequest( userId = userId, request = request )
        val formatted = engineService.formatUnique(formatRequest)
        return ResponseEntity.ok(formatted)
    }
}
