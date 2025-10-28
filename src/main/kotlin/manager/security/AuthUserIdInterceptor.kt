package manager.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import manager.security.auth.response.GetTokenResponse
import org.springframework.web.client.RestClient
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
        val baseUrl = "http://authorization_service:8080"

        val restClient = RestClient.create(baseUrl)

        val getTokenResponse = restClient.get()
            .uri("/authentication/validate-user")

            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")

            .retrieve()

            .body(GetTokenResponse::class.java)

            ?: throw IllegalStateException("No se pudo obtener información del usuario desde Auth0")

        return getTokenResponse.subject
    }
}
