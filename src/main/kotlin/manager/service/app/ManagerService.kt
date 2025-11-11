package manager.service.app

import manager.entity.CompilantState
import manager.entity.Languages
import manager.entity.Snippet
import manager.inputs.snippet.CreateSnippetRequest
import manager.inputs.snippet.RunSnippetRequest
import manager.inputs.snippet.ShareSnippetRequest
import manager.inputs.snippet.UpdateSnippetRequest
import manager.inputs.snippet.UpdateSnippetStateRequest
import manager.outputs.PaginationResponse
import manager.outputs.snippet.CreateSnippetResponse
import manager.outputs.snippet.GetPaginatedSnippetsResponse
import manager.outputs.snippet.RunSnippetResponse
import manager.outputs.snippet.SnippetResponse
import manager.repository.snippet.SnippetRepositoryInterface
import manager.repository.snippet.deleted.DeletedSnippetRepositoryInterface
import manager.service.asset.AssetServiceInterface
import manager.service.authorization.AuthorizationServiceInterface
import manager.service.engine.EngineServiceInterface
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Locale
import java.util.UUID

@Service
class ManagerService(
    private val snippetRepository: SnippetRepositoryInterface,
    private val assetService: AssetServiceInterface,
    private val authorizationService: AuthorizationServiceInterface,
    private val engineService: EngineServiceInterface,
    private val deletedSnippetRepository: DeletedSnippetRepositoryInterface,
    private val configSerivice: ConfigService,
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
                language = request.language.uppercase(Locale.getDefault()),
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

        if (!errors.isEmpty()) {
            snippetRepository.deleteSnippet(result)
            deleteSnippetFromBucket(result, userId)
            return CreateSnippetResponse(
                snippetId = "",
                errorMessage = errors,
            )
        }

        val lintErrors =
            configSerivice.lintUniqueWithPath(
                userId = userId,
                path = "/v1/asset/${userId.substringAfter("|")}/$result",
                language = request.language,
                version = request.version,
            )

        if (lintErrors.lintErrors.isNotEmpty()) {
            snippetRepository.setSnippetState(
                snippetId = result,
                state = CompilantState.NON_COMPILANT,
            )
        } else {
            snippetRepository.setSnippetState(
                snippetId = result,
                state = CompilantState.COMPILANT,
            )
        }

        authorizationService.grantWritePermises(userToken, userId, result)

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
        validateLanguageAndVersion(request.language, request.version)
        val code =
            assetService.getAsset(
                userId.substringAfter("|"),
                snippet.id,
            )

        saveOrUpdateSnippetInBucket(snippet.id + "lint", request.snippet, userId)
        val errors = engineService.validateSnippet("/v1/asset/${userId.substringAfter("|")}/${snippet.id + "lint"}", request.version, request.language)
        if (errors.isNotEmpty()) {
            deleteSnippetFromBucket(snippet.id + "lint", userId)
            return CreateSnippetResponse(
                snippetId = request.snippetId,
                errorMessage = errors,
            )
        }
        deleteSnippetFromBucket(snippet.id + "lint", userId)
        saveOrUpdateSnippetInBucket(snippet.id, request.snippet, userId)

        val lintErrors =
            configSerivice.lintUniqueWithPath(
                userId = userId,
                path = "/v1/asset/${userId.substringAfter("|")}/${snippet.id}",
                language = request.language,
                version = request.version,
            )

        if (lintErrors.lintErrors.isNotEmpty()) {
            snippetRepository.setSnippetState(
                snippetId = snippet.id,
                state = CompilantState.NON_COMPILANT,
            )
        } else {
            snippetRepository.setSnippetState(
                snippetId = snippet.id,
                state = CompilantState.COMPILANT,
            )
        }

        val updatedSnippetId =
            snippetRepository.updateSnippet(
                snippetId = request.snippetId,
                name = request.name,
                language = request.language.uppercase(Locale.getDefault()),
                description = request.description,
                version = request.version,
            )

        return CreateSnippetResponse(
            snippetId = updatedSnippetId,
            errorMessage = errors,
        )
    }

    fun getSnippet(
        snippetId: String,
        userId: String,
    ): SnippetResponse {
        var snippet =
            snippetRepository.getSnippetById(snippetId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $snippetId")
        val code =
            assetService.getAsset(
                userId.substringAfter("|"),
                snippet.id,
            )
        return SnippetResponse(
            id = snippet.id,
            name = snippet.name,
            description = snippet.description,
            snippet = code,
            language = snippet.language.name,
            version = snippet.version,
            compliance = snippet.state.name,
            author = snippet.userId,
        )
    }

    fun getAllSnippets(
        userId: String,
        page: Int,
        pageSize: Int,
    ): GetPaginatedSnippetsResponse {
        val snippets = snippetRepository.getPaginatedSnippetsByUserId(userId, page, pageSize)
        val amountOfSnippets = snippetRepository.countSnippetsByUserId(userId)

        val snippetsList = mutableListOf<SnippetResponse>()

        for (snippet in snippets) {
            val code =
                try {
                    assetService.getAsset(
                        userId.substringAfter("|"),
                        snippet.id,
                    )
                } catch (e: Exception) {
                    "Unable to retrieve snippet code"
                }

            snippetsList.add(
                SnippetResponse(
                    id = snippet.id,
                    name = snippet.name,
                    description = snippet.description,
                    snippet = code,
                    language = snippet.language.name,
                    version = snippet.version,
                    author = snippet.userId,
                    compliance = snippet.state.name,
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

    fun changeSnippetState(
        input: UpdateSnippetStateRequest,
    ) {
        validateSnippetExists(input.snippetId)
        snippetRepository.setSnippetState(
            snippetId = input.snippetId,
            state = input.state,
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
            assetService.deleteAsset(userId.substringAfter("|"), snippetName)
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
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Not supported version: $version for language: $language",
            )
        }
    }

    private fun validateSnippetExists(snippetId: String): Snippet {
        val snippet =
            snippetRepository.getSnippetById(snippetId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $snippetId")
        return snippet
    }
}
