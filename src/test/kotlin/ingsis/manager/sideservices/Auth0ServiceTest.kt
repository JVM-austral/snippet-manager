package ingsis.manager.sideservices

import manager.service.oauth.Auth0Service
import org.junit.jupiter.api.Test
import org.mockito.MockedConstruction
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.`when`
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.assertEquals

class Auth0ServiceTest {
    private val service =
        Auth0Service(
            domain = "https://testdomain.com",
            clientId = "abc",
            clientSecret = "123",
            audience = "aud",
            managementApiAudience = "aud2",
        )

    @Test
    fun `getM2MToken OK`() {
        // Mock que vamos a devolver siempre
        val mockConnection = mock(HttpURLConnection::class.java)

        val body = """{"access_token":"TOKEN_TEST"}"""

        `when`(mockConnection.responseCode).thenReturn(200)
        `when`(mockConnection.inputStream).thenReturn(body.byteInputStream())
        `when`(mockConnection.outputStream).thenReturn(mock(OutputStream::class.java))

        // Cada vez que tu código haga: URL("algo")
        // Mockito va a construir un mock de URL y te da acceso al objeto
        val construction: MockedConstruction<URL> =
            mockConstruction(URL::class.java) { urlMock, _ ->
                // Para cualquier URL construida, openConnection() devuelve tu mock
                `when`(urlMock.openConnection()).thenReturn(mockConnection)
            }

        try {
            val service =
                Auth0Service(
                    domain = "https://testdomain.com",
                    clientId = "abc",
                    clientSecret = "123",
                    audience = "aud",
                    managementApiAudience = "aud2",
                )

            val token = service.getM2MToken()

            assertEquals("TOKEN_TEST", token)
        } finally {
            construction.close()
        }
    }

    @Test
    fun `getUserName OK`() {
        val connToken = mock(HttpURLConnection::class.java)
        val connUser = mock(HttpURLConnection::class.java)

        // Config token
        `when`(connToken.outputStream).thenReturn(mock(OutputStream::class.java))
        `when`(connToken.responseCode).thenReturn(200)
        `when`(connToken.inputStream).thenReturn("""{"access_token":"TOKEN123"}""".byteInputStream())

        // Config user info
        `when`(connUser.responseCode).thenReturn(200)
        `when`(connUser.inputStream).thenReturn("""{"username":"vito"}""".byteInputStream())

        var callCount = 0

        val construction =
            mockConstruction(URL::class.java) { urlMock, _ ->
                `when`(urlMock.openConnection()).thenAnswer {
                    if (callCount++ == 0) connToken else connUser
                }
            }

        try {
            val service =
                Auth0Service(
                    domain = "https://testdomain.com",
                    clientId = "abc",
                    clientSecret = "123",
                    audience = "aud",
                    managementApiAudience = "aud2",
                )

            val name = service.getUserName("123")
            assertEquals("vito", name)
        } finally {
            construction.close()
        }
    }
}
