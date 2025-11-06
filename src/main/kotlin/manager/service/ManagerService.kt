package manager.service

import manager.entity.CompilantState
import manager.entity.Languages
import manager.entity.Snippet
import manager.inputs.snippet.CreateSnippetRequest
import manager.inputs.snippet.RunSnippetRequest
import manager.inputs.snippet.ShareSnippetRequest
import manager.inputs.snippet.UpdateSnippetRequest
import manager.outputs.snippet.CreateSnippetResponse
import manager.outputs.snippet.RunSnippetResponse
import manager.repository.snippet.SnippetRepositoryInterface
import manager.service.engine.EngineService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Locale.getDefault
import java.util.UUID

@Service
class ManagerService(
    private val snippetRepository: SnippetRepositoryInterface,
    private val assetService: AssetService,
    private val authorizationService: AuthorizationService,
    private val engineService: EngineService,
) {
    fun createSnippet(
        request: CreateSnippetRequest,
        userId: String,
        userToken: String,
    ): CreateSnippetResponse {
        validateLanguageAndVersion(request.language, request.version)
        val result =
            snippetRepository.saveSnippet(
                bucketId = "",
                language = request.language.uppercase(getDefault()),
                name = request.name,
                description = request.description,
                version = request.version,
                userId = userId,
            )
        saveOrUpdateSnippetInBucket(result, request.snippet, userId)
        snippetRepository.updateBucketIdForSnippets(
            snippetId = result,
            newBucketId = "/v1/asset/${userId.substringAfter("|")}/$result",
        )

        val errors = engineService.validateSnippet("/v1/asset/${userId.substringAfter("|")}/$result", request.version, request.language)

        authorizationService.grantWritePermises(userToken, userId, result)

        if (errors.isNotEmpty()) {
            snippetRepository.setSnippetState(
                snippetId = result,
                state = CompilantState.NON_COMPILANT,
            )
            return CreateSnippetResponse(
                snippetId = result,
                errorMessage = errors,
            )
        }
        snippetRepository.setSnippetState(
            snippetId = result,
            state = CompilantState.COMPILANT,
        )
        return CreateSnippetResponse(
            snippetId = result,
            errorMessage = errors,
        )
    }

    fun updateSnippet(
        request: UpdateSnippetRequest,
        userId: String,
        userToken: String,
    ): CreateSnippetResponse {
        authorizationService.checkWritePermises(userToken, userId, request.snippetId)
        val snippet = validateSnippetExists(request.snippetId)

        val errors = engineService.validateSnippet(snippet.bucketId, request.version, request.language)
        if (errors.isNotEmpty()) {
            return CreateSnippetResponse(
                snippetId = request.snippetId,
                errorMessage = errors,
            )
        }

        val updatedSnippetId =
            snippetRepository.updateSnippet(
                snippetId = request.snippetId,
                name = request.name,
                language = request.language.uppercase(getDefault()),
                description = request.description,
                version = request.version,
            )
        saveOrUpdateSnippetInBucket(updatedSnippetId, request.snippet, userId)
        return CreateSnippetResponse(
            snippetId = updatedSnippetId,
            errorMessage = emptyList(),
        )
    }

    fun getSnippet(
        snippetId: String,
        userId: String,
    ): Snippet {
        var snippet =
            snippetRepository.getSnippetById(snippetId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $snippetId")
        val code =
            assetService.getAsset(
                userId.substringAfter("|"),
                snippet.id,
            )
        snippet.bucketId = code
        return snippet
    }

    fun getAllSnippets(
        userId: String,
    ): List<Snippet> {
        val snippets =
            snippetRepository.getAllSnippetsByUserId(userId)

        for (snippet in snippets) {
            val code =
                assetService.getAsset(
                    userId.substringAfter("|"),
                    snippet.id,
                )
            snippet.bucketId = code
        }

        return snippets
    }

    private fun validateUUID(id: String) {
        try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid UUID format")
        }
    }

    fun shareSnippet(
        input: ShareSnippetRequest,
        userId: String,
        userToken: String,
    ) {
        authorizationService.checkWritePermises(userToken, userId, input.snippetId)
        validateSnippetExists(input.snippetId)
        authorizationService.grantReadPermises(userToken, input.targetUserId, input.snippetId)
    }

    fun runSnippet(
        input: RunSnippetRequest,
        userId: String,
        userToken: String,
    ): RunSnippetResponse {
        val snippet = validateSnippetExists(input.snippetId)
        authorizationService.checkReadPermises(userToken, userId, input.snippetId)

        return engineService.runSnippet(
            snippet.bucketId,
            snippet.version,
            snippet.language.name,
            input.varInputs,
        )
    }

    private fun saveOrUpdateSnippetInBucket(
        snippetName: String,
        snippetContent: String,
        userId: String,
    ) {
        try {
            assetService.createAsset(userId.substringAfter("|"), "$snippetName", snippetContent)
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Error saving snippet in bucket: ${e.message}",
                e,
            )
        }
    }

    private fun deleteSnippetFromBucket(
        snippetName: String,
        userId: String,
    ) {
        try {
            assetService.deleteAsset(userId, snippetName)
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Error deleting snippet from bucket: ${e.message}",
                e,
            )
        }
    }

    private fun validateLanguageAndVersion(
        language: String,
        version: String,
    ) {
        val lang =
            Languages.entries.find {
                it.name.equals(language, ignoreCase = true) ||
                    it.displayName.equals(language, ignoreCase = true)
            } ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid language: $language")

        if (version !in lang.versions) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not supported version: $version for language: $language")
        }
    }

    private fun validateSnippetExists(snippetId: String): Snippet {
        val snippet =
            snippetRepository.getSnippetById(snippetId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $snippetId")
        return snippet
    }
}
