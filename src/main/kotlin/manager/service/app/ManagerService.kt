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
import manager.outputs.snippet.LanguagesResponse
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
    private val log = org.slf4j.LoggerFactory.getLogger(ManagerService::class.java)

    fun createSnippet(
        request: CreateSnippetRequest,
        userId: String,
        userToken: String,
    ): CreateSnippetResponse {
        log.info("Creating snippet for userId: $userId with name: ${request.name}")
        try {
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
            log.info("Snippet saved with ID: $result for userId: $userId")

            saveOrUpdateSnippetInBucket(result, request.snippet, userId)
            snippetRepository.updateBucketIdForSnippets(
                snippetId = result,
                newBucketId = "/v1/asset/${userId.substringAfter("|")}/$result",
            )

            val errors = engineService.validateSnippet("/v1/asset/${userId.substringAfter("|")}/$result", request.version, request.language)

            if (!errors.isEmpty()) {
                log.warn("Validation errors for snippet $result: $errors")
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
                log.info("Snippet $result has linting errors, setting state to NON_COMPILANT")
                snippetRepository.setSnippetState(
                    snippetId = result,
                    state = CompilantState.NON_COMPILANT,
                )
            } else {
                log.info("Snippet $result is compilant")
                snippetRepository.setSnippetState(
                    snippetId = result,
                    state = CompilantState.COMPILANT,
                )
            }

            val persistedData = getSnippet(result, userId)

            authorizationService.grantWritePermises(userToken, userId, result)
            log.info("Successfully created snippet with ID: $result for userId: $userId")

            return CreateSnippetResponse(
                snippetId = result,
                name = persistedData.name,
                description = persistedData.description,
                language = persistedData.language,
                version = persistedData.version,
                errorMessage = errors,
            )
        } catch (e: Exception) {
            log.warn("Error creating snippet for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    fun updateSnippet(
        request: UpdateSnippetRequest,
        userId: String,
        userToken: String,
    ): CreateSnippetResponse {
        log.info("Updating snippet ${request.snippetId} for userId: $userId")
        try {
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
                log.warn("Validation errors for snippet update ${request.snippetId}: $errors")
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
                log.info("Updated snippet ${snippet.id} has linting errors")
                snippetRepository.setSnippetState(
                    snippetId = snippet.id,
                    state = CompilantState.NON_COMPILANT,
                )
            } else {
                log.info("Updated snippet ${snippet.id} is compilant")
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

            log.info("Successfully updated snippet $updatedSnippetId for userId: $userId")
            return CreateSnippetResponse(
                snippetId = updatedSnippetId,
                errorMessage = errors,
            )
        } catch (e: Exception) {
            log.warn("Error updating snippet ${request.snippetId} for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    fun getSnippet(
        snippetId: String,
        userId: String,
    ): SnippetResponse {
        log.info("Getting snippet $snippetId for userId: $userId")
        try {
            val snippet =
                snippetRepository.getSnippetById(snippetId)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $snippetId")
            val code =
                assetService.getAsset(
                    userId.substringAfter("|"),
                    snippet.id,
                )
            log.info("Successfully retrieved snippet $snippetId for userId: $userId")
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
        } catch (e: ResponseStatusException) {
            log.warn("Snippet not found: $snippetId for userId: $userId")
            throw e
        } catch (e: Exception) {
            log.warn("Error getting snippet $snippetId for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    fun getAllSnippets(
        userId: String,
        page: Int,
        pageSize: Int,
        filter: String? = null,
    ): GetPaginatedSnippetsResponse {
        log.info("Getting all snippets for userId: $userId (page: $page, pageSize: $pageSize)")
        try {
            val (snippets, amountOfSnippets) =
                if (filter.isNullOrBlank()) {
                    val list = snippetRepository.getPaginatedSnippetsByUserId(userId, page, pageSize)
                    Pair(list, snippetRepository.countSnippetsByUserId(userId))
                } else {
                    val list = snippetRepository.getPaginatedSnippetsByUserIdAndFilter(userId, page, pageSize, filter)
                    Pair(list, snippetRepository.countSnippetsByUserIdWithFilter(userId, filter))
                }

            val snippetsList = mutableListOf<SnippetResponse>()

            for (snippet in snippets) {
                val code =
                    try {
                        assetService.getAsset(
                            userId.substringAfter("|"),
                            snippet.id,
                        )
                    } catch (e: Exception) {
                        log.warn("Unable to retrieve code for snippet ${snippet.id}: ${e.message}")
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

            log.info("Successfully retrieved ${snippetsList.size} snippets for userId: $userId")
            return GetPaginatedSnippetsResponse(
                snippets = snippetsList,
                pagination =
                    PaginationResponse(
                        page = page,
                        pageSize = pageSize,
                        count = amountOfSnippets,
                    ),
            )
        } catch (e: Exception) {
            log.warn("Error getting snippets for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    fun deleteSnippet(
        snippetId: String,
        userId: String,
        userToken: String,
    ) {
        log.info("Deleting snippet $snippetId for userId: $userId")
        try {
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
            log.info("Successfully deleted snippet $snippetId for userId: $userId")
        } catch (e: Exception) {
            log.warn("Error deleting snippet $snippetId for userId: $userId - ${e.message}", e)
            throw e
        }
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
        log.info("Sharing snippet ${input.snippetId} from userId: $userId to targetUserId: ${input.targetUserId}")
        try {
            authorizationService.checkWritePermises(userToken, userId, input.snippetId)
            validateSnippetExists(input.snippetId)
            authorizationService.grantReadPermises(userToken, input.targetUserId, input.snippetId)
            log.info("Successfully shared snippet ${input.snippetId} to ${input.targetUserId}")
        } catch (e: Exception) {
            log.warn("Error sharing snippet ${input.snippetId} - ${e.message}", e)
            throw e
        }
    }

    fun runSnippet(
        input: RunSnippetRequest,
        userId: String,
        userToken: String,
    ): RunSnippetResponse {
        log.info("Running snippet ${input.snippetId} for userId: $userId")
        try {
            val snippet = validateSnippetExists(input.snippetId)
            authorizationService.checkReadPermises(userToken, userId, input.snippetId)

            val response =
                engineService.runSnippet(
                    snippet.bucketId,
                    snippet.version,
                    snippet.language.name,
                    input.varInputs,
                )
            log.info("Successfully ran snippet ${input.snippetId} for userId: $userId")
            return response
        } catch (e: Exception) {
            log.warn("Error running snippet ${input.snippetId} for userId: $userId - ${e.message}", e)
            throw e
        }
    }

    fun changeSnippetState(
        input: UpdateSnippetStateRequest,
    ) {
        log.info("Changing snippet state for snippetId: ${input.snippetId} to state: ${input.state}")
        try {
            validateSnippetExists(input.snippetId)
            snippetRepository.setSnippetState(
                snippetId = input.snippetId,
                state = input.state,
            )
            log.info("Successfully changed state for snippet ${input.snippetId}")
        } catch (e: Exception) {
            log.warn("Error changing state for snippet ${input.snippetId} - ${e.message}", e)
            throw e
        }
    }

    fun getSupportedLanguages(): List<LanguagesResponse> {
        log.info("Getting supported versions for all languages")
        try {
            val supportedLanguages =
                Languages.entries.map {
                    LanguagesResponse(
                        displayName = it.displayName,
                        versions = it.versions,
                        extension = it.extension,
                    )
                }
            log.info("Successfully retrieved supported language names")
            return supportedLanguages
        } catch (e: Exception) {
            log.warn("Error getting supported versions - ${e.message}", e)
            throw e
        }
    }

    private fun saveOrUpdateSnippetInBucket(
        snippetName: String,
        snippetContent: String,
        userId: String,
    ) {
        try {
            assetService.createAsset(userId.substringAfter("|"), "$snippetName", snippetContent)
        } catch (e: Exception) {
            log.warn("Error saving snippet to bucket: ${e.message}", e)
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
