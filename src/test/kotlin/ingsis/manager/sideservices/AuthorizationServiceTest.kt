package ingsis.manager.sideservices

import manager.inputs.snippet.PermissionRequest
import manager.outputs.snippet.CheckPermisesResponse
import manager.outputs.snippet.SnippetPermisesResponse
import manager.service.authorization.AuthorizationService
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals

class AuthorizationServiceTest {
    private val restClient: RestClient = mock()

    private val postSpec: RestClient.RequestBodyUriSpec = mock()
    private val uriSpec: RestClient.RequestBodyUriSpec = mock()
    private val bodySpec: RestClient.RequestBodySpec = mock()
    private val headersSpec: RestClient.RequestBodySpec = mock()
    private val responseSpec: RestClient.ResponseSpec = mock()

    private val getSpec: RestClient.RequestHeadersUriSpec<*> = mock()
    private val getHeaderSpec: RestClient.RequestHeadersSpec<*> = mock()

    private val service = AuthorizationService(restClient)

    private fun mockPost(baseUri: String) {
        whenever(restClient.post()).thenReturn(postSpec)
        whenever(postSpec.uri(baseUri)).thenReturn(uriSpec)
        whenever(uriSpec.header(any(), any())).thenReturn(bodySpec)
        whenever(bodySpec.body(any<PermissionRequest>())).thenReturn(headersSpec)
        whenever(headersSpec.retrieve()).thenReturn(responseSpec)
    }

    @Test
    fun `grantReadPermises OK`() {
        val expected = SnippetPermisesResponse("1", "snip", "user", "READ")
        mockPost("/snippet-permissions/grant-read-access")

        whenever(responseSpec.body(SnippetPermisesResponse::class.java))
            .thenReturn(expected)

        val result = service.grantReadPermises("abc", "user", "snip")
        assertEquals(expected, result)
    }

    @Test
    fun `grantReadPermises HttpClientErrorException`() {
        mockPost("/snippet-permissions/grant-read-access")

        whenever(responseSpec.body(SnippetPermisesResponse::class.java))
            .thenThrow(HttpClientErrorException(HttpStatus.BAD_REQUEST, "bad"))

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                service.grantReadPermises("abc", "user", "snip")
            }
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `grantReadPermises generic exception`() {
        mockPost("/snippet-permissions/grant-read-access")

        whenever(responseSpec.body(SnippetPermisesResponse::class.java))
            .thenThrow(RuntimeException("boom"))

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                service.grantReadPermises("abc", "user", "snip")
            }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
    }

    @Test
    fun `grantWritePermises OK`() {
        val expected = SnippetPermisesResponse("1", "snip", "user", "WRITE")
        mockPost("/snippet-permissions/grant-write-access")

        whenever(responseSpec.body(SnippetPermisesResponse::class.java))
            .thenReturn(expected)

        val result = service.grantWritePermises("abc", "user", "snip")
        assertEquals(expected, result)
    }

    @Test
    fun `grantWritePermises HttpClientError`() {
        mockPost("/snippet-permissions/grant-write-access")

        whenever(responseSpec.body(SnippetPermisesResponse::class.java))
            .thenThrow(HttpClientErrorException(HttpStatus.FORBIDDEN, "no"))

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                service.grantWritePermises("abc", "user", "snip")
            }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }

    @Test
    fun `grantWritePermises generic error`() {
        mockPost("/snippet-permissions/grant-write-access")

        whenever(responseSpec.body(SnippetPermisesResponse::class.java))
            .thenThrow(RuntimeException("fail"))

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                service.grantWritePermises("abc", "user", "snip")
            }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
    }

    @Test
    fun `checkWritePermises allowed`() {
        mockPost("/snippet-permissions/validate-write")

        whenever(responseSpec.body(CheckPermisesResponse::class.java))
            .thenReturn(CheckPermisesResponse(true))

        assertDoesNotThrow {
            service.checkWritePermises("abc", "user", "snip")
        }
    }

    @Test
    fun `checkWritePermises forbidden`() {
        mockPost("/snippet-permissions/validate-write")

        whenever(responseSpec.body(CheckPermisesResponse::class.java))
            .thenReturn(CheckPermisesResponse(false))

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                service.checkWritePermises("abc", "user", "snip")
            }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
    }

    @Test
    fun `checkWritePermises HttpClientError`() {
        mockPost("/snippet-permissions/validate-write")

        whenever(responseSpec.body(CheckPermisesResponse::class.java))
            .thenThrow(HttpClientErrorException(HttpStatus.UNAUTHORIZED, "nope"))

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                service.checkWritePermises("abc", "user", "snip")
            }
        assertEquals(HttpStatus.UNAUTHORIZED, ex.statusCode)
    }

    @Test
    fun `checkReadPermises allowed`() {
        mockPost("/snippet-permissions/validate-read-access")

        whenever(responseSpec.body(CheckPermisesResponse::class.java))
            .thenReturn(CheckPermisesResponse(true))

        assertDoesNotThrow {
            service.checkReadPermises("abc", "user", "snip")
        }
    }

    @Test
    fun `checkReadPermises forbidden`() {
        mockPost("/snippet-permissions/validate-read-access")

        whenever(responseSpec.body(CheckPermisesResponse::class.java))
            .thenReturn(CheckPermisesResponse(false))

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                service.checkReadPermises("abc", "user", "snip")
            }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
    }

    @Test
    fun `getSharedSnippets OK`() {
        whenever(restClient.get()).thenReturn(getSpec)
        whenever(getSpec.uri(any<String>())).thenReturn(getHeaderSpec)
        whenever(getHeaderSpec.header(any(), any())).thenReturn(getHeaderSpec)
        whenever(getHeaderSpec.retrieve()).thenReturn(responseSpec)

        whenever(responseSpec.body(any<ParameterizedTypeReference<List<String>>>()))
            .thenReturn(listOf("s1", "s2"))

        val result = service.getSharedSnippets("tok", "user")
        assertEquals(listOf("s1", "s2"), result)
    }

    @Test
    fun `getSharedSnippets empty list`() {
        whenever(restClient.get()).thenReturn(getSpec)
        whenever(getSpec.uri(any<String>())).thenReturn(getHeaderSpec)
        whenever(getHeaderSpec.header(any(), any())).thenReturn(getHeaderSpec)
        whenever(getHeaderSpec.retrieve()).thenReturn(responseSpec)

        whenever(responseSpec.body(any<ParameterizedTypeReference<List<String>>>()))
            .thenReturn(null)

        val result = service.getSharedSnippets("tok", "user")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getSharedSnippets HttpClientError`() {
        whenever(restClient.get()).thenReturn(getSpec)
        whenever(getSpec.uri(any<String>())).thenReturn(getHeaderSpec)
        whenever(getHeaderSpec.header(any(), any())).thenReturn(getHeaderSpec)
        whenever(getHeaderSpec.retrieve()).thenReturn(responseSpec)

        whenever(responseSpec.body(any<ParameterizedTypeReference<List<String>>>()))
            .thenThrow(HttpClientErrorException(HttpStatus.NOT_FOUND))

        val ex =
            assertThrows(ResponseStatusException::class.java) {
                service.getSharedSnippets("tok", "user")
            }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }
}
