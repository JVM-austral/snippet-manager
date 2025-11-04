package manager.controller

import jakarta.validation.Valid
import manager.common.interceptor.CurrentUserToken
import manager.inputs.AnalyzeCodeRequest
import manager.security.CurrentUserId
import manager.service.ConfigService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/config")
class ConfigController(
    private val configService: ConfigService,
) {
    @PostMapping("/linting")
    fun setLintingConfig(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: AnalyzeCodeRequest,
    ): ResponseEntity<String> = ResponseEntity.ok("")
}
