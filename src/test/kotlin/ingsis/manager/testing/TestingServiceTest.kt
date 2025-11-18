package ingsis.manager.testing

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import manager.entity.Languages
import manager.entity.Snippet
import manager.entity.TestEntity
import manager.inputs.testing.CreateTestRequest
import manager.inputs.testing.EditTestRequest
import manager.inputs.testing.IdTestRequest
import manager.repository.snippet.SnippetRepositoryInterface
import manager.repository.testing.TestingRepositoryInterface
import manager.service.app.TestingService
import manager.service.authorization.AuthorizationServiceInterface
import manager.service.engine.EngineServiceInterface
import manager.service.engine.response.TestResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.test.Test
import kotlin.test.assertEquals

class TestingServiceTest {
    @MockK
    lateinit var snippetRepository: SnippetRepositoryInterface

    @MockK
    lateinit var testingRepository: TestingRepositoryInterface

    @MockK
    lateinit var engineService: EngineServiceInterface

    @MockK
    lateinit var authorizationService: AuthorizationServiceInterface

    lateinit var testingService: TestingService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        testingService =
            TestingService(
                snippetRepository = snippetRepository,
                testingRepository = testingRepository,
                engineService = engineService,
                authorizationService = authorizationService,
            )
    }

    @Test
    fun `createTest should create test successfully`() {
        val request =
            CreateTestRequest(
                snippetId = "snippet123",
                name = "Test 1",
                input = listOf("input1", "input2"),
                output = listOf("output1"),
            )
        val userId = "auth0|user123"
        val token = "Bearer token"
        val testId = "test123"

        val snippet =
            Snippet(
                id = "snippet123",
                name = "Test Snippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "/v1/asset/user123/snippet123",
                author = "User",
            )

        every { authorizationService.checkWritePermises(token, userId, "snippet123") } returns Unit
        every { snippetRepository.getSnippetById("snippet123") } returns snippet
        every {
            testingRepository.saveTest("snippet123", "Test 1", request.input, request.output)
        } returns testId

        val result = testingService.createTest(request, userId, token)

        assertEquals(testId, result)

        verify(exactly = 1) { authorizationService.checkWritePermises(token, userId, "snippet123") }
        verify(exactly = 1) { snippetRepository.getSnippetById("snippet123") }
        verify(exactly = 1) { testingRepository.saveTest("snippet123", "Test 1", request.input, request.output) }
    }

    @Test
    fun `createTest should fail when snippet does not exist`() {
        val request =
            CreateTestRequest(
                snippetId = "non-existent",
                name = "Test 1",
                input = listOf("input1"),
                output = listOf("output1"),
            )
        val userId = "auth0|user123"
        val token = "Bearer token"

        every { authorizationService.checkWritePermises(token, userId, "non-existent") } returns Unit
        every { snippetRepository.getSnippetById("non-existent") } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                testingService.createTest(request, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("Snippet not found with id: non-existent", exception.reason)
    }

    @Test
    fun `createTest should fail when user has no write permission`() {
        val request =
            CreateTestRequest(
                snippetId = "snippet123",
                name = "Test 1",
                input = listOf("input1"),
                output = listOf("output1"),
            )
        val userId = "auth0|user123"
        val token = "Bearer token"

        every {
            authorizationService.checkWritePermises(token, userId, "snippet123")
        } throws ResponseStatusException(HttpStatus.FORBIDDEN, "No write permission")

        val exception =
            assertThrows<ResponseStatusException> {
                testingService.createTest(request, userId, token)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }

    @Test
    fun `runTest should return success when test passes`() {
        val request = IdTestRequest(testId = "test123")
        val userId = "auth0|user123"
        val token = "Bearer token"

        val test =
            TestEntity(
                id = "test123",
                snippetId = "snippet123",
                name = "Test 1",
                input = listOf("input1"),
                output = listOf("output1"),
            )

        val snippet =
            Snippet(
                id = "snippet123",
                name = "Test Snippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "/v1/asset/user123/snippet123",
                author = "User",
            )

        val testResponse = TestResponse(passed = true)

        every { testingRepository.getTestById("test123") } returns test
        every { snippetRepository.getSnippetById("snippet123") } returns snippet
        every { authorizationService.checkWritePermises(token, userId, "snippet123") } returns Unit
        every {
            engineService.runTest(
                "PRINTSCRIPT",
                "V1",
                "/v1/asset/user123/snippet123",
                test.input,
                test.output,
            )
        } returns testResponse

        val result = testingService.runTest(request, userId, token)

        assertEquals("Test passed successfully", result)

        verify(exactly = 1) { testingRepository.getTestById("test123") }
        verify(exactly = 1) { snippetRepository.getSnippetById("snippet123") }
        verify(exactly = 1) { engineService.runTest(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `runTest should return failure when test fails`() {
        val request = IdTestRequest(testId = "test123")
        val userId = "auth0|user123"
        val token = "Bearer token"

        val test =
            TestEntity(
                id = "test123",
                snippetId = "snippet123",
                name = "Test 1",
                input = listOf("input1"),
                output = listOf("output1"),
            )

        val snippet =
            Snippet(
                id = "snippet123",
                name = "Test Snippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "/v1/asset/user123/snippet123",
                author = "User",
            )

        val testResponse = TestResponse(passed = false)

        every { testingRepository.getTestById("test123") } returns test
        every { snippetRepository.getSnippetById("snippet123") } returns snippet
        every { authorizationService.checkWritePermises(token, userId, "snippet123") } returns Unit
        every {
            engineService.runTest(any(), any(), any(), any(), any())
        } returns testResponse

        val result = testingService.runTest(request, userId, token)

        assertEquals("Test failed.", result)
    }

    @Test
    fun `runTest should fail when test does not exist`() {
        val request = IdTestRequest(testId = "non-existent")
        val userId = "auth0|user123"
        val token = "Bearer token"

        every { testingRepository.getTestById("non-existent") } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                testingService.runTest(request, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("Test not found with id: non-existent", exception.reason)
    }

    @Test
    fun `deleteTest should delete test successfully`() {
        val request = IdTestRequest(testId = "test123")
        val userId = "auth0|user123"
        val token = "Bearer token"

        val test =
            TestEntity(
                id = "test123",
                snippetId = "snippet123",
                name = "Test 1",
                input = listOf("input1"),
                output = listOf("output1"),
            )

        val snippet =
            Snippet(
                id = "snippet123",
                name = "Test Snippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "/v1/asset/user123/snippet123",
                author = "User",
            )

        every { testingRepository.getTestById("test123") } returns test
        every { snippetRepository.getSnippetById("snippet123") } returns snippet
        every { authorizationService.checkWritePermises(token, userId, "snippet123") } returns Unit
        every { testingRepository.deleteTest("test123") } returns Unit

        testingService.deleteTest(request, userId, token)

        verify(exactly = 1) { testingRepository.getTestById("test123") }
        verify(exactly = 1) { testingRepository.deleteTest("test123") }
    }

    @Test
    fun `deleteTest should fail when test does not exist`() {
        val request = IdTestRequest(testId = "non-existent")
        val userId = "auth0|user123"
        val token = "Bearer token"

        every { testingRepository.getTestById("non-existent") } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                testingService.deleteTest(request, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `editTest should update test successfully`() {
        val request =
            EditTestRequest(
                testId = "test123",
                snippetId = "snippet123",
                name = "Updated Test",
                input = listOf("new input"),
                output = listOf("new output"),
            )
        val userId = "auth0|user123"
        val token = "Bearer token"

        val test =
            TestEntity(
                id = "test123",
                snippetId = "snippet123",
                name = "Old Test",
                input = listOf("old input"),
                output = listOf("old output"),
            )

        val snippet =
            Snippet(
                id = "snippet123",
                name = "Test Snippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "/v1/asset/user123/snippet123",
                author = "User",
            )

        every { testingRepository.getTestById("test123") } returns test
        every { snippetRepository.getSnippetById("snippet123") } returns snippet
        every { authorizationService.checkWritePermises(token, userId, "snippet123") } returns Unit
        every {
            testingRepository.updateTest("test123", "Updated Test", request.input, request.output)
        } returns "Unit"

        testingService.editTest(request, userId, token)

        verify(exactly = 1) { testingRepository.getTestById("test123") }
        verify(exactly = 1) { testingRepository.updateTest("test123", "Updated Test", request.input, request.output) }
    }

    @Test
    fun `editTest should fail when trying to change snippetId`() {
        val request =
            EditTestRequest(
                testId = "test123",
                snippetId = "different-snippet",
                name = "Updated Test",
                input = listOf("input"),
                output = listOf("output"),
            )
        val userId = "auth0|user123"
        val token = "Bearer token"

        val test =
            TestEntity(
                id = "test123",
                snippetId = "snippet123",
                name = "Test 1",
                input = listOf("input1"),
                output = listOf("output1"),
            )

        val snippet =
            Snippet(
                id = "snippet123",
                name = "Test Snippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "/v1/asset/user123/snippet123",
                author = "User",
            )

        every { testingRepository.getTestById("test123") } returns test
        every { snippetRepository.getSnippetById("snippet123") } returns snippet

        val exception =
            assertThrows<ResponseStatusException> {
                testingService.editTest(request, userId, token)
            }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("Cannot change id of the test", exception.reason)
    }

    @Test
    fun `editTest should fail when test does not exist`() {
        val request =
            EditTestRequest(
                testId = "non-existent",
                snippetId = "snippet123",
                name = "Test",
                input = listOf("input"),
                output = listOf("output"),
            )
        val userId = "auth0|user123"
        val token = "Bearer token"

        every { testingRepository.getTestById("non-existent") } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                testingService.editTest(request, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `getAllTestsBySnippetId should return all tests for snippet`() {
        val snippetId = "snippet123"
        val userId = "auth0|user123"
        val token = "Bearer token"

        val snippet =
            Snippet(
                id = "snippet123",
                name = "Test Snippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "/v1/asset/user123/snippet123",
                author = "User",
            )

        val tests =
            listOf(
                TestEntity(
                    id = "test1",
                    snippetId = snippetId,
                    name = "Test 1",
                    input = listOf("input1"),
                    output = listOf("output1"),
                ),
                TestEntity(
                    id = "test2",
                    snippetId = snippetId,
                    name = "Test 2",
                    input = listOf("input2"),
                    output = listOf("output2"),
                ),
            )

        every { snippetRepository.getSnippetById(snippetId) } returns snippet
        every { authorizationService.checkReadPermises(token, userId, snippetId) } returns Unit
        every { testingRepository.getAllTestsBySnippetId(snippetId) } returns tests

        val result = testingService.getAllTestsBySnippetId(snippetId, userId, token)

        assertEquals(2, result.size)
        assertEquals("test1", result[0].id)
        assertEquals("test2", result[1].id)

        verify(exactly = 1) { snippetRepository.getSnippetById(snippetId) }
        verify(exactly = 1) { testingRepository.getAllTestsBySnippetId(snippetId) }
    }

    @Test
    fun `getAllTestsBySnippetId should fail when snippet does not exist`() {
        val snippetId = "non-existent"
        val userId = "auth0|user123"
        val token = "Bearer token"

        every { snippetRepository.getSnippetById(snippetId) } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                testingService.getAllTestsBySnippetId(snippetId, userId, token)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `getAllTestsBySnippetId should fail when user has no read permission`() {
        val snippetId = "snippet123"
        val userId = "auth0|user123"
        val token = "Bearer token"

        val snippet =
            Snippet(
                id = "snippet123",
                name = "Test Snippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = "auth0|other",
                bucketId = "/v1/asset/other/snippet123",
                author = "Other",
            )

        every { snippetRepository.getSnippetById(snippetId) } returns snippet
        every {
            authorizationService.checkReadPermises(token, userId, snippetId)
        } throws ResponseStatusException(HttpStatus.FORBIDDEN, "No read permission")

        val exception =
            assertThrows<ResponseStatusException> {
                testingService.getAllTestsBySnippetId(snippetId, userId, token)
            }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }
}
