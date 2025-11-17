package manager.service.oauth

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
    @Value("\${auth0.management-api.audience}")
    private val managementApiAudience: String,
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

    fun getUserName(id: String): String {
        val authDomain =
            if (domain.startsWith("http://") || domain.startsWith("https://")) {
                domain.trimEnd('/')
            } else {
                "https://${domain.trimEnd('/')}"
            }

        val tokenUrl = URL("$authDomain/oauth/token")
        val tokenConn = tokenUrl.openConnection() as HttpURLConnection

        tokenConn.requestMethod = "POST"
        tokenConn.setRequestProperty("Content-Type", "application/json")
        tokenConn.doOutput = true

        val body =
            JsonObject().apply {
                addProperty("client_id", clientId)
                addProperty("client_secret", clientSecret)
                addProperty("audience", managementApiAudience)
                addProperty("grant_type", "client_credentials")
            }

        tokenConn.outputStream.use { it.write(body.toString().toByteArray()) }

        if (tokenConn.responseCode != 200) {
            throw Exception("Error al obtener token (Management API): ${tokenConn.responseCode}")
        }

        val tokenResponse = tokenConn.inputStream.bufferedReader().use { it.readText() }
        val tokenJson = JsonParser.parseString(tokenResponse).asJsonObject
        val token = tokenJson["access_token"].asString

        val userUrl = URL("$authDomain/api/v2/users/$id")
        val userConn = userUrl.openConnection() as HttpURLConnection

        userConn.requestMethod = "GET"
        userConn.setRequestProperty("Authorization", "Bearer $token")

        if (userConn.responseCode != 200) {
            throw Exception("Error al obtener usuario ${userConn.responseCode}")
        }

        val userResponse = userConn.inputStream.bufferedReader().use { it.readText() }
        val userJson = JsonParser.parseString(userResponse).asJsonObject

        return when {
            userJson.has("username") -> userJson["username"].asString
            userJson.has("nickname") -> userJson["nickname"].asString
            userJson.has("name") -> userJson["name"].asString
            else -> "username_no_disponible"
        }
    }
}
