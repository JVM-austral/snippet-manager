package manager.service

import manager.entity.CompilantState
import manager.entity.Languages
import manager.entity.Snippet
import manager.inputs.snippet.CreateSnippetRequest
import manager.inputs.snippet.RunSnippetRequest
import manager.inputs.snippet.ShareSnippetRequest
import manager.inputs.snippet.UpdateSnippetRequest
import manager.outputs.PaginationResponse
import manager.outputs.snippet.CreateSnippetResponse
import manager.outputs.snippet.GetPaginatedSnippetsResponse
import manager.outputs.snippet.RunSnippetResponse
import manager.outputs.snippet.SnippetResponse
import manager.repository.snippet.SnippetRepositoryInterface
import manager.repository.snippet.deleted.DeletedSnippetRepositoryInterface
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
    private val deletedSnippetRepository: DeletedSnippetRepositoryInterface,
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
        val updatedSnippetId =
            snippetRepository.updateSnippet(
                snippetId = request.snippetId,
                name = request.name,
                language = request.language.uppercase(getDefault()),
                description = request.description,
                version = request.version,
            )
        saveOrUpdateSnippetInBucket(updatedSnippetId, request.snippet, userId)

        val errors = engineService.validateSnippet(snippet.bucketId, request.version, request.language)
        if (errors.isNotEmpty()) {
            snippetRepository.setSnippetState(
                snippetId = updatedSnippetId,
                state = CompilantState.NON_COMPILANT,
            )
            return CreateSnippetResponse(
                snippetId = updatedSnippetId,
                errorMessage = errors,
            )
        }
        snippetRepository.setSnippetState(
            snippetId = updatedSnippetId,
            state = CompilantState.COMPILANT,
        )
        return CreateSnippetResponse(
            snippetId = updatedSnippetId,
            errorMessage = errors,
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
        page: Int,
        pageSize: Int,
    ): GetPaginatedSnippetsResponse {
        val snippets =
            snippetRepository.getPaginatedSnippetsByUserId(userId, page, pageSize)
        val amountOfSnippets =
            snippetRepository.countSnippetsByUserId(userId)

        val snippetsList = mutableListOf<SnippetResponse>()
        for (snippet in snippets) {
            val code =
                assetService.getAsset(
                    userId.substringAfter("|"),
                    snippet.id,
                )
            snippetsList.add(
                SnippetResponse(
                    id = snippet.id,
                    name = snippet.name,
                    description = snippet.description,
                    snippet = code,
                    language = snippet.language.name,
                    version = snippet.version,
                    author = snippet.userId,
                    compliance= snippet.state.name,
                ),
            )
        }

        return GetPaginatedSnippetsResponse(
            snippets = snippetsList,
            pagination =
                PaginationResponse(
                    page = page,
                    pageSize = pageSize,
                    count = amountOfSnippets,
                ),
        )
    }

    fun deleteSnippet(
        snippetId: String,
        userId: String,
        userToken: String,
    ) {
        authorizationService.checkWritePermises(userToken, userId, snippetId)
        val snippet = validateSnippetExists(snippetId)
        snippetRepository.deleteSnippet(snippetId)
        deletedSnippetRepository.saveDeletedSnippet(
            id = snippet.id,
            name = snippet.name,
            bucketId = snippet.bucketId,
            language = snippet.language.name,
            description = snippet.description,
            version = snippet.version,
            userId = snippet.userId,
            creationDate = snippet.creationDate.toString(),
            compilantState = snippet.state,
        )

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
