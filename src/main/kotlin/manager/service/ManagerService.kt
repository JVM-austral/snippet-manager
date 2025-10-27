package manager.service

import manager.entity.Languages
import manager.entity.Snippet
import manager.inputs.CreateSnippetRequest
import manager.inputs.UpdateSnippetRequest
import manager.outputs.CreateSnippetResponse
import manager.repository.SnippetRepositoryInterface
import manager.service.engine.response.ParseResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import java.util.Locale.getDefault
import java.util.UUID

@Service
class ManagerService(
    private val auth0Service: Auth0Service,
    private val webClient: WebClient,
    private val snippetRepository: SnippetRepositoryInterface,
) {
    fun createSnippet(
        request: CreateSnippetRequest,
        userId: String,
    ): CreateSnippetResponse {
        validateLanguageAndVersion(request.language, request.version)
        validateUserUUID(userId)
        validateUser()
        val errors = validateSnippet(request.snippet)
        if (errors.isNotEmpty()) {
            return CreateSnippetResponse(
                snippetId = "",
                errorMessage = errors,
            )
        }
        val result =
            snippetRepository.saveSnippet(
                code = request.snippet,
                language = request.language.uppercase(getDefault()),
                name = request.name,
                description = request.description,
                version = request.version,
                userId = userId,
            )
        addPermissions()
        return CreateSnippetResponse(
            snippetId = result,
            errorMessage = errors,
        )
    }

    fun updateSnippet(
        request: UpdateSnippetRequest,
        userId: String,
    ): CreateSnippetResponse {
        validateUserUUID(userId)
        validateUser()
        checkUserPermissions()
        validateSnippetExists(request.snippetId)
        validateUpdateSnippetRequest(request)

        val updatedSnippetId =
            snippetRepository.updateSnippet(
                snippetId = request.snippetId,
                name = request.name,
                code = request.snippet,
                language = request.language?.uppercase(getDefault()),
                description = request.description,
                version = request.version,
            )
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
        validateUser()
        checkUserPermissions()
        val snippet =
            snippetRepository.getSnippetById(snippetId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $snippetId")
        return snippet
    }

    fun getAllSnippets(
        userId: String,
    ): List<Snippet> {
        validateUserUUID(userId)
        validateUser()
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

    private fun validateSnippet(snippet: String): List<String> {
        val m2mToken = auth0Service.getM2MToken()
        val parseResponse: ParseResponse =
            webClient
                .post()
                .uri("/engine/parse")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken")
                .bodyValue(mapOf("code" to snippet)) //  body del request
                .retrieve() //  hace la llamada
                .bodyToMono(ParseResponse::class.java) //  pasa la respuesta a ParseResponse
                .block() // espera hasta q responda (sincrónica)
                ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Parser service returned empty response",
                )
        return parseResponse.parseErrors
    }

    private fun addPermissions() {
        // Lógica para agregar permisos
    }

    private fun checkUserPermissions() {
        // Lógica para verificar permisos
    }

    private fun validateUser() {
        // Logica para validar el usuario
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

    private fun validateUpdateSnippetRequest(request: UpdateSnippetRequest) {
        if (request.version != null && request.language == null || request.version == null && request.language != null) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Language and veersion must be provided when snippet is updated")
        }
        request.language?.let { language ->
            request.version?.let { version ->
                validateLanguageAndVersion(language, version)
            }
        }
        request.snippet?.let { snippet ->
            validateSnippet(
                snippet,
            )
        }
    }
}
