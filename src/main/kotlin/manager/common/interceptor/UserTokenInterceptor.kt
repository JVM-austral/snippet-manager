package manager.common.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class UserTokenInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val authHeader = request.getHeader("Authorization") ?: return true
        val token = authHeader.removePrefix("Bearer ").trim()
        request.setAttribute("token", token)

        return true
    }
}
