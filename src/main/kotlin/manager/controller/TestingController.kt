package manager.controller

import jakarta.validation.Valid
import manager.common.interceptor.CurrentUserToken
import manager.inputs.testing.CreateTestRequest
import manager.inputs.testing.RunTestRequest
import manager.security.CurrentUserId
import manager.service.TestingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/testing")
class TestingController(
    private val testService: TestingService,
) {
    @PostMapping("/save")
    fun createTest(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: CreateTestRequest,
    ): ResponseEntity<String> {
        val result = testService.createTest(request, userId, userToken)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/run")
    fun runTest(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: RunTestRequest,
    ): ResponseEntity<String> {
        val result = testService.runTest(request, userId, userToken)
        return ResponseEntity.ok(result)
    }
}
