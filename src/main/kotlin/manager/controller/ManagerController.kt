package manager.controller

import jakarta.validation.Valid
import manager.common.interceptor.CurrentUserToken
import manager.inputs.snippet.CreateSnippetRequest
import manager.inputs.snippet.RunSnippetRequest
import manager.inputs.snippet.ShareSnippetRequest
import manager.inputs.snippet.UpdateSnippetRequest
import manager.inputs.snippet.UpdateSnippetStateRequest
import manager.outputs.snippet.CreateSnippetResponse
import manager.outputs.snippet.GetPaginatedSnippetsResponse
import manager.outputs.snippet.RunSnippetResponse
import manager.outputs.snippet.SnippetResponse
import manager.security.CurrentUserId
import manager.service.app.ManagerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/snippets")
class ManagerController(
    private val managerService: ManagerService,
) {
    @PostMapping
    fun createSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: CreateSnippetRequest,
    ): ResponseEntity<CreateSnippetResponse> {
        val result = managerService.createSnippet(request, userId, userToken)
        return ResponseEntity.ok(result)
    }

    @PatchMapping
    fun updateSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: UpdateSnippetRequest,
    ): ResponseEntity<CreateSnippetResponse> {
        val result = managerService.updateSnippet(request, userId, userToken)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{snippetId}")
    fun getSnippet(
        @CurrentUserId userId: String,
        @PathVariable snippetId: String,
    ): ResponseEntity<SnippetResponse> {
        val result = managerService.getSnippet(snippetId, userId)
        return ResponseEntity.ok(result)
    }

    @GetMapping
    fun getAllSnippets(
        @CurrentUserId userId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(name = "page_size", defaultValue = "10") pageSize: Int,
    ): ResponseEntity<GetPaginatedSnippetsResponse> {
        val result = managerService.getAllSnippets(userId, page, pageSize)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/share")
    fun shareSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: ShareSnippetRequest,
    ): ResponseEntity<String> {
        managerService.shareSnippet(request, userId, userToken)
        return ResponseEntity.ok("Snippet shared successfully")
    }

    @PostMapping("/run")
    fun runSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @Valid @RequestBody request: RunSnippetRequest,
    ): ResponseEntity<RunSnippetResponse> {
        val output = managerService.runSnippet(request, userId, userToken)
        return ResponseEntity.ok(output)
    }

    @DeleteMapping("/{snippetId}")
    fun deleteSnippet(
        @CurrentUserId userId: String,
        @CurrentUserToken userToken: String,
        @PathVariable snippetId: String,
    ): ResponseEntity<String> {
        managerService.deleteSnippet(snippetId, userId, userToken)
        return ResponseEntity.ok("Snippet deleted successfully")
    }

    @PutMapping("/compiling-state")
    fun changeSnippetState(
        @Valid @RequestBody request: UpdateSnippetStateRequest,
    ): ResponseEntity<String> {
        managerService.changeSnippetState(request)
        return ResponseEntity.ok("Snippet state updated successfully")
    }
}
