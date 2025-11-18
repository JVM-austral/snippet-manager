package manager.controller

import jakarta.validation.Valid
import manager.common.interceptor.CurrentUserToken
import manager.entity.TestEntity
import manager.inputs.testing.CreateTestRequest
import manager.inputs.testing.EditTestRequest
import manager.inputs.testing.IdTestRequest
import manager.security.CurrentUserId
import manager.service.app.TestingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/testing")
class TestingController(
    private val testService: TestingService,
) {
    private val log = org.slf4j.LoggerFactory.getLogger(TestingController::class.java)

    @PostMapping("/save")
    fun createOrUpdateTest(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: CreateTestRequest,
    ): ResponseEntity<String> {
        log.info("Received createOrUpdateTest request from userId: $userId for id: ${request.snippetId}")
        val result = testService.createTest(request, userId, userToken)
        log.info("Test created with ID: $result for userId: $userId")
        return ResponseEntity.ok(result)
    }

    @PostMapping("/run")
    fun runTest(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: IdTestRequest,
    ): ResponseEntity<String> {
        log.info("Received runTest request from userId: $userId for testId: ${request.testId}")
        val result = testService.runTest(request, userId, userToken)
        log.info("Test executed for testId: ${request.testId} by userId: $userId")
        return ResponseEntity.ok(result)
    }

    @DeleteMapping
    fun deleteTest(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: IdTestRequest,
    ): ResponseEntity<String> {
        log.info("Received deleteTest request from userId: $userId for testId: ${request.testId}")
        testService.deleteTest(request, userId, userToken)
        log.info("Test deleted with ID: ${request.testId} by userId: $userId")
        return ResponseEntity.ok("Test deleted successfully")
    }

    @PutMapping("/edit")
    fun updateTest(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: EditTestRequest,
    ): ResponseEntity<String> {
        log.info("Received updateTest request from userId: $userId for testId: ${request.testId}")
        testService.editTest(request, userId, userToken)
        log.info("Test updated with ID: ${request.testId} by userId: $userId")
        return ResponseEntity.ok("Test updated successfully")
    }

    @GetMapping("/{snippetId}")
    fun getAllTestsForSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @PathVariable snippetId: String,
    ): ResponseEntity<List<TestEntity>> {
        log.info("Received getAllTestsForSnippet request from userId: $userId for id: $snippetId")
        val response = testService.getAllTestsBySnippetId(snippetId, userId, userToken)
        log.info("Retrieved ${response.size} tests for id: $snippetId by userId: $userId")
        return ResponseEntity.ok(response)
    }
}
