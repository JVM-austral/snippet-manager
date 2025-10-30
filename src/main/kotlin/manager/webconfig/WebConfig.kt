package manager.webconfig

import manager.common.interceptor.CurrentUserTokenResolver
import manager.common.interceptor.UserTokenInterceptor
import manager.security.AuthUserIdInterceptor
import manager.security.CurrentUserIdResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val currentUserIdResolver: CurrentUserIdResolver,
    private val currentUserTokenResolver: CurrentUserTokenResolver,
    private val authUserIdInterceptor: AuthUserIdInterceptor,
    private val userTokenInterceptor: UserTokenInterceptor,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserIdResolver)
        resolvers.add(currentUserTokenResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authUserIdInterceptor)
        registry.addInterceptor(userTokenInterceptor)
    }
}
