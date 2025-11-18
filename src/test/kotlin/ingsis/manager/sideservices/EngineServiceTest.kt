package ingsis.manager.sideservices

import manager.inputs.snippet.ParseRequest
import manager.inputs.snippet.RunSnippetInEngineRequest
import manager.outputs.snippet.RunSnippetResponse
import manager.repository.format.FormatConfig
import manager.repository.lint.LintConfig
import manager.service.engine.EngineService
import manager.service.engine.inputs.FormatUniqueInputForEngine
import manager.service.engine.inputs.LintUniqueInputForEngine
import manager.service.engine.inputs.TestInput
import manager.service.engine.response.LintErrorResponse
import manager.service.engine.response.LintResponse
import manager.service.engine.response.ParseResponse
import manager.service.engine.response.TestResponse
import manager.service.oauth.Auth0ServiceInterface
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EngineServiceTest {
    private val auth0Service = mock(Auth0ServiceInterface::class.java)
    private val restClient = mock(RestClient::class.java)

    private val uriSpec = mock(RestClient.RequestBodyUriSpec::class.java)
    private val requestBodySpec = mock(RestClient.RequestBodySpec::class.java)
    private val responseSpec = mock(RestClient.ResponseSpec::class.java)

    private val service = EngineService(auth0Service, restClient)

    private fun setupValidateChain(token: String) {
        whenever(restClient.post()).thenReturn(uriSpec)
        whenever(uriSpec.uri("/engine/parse")).thenReturn(uriSpec)
        whenever(uriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.body(any<ParseRequest>()))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.retrieve()).thenReturn(responseSpec)
    }

    @Test
    fun `validateSnippet - BadRequest throws 400 with expected message`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupValidateChain(token)

        val original = HttpClientErrorException(HttpStatus.BAD_REQUEST, "error body")
        whenever(responseSpec.body(ParseResponse::class.java)).thenThrow(original)

        val ex =
            assertThrows<ResponseStatusException> {
                service.validateSnippet("p", "1", "kotlin")
            }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `validateSnippet - Other HttpClientErrorException returns same status and parser service error`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupValidateChain(token)

        val original =
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "not found",
                HttpHeaders(),
                "body 404".toByteArray(),
                null,
            )
        whenever(responseSpec.body(ParseResponse::class.java)).thenThrow(original)

        val ex =
            assertThrows<ResponseStatusException> {
                service.validateSnippet("p", "1", "kotlin")
            }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        assertTrue(ex.reason!!.contains("Parser service error"))
        assertTrue(ex.reason!!.contains("body 404"))
    }

    @Test
    fun `validateSnippet - HttpServerErrorException maps to 503 SERVICE_UNAVAILABLE`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupValidateChain(token)

        val original = HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "bad")
        whenever(responseSpec.body(ParseResponse::class.java)).thenThrow(original)

        val ex =
            assertThrows<ResponseStatusException> {
                service.validateSnippet("p", "1", "kotlin")
            }

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.statusCode)
    }

    @Test
    fun `validateSnippet - Null body response throws 500 Parser service returned empty response`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupValidateChain(token)

        whenever(responseSpec.body(ParseResponse::class.java)).thenReturn(null)

        val ex =
            assertThrows<ResponseStatusException> {
                service.validateSnippet("p", "1", "kotlin")
            }

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
        assertTrue(ex.reason!!.contains("Parser service returned empty response"))
    }

    @Test
    fun `validateSnippet - Success returns parse errors list`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupValidateChain(token)

        val expectedErrors = listOf("Error 1", "Error 2")
        whenever(responseSpec.body(ParseResponse::class.java))
            .thenReturn(ParseResponse(expectedErrors))

        val result = service.validateSnippet("p", "1", "kotlin")

        assertEquals(expectedErrors, result)
    }

    private fun setupRunSnippetChain(token: String) {
        whenever(restClient.post()).thenReturn(uriSpec)
        whenever(uriSpec.uri("/engine/execute")).thenReturn(uriSpec)
        whenever(uriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.body(any<RunSnippetInEngineRequest>()))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.retrieve()).thenReturn(responseSpec)
    }

    @Test
    fun `runSnippet - Success returns RunSnippetResponse`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupRunSnippetChain(token)

        val expectedResponse = RunSnippetResponse(listOf("output1", "output2"), emptyList())
        whenever(responseSpec.body(RunSnippetResponse::class.java))
            .thenReturn(expectedResponse)

        val result = service.runSnippet("p", "1", "kotlin", listOf("input1"))

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `runSnippet - BadRequest throws 400`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupRunSnippetChain(token)

        val original = HttpClientErrorException(HttpStatus.BAD_REQUEST, "error")
        whenever(responseSpec.body(RunSnippetResponse::class.java)).thenThrow(original)

        val ex =
            assertThrows<ResponseStatusException> {
                service.runSnippet("p", "1", "kotlin", listOf())
            }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `runSnippet - Null body throws 500`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupRunSnippetChain(token)

        whenever(responseSpec.body(RunSnippetResponse::class.java)).thenReturn(null)

        val ex =
            assertThrows<ResponseStatusException> {
                service.runSnippet("p", "1", "kotlin", listOf())
            }

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
        assertTrue(ex.reason!!.contains("Runner service returned empty response"))
    }

    @Test
    fun `runSnippet - HttpServerErrorException maps to 503`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupRunSnippetChain(token)

        val original = HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "error")
        whenever(responseSpec.body(RunSnippetResponse::class.java)).thenThrow(original)

        val ex =
            assertThrows<ResponseStatusException> {
                service.runSnippet("p", "1", "kotlin", listOf())
            }

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.statusCode)
    }

    private fun setupRunTestChain(token: String) {
        whenever(restClient.post()).thenReturn(uriSpec)
        whenever(uriSpec.uri("/engine/test")).thenReturn(uriSpec)
        whenever(uriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.body(any<TestInput>()))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.retrieve()).thenReturn(responseSpec)
    }

    @Test
    fun `runTest - Success returns TestResponse with passed true`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupRunTestChain(token)

        val expectedResponse = TestResponse(passed = true, failedAt = null)
        whenever(responseSpec.body(TestResponse::class.java))
            .thenReturn(expectedResponse)

        val result = service.runTest("kotlin", "1", "path", listOf("in"), listOf("out"))

        assertTrue(result.passed)
        assertEquals(null, result.failedAt)
    }

    @Test
    fun `runTest - Success returns TestResponse with passed false`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupRunTestChain(token)

        val expectedResponse = TestResponse(passed = false, failedAt = 2)
        whenever(responseSpec.body(TestResponse::class.java))
            .thenReturn(expectedResponse)

        val result = service.runTest("kotlin", "1", "path", listOf("in"), listOf("out"))

        assertFalse(result.passed)
        assertEquals(2, result.failedAt)
    }

    @Test
    fun `runTest - BadRequest throws 400`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupRunTestChain(token)

        val original = HttpClientErrorException(HttpStatus.BAD_REQUEST, "error")
        whenever(responseSpec.body(TestResponse::class.java)).thenThrow(original)

        val ex =
            assertThrows<ResponseStatusException> {
                service.runTest("kotlin", "1", "path", listOf(), listOf())
            }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `runTest - Null body throws 500`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupRunTestChain(token)

        whenever(responseSpec.body(TestResponse::class.java)).thenReturn(null)

        val ex =
            assertThrows<ResponseStatusException> {
                service.runTest("kotlin", "1", "path", listOf(), listOf())
            }

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
        assertTrue(ex.reason!!.contains("Testing service returned empty response"))
    }

    private fun setupFormatChain(token: String) {
        whenever(restClient.post()).thenReturn(uriSpec)
        whenever(uriSpec.uri("/engine/format")).thenReturn(uriSpec)
        whenever(uriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.body(any<FormatUniqueInputForEngine>()))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.retrieve()).thenReturn(responseSpec)
    }

    @Test
    fun `formatUnique - Success returns formatted code`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupFormatChain(token)

        val formattedCode = "val x = 1\n"
        whenever(responseSpec.body(String::class.java)).thenReturn(formattedCode)

        val input =
            FormatUniqueInputForEngine(
                language = "kotlin",
                version = "1",
                config = FormatConfig(),
                code = "val x=1",
            )
        val result = service.formatUnique(input)

        assertEquals(formattedCode, result)
    }

    @Test
    fun `formatUnique - BadRequest throws 400`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupFormatChain(token)

        val original = HttpClientErrorException(HttpStatus.BAD_REQUEST, "error")
        whenever(responseSpec.body(String::class.java)).thenThrow(original)

        val input =
            FormatUniqueInputForEngine(
                language = "kotlin",
                version = "1",
                config = FormatConfig(),
                code = "val x=1",
            )

        val ex =
            assertThrows<ResponseStatusException> {
                service.formatUnique(input)
            }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `formatUnique - Null body throws 500`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupFormatChain(token)

        whenever(responseSpec.body(String::class.java)).thenReturn(null)

        val input =
            FormatUniqueInputForEngine(
                language = "kotlin",
                version = "1",
                config = FormatConfig(),
                code = "val x=1",
            )

        val ex =
            assertThrows<ResponseStatusException> {
                service.formatUnique(input)
            }

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
        assertTrue(ex.reason!!.contains("Formatting service returned empty response"))
    }

    @Test
    fun `formatUnique - HttpServerErrorException maps to 503`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupFormatChain(token)

        val original = HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "error")
        whenever(responseSpec.body(String::class.java)).thenThrow(original)

        val input =
            FormatUniqueInputForEngine(
                language = "kotlin",
                version = "1",
                config = FormatConfig(),
                code = "val x=1",
            )

        val ex =
            assertThrows<ResponseStatusException> {
                service.formatUnique(input)
            }

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.statusCode)
        assertTrue(ex.reason!!.contains("Formatting service unavailable"))
    }

    private fun setupLintChain(token: String) {
        whenever(restClient.post()).thenReturn(uriSpec)
        whenever(uriSpec.uri("/engine/analyze")).thenReturn(uriSpec)
        whenever(uriSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.body(any<LintUniqueInputForEngine>()))
            .thenReturn(requestBodySpec)
        whenever(requestBodySpec.retrieve()).thenReturn(responseSpec)
    }

    @Test
    fun `lintUnique - Success returns LintResponse`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupLintChain(token)

        val lintErrors =
            listOf(
                LintErrorResponse("Error 1", 1, 5),
                LintErrorResponse("Error 2", 3, 10),
            )
        val expectedResponse = LintResponse(lintErrors)
        whenever(responseSpec.body(LintResponse::class.java))
            .thenReturn(expectedResponse)

        val result = service.lintUnique(LintConfig(), "path", "kotlin", "1")

        assertEquals(2, result.lintErrors.size)
        assertEquals("Error 1", result.lintErrors[0].message)
        assertEquals(1, result.lintErrors[0].line)
    }

    @Test
    fun `lintUnique - BadRequest throws 400`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupLintChain(token)

        val original = HttpClientErrorException(HttpStatus.BAD_REQUEST, "error")
        whenever(responseSpec.body(LintResponse::class.java)).thenThrow(original)

        val ex =
            assertThrows<ResponseStatusException> {
                service.lintUnique(LintConfig(), "path", "kotlin", "1")
            }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `lintUnique - Null body throws 500`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupLintChain(token)

        whenever(responseSpec.body(LintResponse::class.java)).thenReturn(null)

        val ex =
            assertThrows<ResponseStatusException> {
                service.lintUnique(LintConfig(), "path", "kotlin", "1")
            }

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
        assertTrue(ex.reason!!.contains("Linting service returned empty response"))
    }

    @Test
    fun `lintUnique - HttpClientErrorException with 404 returns same status`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupLintChain(token)

        val original =
            HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "not found",
                HttpHeaders(),
                "body".toByteArray(),
                null,
            )
        whenever(responseSpec.body(LintResponse::class.java)).thenThrow(original)

        val ex =
            assertThrows<ResponseStatusException> {
                service.lintUnique(LintConfig(), "path", "kotlin", "1")
            }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        assertTrue(ex.reason!!.contains("Linting service error"))
    }

    @Test
    fun `lintUnique - HttpServerErrorException maps to 503`() {
        val token = "token"
        whenever(auth0Service.getM2MToken()).thenReturn(token)
        setupLintChain(token)

        val original = HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "error")
        whenever(responseSpec.body(LintResponse::class.java)).thenThrow(original)

        val ex =
            assertThrows<ResponseStatusException> {
                service.lintUnique(LintConfig(), "path", "kotlin", "1")
            }

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.statusCode)
        assertTrue(ex.reason!!.contains("Linting service unavailable"))
    }
}
