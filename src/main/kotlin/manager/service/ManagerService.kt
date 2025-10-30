package manager.service

import manager.entity.Languages
import manager.entity.Snippet
import manager.inputs.CreateSnippetRequest
import manager.inputs.ParseRequest
import manager.inputs.PermissionRequest
import manager.inputs.UpdateSnippetRequest
import manager.outputs.CheckPermisesResponse
import manager.outputs.CreateSnippetResponse
import manager.outputs.SnippetPermisesResponse
import manager.repository.SnippetRepositoryInterface
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
        saveOrUpdateSnippetInBucket(request.name, request.snippet, userId)
        val result =
            snippetRepository.saveSnippet(
                bucketId = "$userId/${request.name}",
                language = request.language.uppercase(getDefault()),
                name = request.name,
                description = request.description,
                version = request.version,
                userId = userId,
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
                bucketId = request.snippet,
                language = request.language.uppercase(getDefault()),
                description = request.description,
                version = request.version,
            )
        saveOrUpdateSnippetInBucket(request.name, request.snippet, userId)
        return CreateSnippetResponse(
            snippetId = updatedSnippetId,
            errorMessage = emptyList(),
        )
    }

    fun getSnippet(
        snippetId: String,
        userId: String,
    ): Snippet {
        validateUserUUID(userId)
        validateUserUUID(snippetId)
        val snippet =
            snippetRepository.getSnippetById(snippetId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $snippetId")
        return snippet
    }

    fun getAllSnippets(
        userId: String,
    ): List<Snippet> {
        validateUserUUID(userId)
        val snippets =
            snippetRepository.getAllSnippetsByUserId(userId)
        return snippets
    }

    private fun validateUserUUID(id: String) {
        try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid UUID format")
        }
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

    private fun validateSnippetExists(snippetId: String) {
        val snippet =
            snippetRepository.getSnippetById(snippetId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $snippetId")
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
            val response =authorizationClient
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
}
