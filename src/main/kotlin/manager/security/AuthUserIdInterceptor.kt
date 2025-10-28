package manager.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import manager.security.auth.response.GetTokenResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
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
        val fullAuthUrl = "http://authorization_service:8000/authentication/validate-user"

        val response: GetTokenResponse =
            WebClient
                .builder()
                .build()
                .get()
                .uri(fullAuthUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .retrieve()
                .bodyToMono(GetTokenResponse::class.java)
                .block()
                ?: throw IllegalStateException("No se pudo obtener información del usuario desde Auth0")
        return response.subject
    }
}
