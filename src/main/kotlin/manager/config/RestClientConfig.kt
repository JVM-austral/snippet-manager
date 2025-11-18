package manager.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientConfig {
    @Bean
    fun authorizationRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("http://authorization-service:8080")
            .build()

    @Bean
    fun assetRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("http://asset-service:8080")
            .build()

    @Bean
    fun engineRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("http://snippet-engine-service:8080")
            .build()
}
