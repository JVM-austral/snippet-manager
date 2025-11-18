package ingsis.manager.sideservices

import manager.service.asset.AssetService
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals

class AssetServiceTest {
    private val restClient: RestClient = mock()

    private val putSpec: RestClient.RequestBodyUriSpec = mock()
    private val putUriSpec: RestClient.RequestBodySpec = mock()
    private val putBodySpec: RestClient.RequestBodySpec = mock()
    private val putRetrieveSpec: RestClient.ResponseSpec = mock()

    private val getSpec: RestClient.RequestHeadersUriSpec<*> = mock()
    private val getRetrieveSpec: RestClient.ResponseSpec = mock()

    private val deleteSpec: RestClient.RequestHeadersUriSpec<*> = mock()
    private val deleteRetrieveSpec: RestClient.ResponseSpec = mock()

    private val service = AssetService(restClient)

    @Test
    fun `createAsset returns correct message for 201`() {
        whenever(restClient.put()).thenReturn(putSpec)
        whenever(putSpec.uri("/v1/asset/container/key")).thenReturn(putUriSpec)
        whenever(putUriSpec.body("data")).thenReturn(putBodySpec)
        whenever(putBodySpec.retrieve()).thenReturn(putRetrieveSpec)

        val response = ResponseEntity("ok", HttpStatus.CREATED)
        whenever(putRetrieveSpec.toEntity(String::class.java)).thenReturn(response)

        val result = service.createAsset("container", "key", "data")

        assertEquals("Asset creado correctamente en container/key", result)
    }

    @Test
    fun `createAsset returns correct message for 200`() {
        whenever(restClient.put()).thenReturn(putSpec)
        whenever(putSpec.uri("/v1/asset/container/key")).thenReturn(putUriSpec)
        whenever(putUriSpec.body("data")).thenReturn(putBodySpec)
        whenever(putBodySpec.retrieve()).thenReturn(putRetrieveSpec)

        val response = ResponseEntity("ok", HttpStatus.OK)
        whenever(putRetrieveSpec.toEntity(String::class.java)).thenReturn(response)

        val result = service.createAsset("container", "key", "data")

        assertEquals("Asset actualizado correctamente en container/key", result)
    }

    @Test
    fun `createAsset throws when unexpected status code`() {
        whenever(restClient.put()).thenReturn(putSpec)
        whenever(putSpec.uri("/v1/asset/container/key")).thenReturn(putUriSpec)
        whenever(putUriSpec.body("data")).thenReturn(putBodySpec)
        whenever(putBodySpec.retrieve()).thenReturn(putRetrieveSpec)

        val response = ResponseEntity("ok", HttpStatus.ACCEPTED)
        whenever(putRetrieveSpec.toEntity(String::class.java)).thenReturn(response)

        val ex =
            assertThrows(RuntimeException::class.java) {
                service.createAsset("container", "key", "data")
            }

        assertTrue(ex.message!!.contains("Respuesta inesperada"))
    }

    @Test
    fun `createAsset throws when RestClient fails`() {
        whenever(restClient.put()).thenReturn(putSpec)

        whenever(putSpec.uri(any<String>())).thenThrow(RuntimeException("boom"))

        val ex =
            assertThrows(RuntimeException::class.java) {
                service.createAsset("container", "key", "data")
            }

        assertEquals("boom", ex.message)
    }

    @Test
    fun `getAsset returns body correctly`() {
        whenever(restClient.get()).thenReturn(getSpec)
        whenever(getSpec.uri("/v1/asset/container/key")).thenReturn(getSpec)
        whenever(getSpec.retrieve()).thenReturn(getRetrieveSpec)

        val response = ResponseEntity("contenido", HttpStatus.OK)
        whenever(getRetrieveSpec.toEntity(String::class.java)).thenReturn(response)

        val result = service.getAsset("container", "key")

        assertEquals("contenido", result)
    }

    @Test
    fun `getAsset throws when body is null`() {
        whenever(restClient.get()).thenReturn(getSpec)
        whenever(getSpec.uri("/v1/asset/container/key")).thenReturn(getSpec)
        whenever(getSpec.retrieve()).thenReturn(getRetrieveSpec)

        val response = ResponseEntity<String>(null, HttpStatus.OK)
        whenever(getRetrieveSpec.toEntity(String::class.java)).thenReturn(response)

        val ex =
            assertThrows(RuntimeException::class.java) {
                service.getAsset("container", "key")
            }

        assertEquals("Asset no encontrado", ex.message)
    }

    @Test
    fun `getAsset throws on RestClient failure`() {
        whenever(restClient.get()).thenReturn(getSpec)
        whenever(getSpec.uri(any<String>())).thenThrow(RuntimeException("fail"))

        val ex =
            assertThrows(RuntimeException::class.java) {
                service.getAsset("container", "key")
            }

        assertEquals("fail", ex.message)
    }

    @Test
    fun `deleteAsset returns correct message`() {
        whenever(restClient.delete()).thenReturn(deleteSpec)
        whenever(deleteSpec.uri("/v1/asset/container/key")).thenReturn(deleteSpec)
        whenever(deleteSpec.retrieve()).thenReturn(deleteRetrieveSpec)
        whenever(deleteRetrieveSpec.toEntity(String::class.java))
            .thenReturn(ResponseEntity("ok", HttpStatus.OK))

        val result = service.deleteAsset("container", "key")

        assertEquals("Asset key eliminado de container", result)
    }

    @Test
    fun `deleteAsset throws on RestClient error`() {
        whenever(restClient.delete()).thenThrow(RuntimeException("problem"))

        val ex =
            assertThrows(RuntimeException::class.java) {
                service.deleteAsset("container", "key")
            }

        assertEquals("problem", ex.message)
    }
}
