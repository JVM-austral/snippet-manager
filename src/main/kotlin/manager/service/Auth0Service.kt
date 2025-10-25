package manager.service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.springframework.beans.factory.annotation.Value

class Auth0Service(
    @Value("\${auth0.domain}")
    private val domain: String,
    @Value("\${auth0.client-id}")
    private val clientId: String,
    @Value("\${auth0.client-secret}")
    private val clientSecret: String,
    @Value("\${auth0.audience}")
    private val audience: String
) {
    fun getM2MToken(): String {
        val url = URL("https://$domain/oauth/token")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val body = JsonObject().apply {
            addProperty("client_id", clientId)
            addProperty("client_secret", clientSecret)
            addProperty("audience", audience)
            addProperty("grant_type", "client_credentials")
        }
        val bodyString = body.toString()

        val output: OutputStream = connection.outputStream
        output.write(bodyString.toByteArray())
        output.flush()
        output.close()

        val responseCode = connection.responseCode
        if (responseCode != 200) {
            throw Exception("Error al obtener token: $responseCode")
        }

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = reader.readText()
        reader.close()

        val jsonResponse = JsonParser.parseString(response).asJsonObject
        return jsonResponse.get("access_token").asString
    }
}