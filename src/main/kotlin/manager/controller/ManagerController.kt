package manager.controller

import jakarta.validation.Valid
import manager.common.interceptor.CurrentUserToken
import manager.entity.Snippet
import manager.inputs.snippet.CreateSnippetRequest
import manager.inputs.snippet.RunSnippetRequest
import manager.inputs.snippet.ShareSnippetRequest
import manager.inputs.snippet.UpdateSnippetRequest
import manager.outputs.snippet.CreateSnippetResponse
import manager.outputs.snippet.GetPaginatedSnippetsResponse
import manager.outputs.snippet.RunSnippetResponse
import manager.security.CurrentUserId
import manager.service.app.ManagerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(name = "page_size", defaultValue = "10") pageSize: Int,
    ): ResponseEntity<GetPaginatedSnippetsResponse> {
        val result = snippetService.getAllSnippets(userId, page, pageSize)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/share")
    fun shareSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: ShareSnippetRequest,
    ): ResponseEntity<String> {
        snippetService.shareSnippet(request, userId, userToken)
        return ResponseEntity.ok("Snippet shared successfully")
    }

    @PostMapping("/run")
    fun runSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: RunSnippetRequest,
    ): ResponseEntity<RunSnippetResponse> {
        val output = snippetService.runSnippet(request, userId, userToken)
        return ResponseEntity.ok(output)
    }

    @DeleteMapping("/{snippetId}")
    fun deleteSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @PathVariable snippetId: String,
    ): ResponseEntity<String> {
        snippetService.deleteSnippet(snippetId, userId, userToken)
        return ResponseEntity.ok("Snippet deleted successfully")
    }
}
