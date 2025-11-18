package manager.service.authorization

import manager.inputs.snippet.PermissionRequest
import manager.outputs.snippet.CheckPermisesResponse
import manager.outputs.snippet.SnippetPermisesResponse
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException

@Service
class AuthorizationService(
    private val authorizationRestClient: RestClient,
) : AuthorizationServiceInterface {
    override fun grantReadPermises(
        token: String,
        userId: String,
        snippetId: String,
    ): SnippetPermisesResponse {
        try {
            val response =
                authorizationRestClient
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

    override fun grantWritePermises(
        token: String,
        userId: String,
        snippetId: String,
    ): SnippetPermisesResponse {
        try {
            val response =
                authorizationRestClient
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

    override fun checkWritePermises(
        token: String,
        userId: String,
        snippetId: String,
    ) {
        try {
            val response: CheckPermisesResponse? =
                authorizationRestClient
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

    override fun checkReadPermises(
        token: String,
        userId: String,
        snippetId: String,
    ) {
        try {
            val response: CheckPermisesResponse? =
                authorizationRestClient
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

    override fun getSharedSnippets(
        token: String,
        userId: String,
    ): List<String> {
        try {
            val response =
                authorizationRestClient
                    .get()
                    .uri("/snippet-permissions/shared-snippets/$userId")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .body(object : ParameterizedTypeReference<List<String>>() {})

            return response ?: emptyList()
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Error fetching shared snippets: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error fetching shared snippets: ${e.message}",
                e,
            )
        }
    }
}
