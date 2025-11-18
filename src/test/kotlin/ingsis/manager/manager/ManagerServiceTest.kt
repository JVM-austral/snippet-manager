package ingsis.manager.manager

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import manager.entity.CompilantState
import manager.entity.Languages
import manager.entity.Snippet
import manager.inputs.snippet.CreateSnippetRequest
import manager.inputs.snippet.UpdateSnippetRequest
import manager.inputs.snippet.UpdateSnippetStateRequest
import manager.outputs.snippet.SnippetPermisesResponse
import manager.repository.snippet.SnippetRepositoryInterface
import manager.repository.snippet.deleted.DeletedSnippetRepositoryInterface
import manager.service.app.ConfigService
import manager.service.app.ManagerService
import manager.service.asset.AssetServiceInterface
import manager.service.authorization.AuthorizationServiceInterface
import manager.service.engine.EngineServiceInterface
import manager.service.engine.response.LintResponse
import manager.service.oauth.Auth0Service
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManagerServiceTest {
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
    fun `createSnippet should create snippet successfully`() {
        val request =
            CreateSnippetRequest(
                name = "MySnippet",
                description = "A test snippet",
                language = "PRINTSCRIPT",
                version = "V2",
                snippet = "System.out.println(\"Hi!\")",
            )
        val userId = "auth0|vito123"
        val token = "Bearer pepe"
        val generatedId = "789"

        every { auth0Service.getUserName(userId) } returns "Vito"
        every {
            snippetRepository.saveSnippet(
                bucketId = "",
                language = "PRINTSCRIPT",
                name = "MySnippet",
                description = "A test snippet",
                version = "V2",
                userId = userId,
                author = "Vito",
            )
        } returns generatedId

        every { assetService.createAsset("vito123", generatedId, request.snippet) } returns "hola"
        every { snippetRepository.updateBucketIdForSnippets(generatedId, any()) } returns Unit

        every {
            engineService.validateSnippet(any(), "V2", "PRINTSCRIPT")
        } returns emptyList()

        every {
            configService.lintUniqueWithPath(
                userId = userId,
                path = any(),
                language = "PRINTSCRIPT",
                version = "V2",
            )
        } returns LintResponse(lintErrors = emptyList())

        every {
            snippetRepository.setSnippetState(generatedId, any())
        } returns Unit

        every {
            authorizationService.grantWritePermises(token, userId, generatedId)
        } returns
            SnippetPermisesResponse(
                id = "1",
                userId = userId,
                snippetId = generatedId,
                permission = "WRITE",
            )

        val result = managerService.createSnippet(request, userId, token)

        assertEquals(generatedId, result.id)
        assertEquals("MySnippet", result.name)
        assertEquals("PRINTSCRIPT", result.language)
        assertEquals("Vito", result.author)
        assertTrue(result.errorMessage.isEmpty())

        verify(exactly = 1) { snippetRepository.saveSnippet(any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 1) { assetService.createAsset("vito123", generatedId, request.snippet) }
        verify(exactly = 1) { engineService.validateSnippet(any(), "V2", "PRINTSCRIPT") }
        verify(exactly = 1) { authorizationService.grantWritePermises(token, userId, generatedId) }
    }

    @Test
    fun `updateSnippet should update snippet successfully`() {
        val updateRequest =
            UpdateSnippetRequest(
                snippetId = "123",
                name = "UpdatedName",
                description = "Updated description",
                language = "PRINTSCRIPT",
                version = "V2",
                snippet = "let a = 10;",
            )

        val userId = "auth0|vito123"
        val token = "Bearer pepe"

        val existingSnippet =
            Snippet(
                id = "123",
                name = "OldName",
                description = "Old description",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                author = "Vito",
                bucketId = "/v1/asset/vito123/123",
                state = CompilantState.COMPILANT,
            )

        every { authorizationService.checkWritePermises(token, userId, "123") } returns Unit
        every { snippetRepository.getSnippetById("123") } returns existingSnippet
        every { assetService.getAsset("vito123", "123") } returns "old content"

        every { assetService.createAsset("vito123", "123lint", updateRequest.snippet) } returns "Unit"

        every {
            engineService.validateSnippet(any(), "V2", "PRINTSCRIPT")
        } returns emptyList()

        every { assetService.deleteAsset("vito123", "123lint") } returns "Unit"

        every { assetService.createAsset("vito123", "123", updateRequest.snippet) } returns "Unit"

        every {
            configService.lintUniqueWithPath(
                userId = userId,
                path = any(),
                language = "PRINTSCRIPT",
                version = "V2",
            )
        } returns LintResponse(emptyList())

        every { snippetRepository.setSnippetState("123", any()) } returns Unit

        every {
            snippetRepository.updateSnippet(
                snippetId = "123",
                name = "UpdatedName",
                language = "PRINTSCRIPT",
                description = "Updated description",
                version = "V2",
            )
        } returns "123"

        val result = managerService.updateSnippet(updateRequest, userId, token)

        assertEquals("123", result.id)
        assertTrue(result.errorMessage.isEmpty())

        verify(exactly = 1) { authorizationService.checkWritePermises(token, userId, "123") }
        verify(exactly = 1) { snippetRepository.getSnippetById("123") }
        verify(exactly = 1) { assetService.getAsset("vito123", "123") }

        verify(exactly = 1) { assetService.createAsset("vito123", "123lint", updateRequest.snippet) }
        verify(exactly = 1) { engineService.validateSnippet(any(), "V2", "PRINTSCRIPT") }
        verify(exactly = 1) { assetService.deleteAsset("vito123", "123lint") }

        verify(exactly = 1) { assetService.createAsset("vito123", "123", updateRequest.snippet) }
        verify(exactly = 1) { snippetRepository.updateSnippet(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getSnippet should return snippet successfully`() {
        val snippetId = "123"
        val userId = "auth0|vito123"
        val token = "Bearer token"

        val snippet =
            Snippet(
                id = snippetId,
                name = "MySnippet",
                description = "Description",
                bucketId = "bucket-77",
                language = Languages.PRINTSCRIPT,
                version = "V2",
                userId = userId,
                author = "Vito",
                state = CompilantState.COMPILANT,
            )

        every { authorizationService.checkReadPermises(token, userId, snippetId) } returns Unit
        every { snippetRepository.getSnippetById(snippetId) } returns snippet
        every { assetService.getAsset("vito123", snippetId) } returns "print(1+1)"

        val result = managerService.getSnippet(snippetId, userId, token)

        assertEquals(snippetId, result.id)
        assertEquals("MySnippet", result.name)
        assertEquals("Description", result.description)
        assertEquals("print(1+1)", result.snippet)
        assertEquals("PRINTSCRIPT", result.language)
        assertEquals("V2", result.version)
        assertEquals("COMPILANT", result.compliance)
        assertEquals("Vito", result.author)

        verify(exactly = 1) { authorizationService.checkReadPermises(token, userId, snippetId) }
        verify(exactly = 1) { snippetRepository.getSnippetById(snippetId) }
        verify(exactly = 1) { assetService.getAsset("vito123", snippetId) }
    }

    @Test
    fun `getAllSnippets should return own snippets plus shared snippets`() {
        val userId = "auth0|vito123"
        val token = "Bearer 123"

        val snippet1 =
            Snippet(
                id = "1",
                name = "Code1",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "b1",
                state = CompilantState.COMPILANT,
                author = "Vito",
            )

        val snippet2 =
            Snippet(
                id = "2",
                name = "Code2",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "b2",
                state = CompilantState.NON_COMPILANT,
                author = "Vito",
            )

        every {
            snippetRepository.getPaginatedSnippetsByUserId(userId, 0, 10)
        } returns listOf(snippet1, snippet2)

        every { snippetRepository.countSnippetsByUserId(userId) } returns 2

        every { assetService.getAsset("vito123", "1") } returns "print(1)"
        every { assetService.getAsset("vito123", "2") } returns "print(2)"

        every { authorizationService.getSharedSnippets(token, userId) } returns listOf("99")

        val sharedSnippet =
            Snippet(
                id = "99",
                name = "Shared",
                description = "shared desc",
                language = Languages.PRINTSCRIPT,
                version = "V2",
                userId = "auth0|otro",
                bucketId = "bx",
                state = CompilantState.COMPILANT,
                author = "Pepe",
            )

        every { snippetRepository.getSnippetById("99") } returns sharedSnippet
        every { assetService.getAsset("otro", "99") } returns "shared code"

        val result =
            managerService.getAllSnippets(
                userId = userId,
                token = token,
                page = 0,
                pageSize = 10,
            )

        assertEquals(3, result.snippets.size)
        assertEquals(3, result.pagination.count)

        assertEquals("1", result.snippets[0].id)
        assertEquals("print(1)", result.snippets[0].snippet)

        assertEquals("2", result.snippets[1].id)
        assertEquals("print(2)", result.snippets[1].snippet)

        val shared = result.snippets.find { it.id == "99" }!!
        assertEquals("shared code", shared.snippet)
        assertEquals("Pepe", shared.author)

        verify(exactly = 1) {
            snippetRepository.getPaginatedSnippetsByUserId(userId, 0, 10)
        }
        verify(exactly = 1) {
            authorizationService.getSharedSnippets(token, userId)
        }
        verify(exactly = 3) {
            assetService.getAsset(any(), any())
        }
    }

    @Test
    fun `deleteSnippet should delete snippet successfully`() {
        val snippetId = "123"
        val userId = "auth0|vito123"
        val token = "Bearer token"

        val snippet =
            Snippet(
                id = snippetId,
                name = "MySnippet",
                description = "Desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "/v1/asset/vito123/123",
                state = CompilantState.COMPILANT,
                author = "Vito",
            )

        every {
            authorizationService.checkWritePermises(token, userId, snippetId)
        } returns Unit

        every { snippetRepository.getSnippetById(snippetId) } returns snippet

        every { snippetRepository.deleteSnippet(snippetId) } returns Unit

        every {
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
        } returns Unit

        managerService.deleteSnippet(snippetId, userId, token)

        verify(exactly = 1) {
            authorizationService.checkWritePermises(token, userId, snippetId)
        }
        verify(exactly = 1) { snippetRepository.getSnippetById(snippetId) }
        verify(exactly = 1) { snippetRepository.deleteSnippet(snippetId) }
        verify(exactly = 1) {
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
    }

    @Test
    fun `changeSnippetState should update state successfully`() {
        val snippetId = "123"
        val newState = CompilantState.NON_COMPILANT

        val snippet =
            Snippet(
                id = snippetId,
                name = "TestSnippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = "auth0|vito",
                bucketId = "/v1/asset/vito/123",
                state = CompilantState.COMPILANT,
                author = "Vito",
            )

        every { snippetRepository.getSnippetById(snippetId) } returns snippet

        every {
            snippetRepository.setSnippetState(snippetId, newState)
        } returns Unit

        val input =
            UpdateSnippetStateRequest(
                snippetId = snippetId,
                state = newState,
            )

        managerService.changeSnippetState(input)

        verify(exactly = 1) { snippetRepository.getSnippetById(snippetId) }
        verify(exactly = 1) { snippetRepository.setSnippetState(snippetId, newState) }
    }

    @Test
    fun `getSupportedLanguages should return all supported languages`() {
        val result = managerService.getSupportedLanguages()

        assertEquals(Languages.entries.size, result.size)

        Languages.entries.forEachIndexed { index, language ->
            val response = result[index]

            assertEquals(language.displayName, response.displayName)
            assertEquals(language.versions, response.versions)
            assertEquals(language.extension, response.extension)
        }
    }
}
