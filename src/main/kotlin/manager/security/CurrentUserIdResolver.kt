package manager.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserIdResolver : HandlerMethodArgumentResolver {
    private val secret = "js8qUWdtEoPBFtqSP11xK3bU2zCw5m4J7U93ZhF3V9k"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUserId::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
    ): Any {
        val authHeader =
            webRequest.getHeader("Authorization")
                ?: throw IllegalArgumentException("Falta el header Authorization")

        val token = authHeader.removePrefix("Bearer ").trim()

        val claims =
            Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body

        return claims["id"] ?: throw IllegalArgumentException("No se encontró el campo 'id' en el token")
    }
}
