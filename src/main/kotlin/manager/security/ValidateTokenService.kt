package manager.security

import manager.security.auth.response.GetTokenResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class ValidateTokenService {
    private val authorizationClient: RestClient by lazy {
        RestClient
            .builder()
            .baseUrl("http://authorization_service:8080")
            .build()
    }

    fun getUserInfo(token: String): String {
        try {
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
