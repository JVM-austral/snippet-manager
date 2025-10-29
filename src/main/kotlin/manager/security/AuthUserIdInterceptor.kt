package manager.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import manager.security.auth.response.GetTokenResponse
import org.apache.http.HttpHost
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthUserIdInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val authHeader = request.getHeader("Authorization") ?: return true
        val token = authHeader.removePrefix("Bearer ").trim()

        println("PRINTLN DEL TOKEN EN INTERCEPTOR$token")

        val userId = getUserInfo(token)

        println("PRINTLN DEL USERID EN INTERCEPTOR$userId")

        request.setAttribute("userId", userId)

        return true
    }

    private fun getRestClient(): RestClient =
        RestClient
            .builder(
                HttpHost("authorization_service", 8080),
            ).build()

    private fun getUserInfo(token: String): String {
        val restClient = getRestClient()
        try {
            val request = Request("GET", "/authentication/validate-user")

            val options =
                RequestOptions.DEFAULT
                    .toBuilder()
                    .addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .build()
            request.options = options

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
}
