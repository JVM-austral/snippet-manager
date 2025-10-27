package manager.service

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

@Service
class Auth0Service(
    @Value("\${auth0.domain}")
    private val domain: String,
    @Value("\${auth0.client-id}")
    private val clientId: String,
    @Value("\${auth0.client-secret}")
    private val clientSecret: String,
    @Value("\${auth0.audience}")
    private val audience: String,
) {
    fun getM2MToken(): String {
        val authDomain =
            if (domain.startsWith("http://") || domain.startsWith("https://")) {
                domain.trimEnd('/')
            } else {
                "https://${domain.trimEnd('/')}"
            }

        val url = URL("$authDomain/oauth/token")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val body =
            JsonObject().apply {
                addProperty("client_id", clientId)
                addProperty("client_secret", clientSecret)
                addProperty("audience", audience)
                addProperty("grant_type", "client_credentials")
            }

        val bodyString = body.toString()

        connection.outputStream.use { output: OutputStream ->
            output.write(bodyString.toByteArray())
            output.flush()
        }

        if (connection.responseCode != 200) {
            throw Exception("Error al obtener token: ${connection.responseCode}")
        }

        val response =
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }

        val jsonResponse = JsonParser.parseString(response).asJsonObject

        return jsonResponse.get("access_token").asString
    }
}
