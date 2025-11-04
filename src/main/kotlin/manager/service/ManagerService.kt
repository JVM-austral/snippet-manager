package manager.service

import manager.entity.Languages
import manager.entity.Snippet
import manager.inputs.snippet.CreateSnippetRequest
import manager.inputs.snippet.ParseRequest
import manager.inputs.snippet.PermissionRequest
import manager.inputs.snippet.RunSnippetInEngineRequest
import manager.inputs.snippet.RunSnippetRequest
import manager.inputs.snippet.ShareSnippetRequest
import manager.inputs.snippet.UpdateSnippetRequest
import manager.outputs.snippet.CheckPermisesResponse
import manager.outputs.snippet.CreateSnippetResponse
import manager.outputs.snippet.RunSnippetResponse
import manager.outputs.snippet.SnippetPermisesResponse
import manager.repository.snippet.SnippetRepositoryInterface
import manager.service.engine.response.ParseResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import java.util.Locale.getDefault
import java.util.UUID

@Service
class ManagerService(
    private val auth0Service: Auth0Service,
    private val snippetRepository: SnippetRepositoryInterface,
    private val assetService: AssetService,
) {
    fun createSnippet(
        request: CreateSnippetRequest,
        userId: String,
        userToken: String,
    ): CreateSnippetResponse {
        validateLanguageAndVersion(request.language, request.version)
        val errors = validateSnippet(request.snippet, request.version, request.language)
        if (errors.isNotEmpty()) {
            return CreateSnippetResponse(
                snippetId = "",
                errorMessage = errors,
            )
        }

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
            newBucketId = "${userId.substringAfter("|")}/$result",
        )
        grantWritePermises(userToken, userId, result)
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
        checkWritePermises(userToken, userId, request.snippetId)
        validateSnippetExists(request.snippetId)

        val errors = validateSnippet(request.snippet, request.version, request.language)
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
        checkWritePermises(userToken, userId, input.snippetId)
        validateSnippetExists(input.snippetId)
        grantReadPermises(userToken, input.targetUserId, input.snippetId)
    }

    fun runSnippet(
        input: RunSnippetRequest,
        userId: String,
        userToken: String,
    ): RunSnippetResponse {
        val snippet = validateSnippetExists(input.snippetId)
        checkReadPermises(userToken, userId, input.snippetId)
        println(
            "/v1/asset/" + snippet.bucketId.substringAfter("|") +
                snippet.version +
                snippet.language.name +
                input.varInputs,
        )
        return runSnippet(
            "/v1/asset/" + snippet.bucketId.substringAfter("|"),
            snippet.version,
            snippet.language.name,
            input.varInputs,
        )
    }

    private fun validateSnippet(
        snippet: String,
        version: String,
        language: String,
    ): List<String> {
        val m2mToken = auth0Service.getM2MToken()

        val client =
            RestClient
                .builder()
                .baseUrl("http://snippet-engine-service:8080")
                .build()

        try {
            val parseResponse: ParseResponse =
                client
                    .post()
                    .uri("/engine/parse")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken")
                    .body(
                        ParseRequest(
                            code = snippet,
                            language = language,
                            version = version,
                        ),
                    ).retrieve()
                    .body(ParseResponse::class.java)
                    ?: throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Parser service returned empty response",
                    )
            return parseResponse.parseErrors
        } catch (e: HttpClientErrorException.BadRequest) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Snippet validation failed: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Parser service error: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Parser service unavailable: ${e.message}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error calling parser service: ${e.message}",
                e,
            )
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

    fun grantReadPermises(
        token: String,
        userId: String,
        snippetId: String,
    ): SnippetPermisesResponse {
        val authorizationClient: RestClient =
            RestClient
                .builder()
                .baseUrl("http://authorization-service:8080")
                .build()
        try {
            val response =
                authorizationClient
                    .post()
                    .uri("/snippet-permissions/grant-read-access")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .body(
                        PermissionRequest(
                            userId = userId,
                            snippetId = snippetId,
                        ),
                    ).retrieve()
                    .body(SnippetPermisesResponse::class.java)

            return response!!
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Error granting permissions: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error granting read permissions: ${e.message}",
                e,
            )
        }
    }

    fun grantWritePermises(
        token: String,
        userId: String,
        snippetId: String,
    ): SnippetPermisesResponse {
        val authorizationClient: RestClient =
            RestClient
                .builder()
                .baseUrl("http://authorization-service:8080")
                .build()
        try {
            val response =
                authorizationClient
                    .post()
                    .uri("/snippet-permissions/grant-write-access")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .body(
                        PermissionRequest(
                            userId = userId,
                            snippetId = snippetId,
                        ),
                    ).retrieve()
                    .body(SnippetPermisesResponse::class.java)

            return response!!
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Error granting permissions: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error granting read permissions: ${e.message}",
                e,
            )
        }
    }

    private fun checkWritePermises(
        token: String,
        userId: String,
        snippetId: String,
    ) {
        val authorizationClient: RestClient =
            RestClient
                .builder()
                .baseUrl("http://authorization-service:8080")
                .build()

        try {
            val response: CheckPermisesResponse? =
                authorizationClient
                    .post()
                    .uri("/snippet-permissions/validate-write")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .body(
                        PermissionRequest(
                            userId = userId,
                            snippetId = snippetId,
                        ),
                    ).retrieve()
                    .body(CheckPermisesResponse::class.java)

            response?.allowed?.let {
                if (!it) {
                    throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "User does not have write permissions for this snippet.",
                    )
                }
            }
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Error checking permissions: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error checking write permissions: ${e.message}",
                e,
            )
        }
    }

    private fun checkReadPermises(
        token: String,
        userId: String,
        snippetId: String,
    ) {
        val authorizationClient: RestClient =
            RestClient
                .builder()
                .baseUrl("http://authorization-service:8080")
                .build()

        try {
            val response: CheckPermisesResponse? =
                authorizationClient
                    .post()
                    .uri("/snippet-permissions/validate-read-access")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .body(
                        PermissionRequest(
                            userId = userId,
                            snippetId = snippetId,
                        ),
                    ).retrieve()
                    .body(CheckPermisesResponse::class.java)

            response?.allowed?.let {
                if (!it) {
                    throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "User does not have read permissions for this snippet.",
                    )
                }
            }
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Error checking permissions: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error checking read permissions: ${e.message}",
                e,
            )
        }
    }

    private fun runSnippet(
        path: String,
        version: String,
        language: String,
        inputs: List<String>,
    ): RunSnippetResponse {
        val m2mToken = auth0Service.getM2MToken()

        val client =
            RestClient
                .builder()
                .baseUrl("http://snippet-engine-service:8080")
                .build()

        try {
            val executeResponse: RunSnippetResponse =
                client
                    .post()
                    .uri("/engine/execute")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken")
                    .body(
                        RunSnippetInEngineRequest(
                            assetPath = path,
                            language = language,
                            version = version,
                            varInputs = inputs,
                        ),
                    ).retrieve()
                    .body(RunSnippetResponse::class.java)
                    ?: throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Runner service returned empty response",
                    )
            return executeResponse
        } catch (e: HttpClientErrorException.BadRequest) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Snippet validation failed: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Runner service error: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Runner service unavailable: ${e.message}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error calling Runner service: ${e.message}",
                e,
            )
        }
    }
}
