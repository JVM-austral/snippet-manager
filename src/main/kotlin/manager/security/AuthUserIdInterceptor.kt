package manager.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import manager.security.auth.response.GetTokenResponse
import org.apache.http.HttpHost
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.Request
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

    private fun getUserInfo(token: String): String {
        val restClient =
            RestClient
                .builder(
                    HttpHost("authorization_service", 8080),
                ).build()

        try {
            val request = Request("GET", "/authentication/validate-user")
            request.addParameter(HttpHeaders.AUTHORIZATION, "Bearer $token")

            val response = restClient.performRequest(request)
            val responseBody =
                EntityUtils
                    .toString(response.entity)

            val mapper =
                ObjectMapper()
            val getTokenResponse =
                mapper.readValue(responseBody, GetTokenResponse::class.java)
                    ?: throw IllegalStateException("No se pudo obtener información del usuario desde Auth0")

            return getTokenResponse.subject
        } finally {
            restClient.close()
        }
    }
}
