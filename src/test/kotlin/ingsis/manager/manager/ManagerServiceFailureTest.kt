package ingsis.manager.manager

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import manager.entity.CompilantState
import manager.entity.Languages
import manager.entity.Snippet
import manager.inputs.snippet.CreateSnippetRequest
import manager.inputs.snippet.RunSnippetRequest
import manager.inputs.snippet.ShareSnippetRequest
import manager.inputs.snippet.UpdateSnippetRequest
import manager.inputs.snippet.UpdateSnippetStateRequest
import manager.repository.snippet.SnippetRepositoryInterface
import manager.repository.snippet.deleted.DeletedSnippetRepositoryInterface
import manager.service.app.ConfigService
import manager.service.app.ManagerService
import manager.service.asset.AssetServiceInterface
import manager.service.authorization.AuthorizationServiceInterface
import manager.service.engine.EngineServiceInterface
import manager.service.oauth.Auth0Service
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManagerServiceFailureTest {
    @MockK
    lateinit var snippetRepository: SnippetRepositoryInterface

    @MockK lateinit var assetService: AssetServiceInterface

    @MockK lateinit var authorizationService: AuthorizationServiceInterface

    @MockK lateinit var engineService: EngineServiceInterface

    @MockK lateinit var deletedSnippetRepository: DeletedSnippetRepositoryInterface

    @MockK lateinit var configService: ConfigService

    @MockK lateinit var auth0Service: Auth0Service

    lateinit var managerService: ManagerService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        managerService =
            ManagerService(
                snippetRepository = snippetRepository,
                assetService = assetService,
                authorizationService = authorizationService,
                engineService = engineService,
                deletedSnippetRepository = deletedSnippetRepository,
                configSerivice = configService,
                auth0Service = auth0Service,
            )
    }

    @Test
    fun `createSnippet should fail with invalid language`() {
        val request =
            CreateSnippetRequest(
                name = "MySnippet",
                description = "Test",
                language = "INVALID_LANG",
                version = "V1",
                snippet = "print(1)",
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.createSnippet(request, userId, token)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("Invalid language"))
    }

    @Test
    fun `createSnippet should fail with unsupported version`() {
        val request =
            CreateSnippetRequest(
                name = "MySnippet",
                description = "Test",
                language = "PRINTSCRIPT",
                version = "V99",
                snippet = "print(1)",
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.createSnippet(request, userId, token)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("Not supported version"))
    }

    @Test
    fun `createSnippet should delete snippet and return errors when validation fails`() {
        val request =
            CreateSnippetRequest(
                name = "MySnippet",
                description = "Test",
                language = "PRINTSCRIPT",
                version = "V2",
                snippet = "invalid syntax here",
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"
        val generatedId = "789"

        every { auth0Service.getUserName(userId) } returns "Vito"
        every {
            snippetRepository.saveSnippet(any(), any(), any(), any(), any(), any(), any())
        } returns generatedId

        every { assetService.createAsset(any(), any(), any()) } returns "ok"
        every { snippetRepository.updateBucketIdForSnippets(any(), any()) } returns Unit

        every {
            engineService.validateSnippet(any(), any(), any())
        } returns listOf("Syntax error at line 1", "Unexpected token")

        every { snippetRepository.deleteSnippet(generatedId) } returns Unit
        every { assetService.deleteAsset(any(), any()) } returns "ok"

        val result = managerService.createSnippet(request, userId, token)

        assertEquals("", result.id)
        assertEquals(2, result.errorMessage.size)
        assertTrue(result.errorMessage.contains("Syntax error at line 1"))

        verify(exactly = 1) { snippetRepository.deleteSnippet(generatedId) }
        verify(exactly = 1) { assetService.deleteAsset(any(), any()) }
    }

    @Test
    fun `createSnippet should fail when asset service throws exception`() {
        val request =
            CreateSnippetRequest(
                name = "MySnippet",
                description = "Test",
                language = "PRINTSCRIPT",
                version = "V2",
                snippet = "print(1)",
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"
        val generatedId = "789"

        every { auth0Service.getUserName(userId) } returns "Vito"
        every {
            snippetRepository.saveSnippet(any(), any(), any(), any(), any(), any(), any())
        } returns generatedId

        every {
            assetService.createAsset(any(), any(), any())
        } throws RuntimeException("Bucket connection failed")

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.createSnippet(request, userId, token)
            }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertTrue(exception.reason!!.contains("Error saving snippet in bucket"))
    }

    @Test
    fun `updateSnippet should fail when snippet does not exist`() {
        val request =
            UpdateSnippetRequest(
                snippetId = "non-existent",
                name = "Updated",
                description = "desc",
                language = "PRINTSCRIPT",
                version = "V2",
                snippet = "print(2)",
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"

        every { authorizationService.checkWritePermises(token, userId, "non-existent") } returns Unit
        every { snippetRepository.getSnippetById("non-existent") } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.updateSnippet(request, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(exception.reason!!.contains("Snippet not found"))
    }

    @Test
    fun `updateSnippet should fail when user has no write permission`() {
        val request =
            UpdateSnippetRequest(
                snippetId = "123",
                name = "Updated",
                description = "desc",
                language = "PRINTSCRIPT",
                version = "V2",
                snippet = "print(2)",
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"

        every {
            authorizationService.checkWritePermises(token, userId, "123")
        } throws ResponseStatusException(HttpStatus.FORBIDDEN, "No write permission")

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.updateSnippet(request, userId, token)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("No write permission"))
    }

    @Test
    fun `updateSnippet should return errors when validation fails`() {
        val request =
            UpdateSnippetRequest(
                snippetId = "123",
                name = "Updated",
                description = "desc",
                language = "PRINTSCRIPT",
                version = "V2",
                snippet = "invalid code",
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"

        val existingSnippet =
            Snippet(
                id = "123",
                name = "Old",
                description = "Old desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                author = "Vito",
                bucketId = "/v1/asset/vito123/123",
                state = CompilantState.COMPILANT,
            )

        every { authorizationService.checkWritePermises(token, userId, "123") } returns Unit
        every { snippetRepository.getSnippetById("123") } returns existingSnippet
        every { assetService.getAsset(any(), any()) } returns "old code"
        every { assetService.createAsset(any(), "123lint", any()) } returns "ok"

        every {
            engineService.validateSnippet(any(), any(), any())
        } returns listOf("Invalid syntax")

        every { assetService.deleteAsset(any(), "123lint") } returns "ok"

        val result = managerService.updateSnippet(request, userId, token)

        assertEquals("123", result.id)
        assertEquals(1, result.errorMessage.size)
        assertTrue(result.errorMessage.contains("Invalid syntax"))

        verify(exactly = 1) { assetService.deleteAsset(any(), "123lint") }
        verify(exactly = 0) { snippetRepository.updateSnippet(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getSnippet should fail when snippet does not exist`() {
        val snippetId = "non-existent"
        val userId = "auth0|vito123"
        val token = "Bearer token"

        every { authorizationService.checkReadPermises(token, userId, snippetId) } returns Unit
        every { snippetRepository.getSnippetById(snippetId) } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.getSnippet(snippetId, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(exception.reason!!.contains("Snippet not found"))
    }

    @Test
    fun `getSnippet should fail when user has no read permission`() {
        val snippetId = "123"
        val userId = "auth0|vito123"
        val token = "Bearer token"

        every {
            authorizationService.checkReadPermises(token, userId, snippetId)
        } throws ResponseStatusException(HttpStatus.FORBIDDEN, "No read permission")

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.getSnippet(snippetId, userId, token)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
        assertTrue(exception.reason!!.contains("No read permission"))
    }

    @Test
    fun `deleteSnippet should fail when snippet does not exist`() {
        val snippetId = "non-existent"
        val userId = "auth0|vito123"
        val token = "Bearer token"

        every { authorizationService.checkWritePermises(token, userId, snippetId) } returns Unit
        every { snippetRepository.getSnippetById(snippetId) } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.deleteSnippet(snippetId, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(exception.reason!!.contains("Snippet not found"))
    }

    @Test
    fun `deleteSnippet should fail when user has no write permission`() {
        val snippetId = "123"
        val userId = "auth0|vito123"
        val token = "Bearer token"

        every {
            authorizationService.checkWritePermises(token, userId, snippetId)
        } throws ResponseStatusException(HttpStatus.FORBIDDEN, "No write permission")

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.deleteSnippet(snippetId, userId, token)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }

    @Test
    fun `shareSnippet should fail when snippet does not exist`() {
        val request =
            ShareSnippetRequest(
                snippetId = "non-existent",
                targetUserId = "auth0|pepe",
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"

        every { authorizationService.checkWritePermises(token, userId, "non-existent") } returns Unit
        every { snippetRepository.getSnippetById("non-existent") } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.shareSnippet(request, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `shareSnippet should fail when user has no write permission`() {
        val request =
            ShareSnippetRequest(
                snippetId = "123",
                targetUserId = "auth0|pepe",
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"

        every {
            authorizationService.checkWritePermises(token, userId, "123")
        } throws ResponseStatusException(HttpStatus.FORBIDDEN, "No permission to share")

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.shareSnippet(request, userId, token)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }

    @Test
    fun `runSnippet should fail when snippet does not exist`() {
        val request =
            RunSnippetRequest(
                snippetId = "non-existent",
                varInputs = emptyList(),
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"

        every { snippetRepository.getSnippetById("non-existent") } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.runSnippet(request, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `runSnippet should fail when user has no read permission`() {
        val request =
            RunSnippetRequest(
                snippetId = "123",
                varInputs = emptyList(),
            )
        val userId = "auth0|vito123"
        val token = "Bearer token"

        val snippet =
            Snippet(
                id = "123",
                name = "Test",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = "auth0|otro",
                bucketId = "/v1/asset/otro/123",
                state = CompilantState.COMPILANT,
                author = "Otro",
            )

        every { snippetRepository.getSnippetById("123") } returns snippet
        every {
            authorizationService.checkReadPermises(token, userId, "123")
        } throws ResponseStatusException(HttpStatus.FORBIDDEN, "No read permission")

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.runSnippet(request, userId, token)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }

    @Test
    fun `changeSnippetState should fail when snippet does not exist`() {
        val request =
            UpdateSnippetStateRequest(
                snippetId = "non-existent",
                state = CompilantState.NON_COMPILANT,
            )

        every { snippetRepository.getSnippetById("non-existent") } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                managerService.changeSnippetState(request)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `getAllSnippets should handle asset retrieval failures gracefully`() {
        val userId = "auth0|vito123"
        val token = "Bearer token"

        val snippet =
            Snippet(
                id = "1",
                name = "Test",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "b1",
                state = CompilantState.COMPILANT,
                author = "Vito",
            )

        every {
            snippetRepository.getPaginatedSnippetsByUserId(userId, 0, 10)
        } returns listOf(snippet)

        every { snippetRepository.countSnippetsByUserId(userId) } returns 1

        every {
            assetService.getAsset(any(), any())
        } throws RuntimeException("Connection timeout")

        every { authorizationService.getSharedSnippets(token, userId) } returns emptyList()

        val result = managerService.getAllSnippets(userId, token, 0, 10)

        assertEquals(1, result.snippets.size)
        assertEquals("Unable to retrieve snippet code", result.snippets[0].snippet)
    }
}
