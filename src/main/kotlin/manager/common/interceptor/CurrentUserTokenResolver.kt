package manager.common.interceptor

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class CurrentUserTokenResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUserToken::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
    ): Any? {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
        return request?.getAttribute("token")
            ?: throw IllegalStateException("No se encontró el token en la request")
    }
}
