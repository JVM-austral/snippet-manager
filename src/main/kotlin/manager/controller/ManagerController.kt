package manager.controller

import jakarta.validation.Valid
import manager.common.interceptor.CurrentUserToken
import manager.entity.Snippet
import manager.inputs.CreateSnippetRequest
import manager.inputs.UpdateSnippetRequest
import manager.outputs.CreateSnippetResponse
import manager.security.CurrentUserId
import manager.service.ManagerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/snippets")
class ManagerController(
    private val snippetService: ManagerService,
) {
    @PostMapping
    fun createSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: CreateSnippetRequest,
    ): ResponseEntity<CreateSnippetResponse> {
        val result = snippetService.createSnippet(request, userId, userToken)
        return ResponseEntity.ok(result)
    }

    @PatchMapping
    fun updateSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: UpdateSnippetRequest,
    ): ResponseEntity<CreateSnippetResponse> {
        val result = snippetService.updateSnippet(request, userId, userToken)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{snippetId}")
    fun getSnippet(
        @CurrentUserId userId: String,
        @PathVariable snippetId: String,
    ): ResponseEntity<Snippet> {
        val result = snippetService.getSnippet(snippetId, userId)
        return ResponseEntity.ok(result)
    }

    @GetMapping
    fun getAllSnippets(
        @CurrentUserId userId: String,
    ): ResponseEntity<List<Snippet>> {
        val result = snippetService.getAllSnippets(userId)
        return ResponseEntity.ok(result)
    }
}
