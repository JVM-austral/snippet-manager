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
        @Valid @RequestBody request: IdTestRequest,
    ): ResponseEntity<String> {
        val result = testService.runTest(request, userId, userToken)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping
    fun deleteTest(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: IdTestRequest,
    ): ResponseEntity<String> {
        testService.deleteTest(request, userId, userToken)
        return ResponseEntity.ok("Test deleted successfully")
    }

    @PutMapping("/edit")
    fun updateTest(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: EditTestRequest,
    ): ResponseEntity<String> {
        testService.editTest(request, userId, userToken)
        return ResponseEntity.ok("Test updated successfully")
    }

    @GetMapping("/{snippetId}")
    fun getAllTestsForSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @PathVariable snippetId: String,
    ): ResponseEntity<List<TestEntity>> {
        val response = testService.getAllTestsBySnippetId(snippetId, userId, userToken)
        return ResponseEntity.ok(response)
    }
}
