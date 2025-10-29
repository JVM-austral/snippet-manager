package manager.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import manager.security.auth.response.GetTokenResponse
import org.apache.http.HttpHost
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.servlet.HandlerInterceptor
import org.elasticsearch.client.RestClient as ESRestClient

@Component
class AuthUserIdInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val authHeader = request.getHeader("Authorization") ?: return true
        val token = authHeader.removePrefix("Bearer ").trim()

        val userId = getUserInfo2(token)

        request.setAttribute("userId", userId)

        return true
    }

    private fun getRestClient(): ESRestClient =
        ESRestClient
            .builder(
                HttpHost("asset_service", 8080),
            ).build()

    private fun getUserInfo1(token: String): String {
        val restClient = getRestClient()
        try {
            val request = Request("GET", "/authentication/validate-user")

            val options =
                RequestOptions.DEFAULT
                    .toBuilder()
                    .addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .build()
            request.options = options

            println(request)

            val response = restClient.performRequest(request)
            val status = response.statusLine.statusCode

            if (status == 200) {
                val body = EntityUtils.toString(response.entity)
                val tokenResponse = ObjectMapper().readValue(body, GetTokenResponse::class.java)
                return tokenResponse.subject
            } else {
                val body = response.entity?.let { EntityUtils.toString(it) } ?: ""
                throw RuntimeException("Unexpected response: $status $body")
            }
        } finally {
            restClient.close()
        }
    }

    fun getUserInfo2(token: String): String {
        try {
            val authorizationClient =
                RestClient
                    .builder()
                    .baseUrl("http://authorization_service:8080")
                    .build()

            val response: GetTokenResponse =
                authorizationClient
                    .get()
                    .uri("/authentication/validate-user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .body(GetTokenResponse::class.java)!!

            return response.subject
        } catch (e: Exception) {
            throw RuntimeException("Error validating user: ${e.message}")
        }
    }
}
