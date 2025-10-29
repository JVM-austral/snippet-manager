package manager.security

import manager.security.auth.response.GetTokenResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class ValidateTokenService {
    fun getUserInfo(token: String): String {
        try {
            val authorizationClient =
                RestClient
                    .builder()
                    .baseUrl("http://authorization_service:8080/authentication/validate-user")
                    .build()

            val response: GetTokenResponse =
                authorizationClient
                    .get()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .body(GetTokenResponse::class.java)!!

            return response.subject
        } catch (e: Exception) {
            throw RuntimeException("Error validating user: ${e.message}")
        }
    }
}
