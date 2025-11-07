package manager.controller

import jakarta.validation.Valid
import manager.inputs.config.AnalyzeCodeRequest
import manager.inputs.config.FormatCodeRequest
import manager.redis.FormatStreamProducer
import manager.security.CurrentUserId
import manager.service.app.ConfigService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/config")
class ConfigController(
    private val configService: ConfigService,
    private val producer: FormatStreamProducer,
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
}
