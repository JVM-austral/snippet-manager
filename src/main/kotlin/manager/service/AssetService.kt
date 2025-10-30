package manager.service

import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class AssetService {
    private val restClient =
        RestClient
            .builder()
            .baseUrl("http://asset-service:8080")
            .build()

    fun createAsset(
        container: String,
        key: String,
        data: String,
    ): String {
        val response =
            restClient
                .put()
                .uri("/v1/asset/$container/$key")
                .body(data)
                .retrieve()
                .toEntity(String::class.java)

        return when (response.statusCode.value()) {
            201 -> "Asset creado correctamente en $container/$key"
            200 -> "Asset actualizado correctamente en $container/$key"
            else -> throw RuntimeException("Respuesta inesperada: ${response.statusCode}")
        }
    }

    fun getAsset(
        container: String,
        key: String,
    ): String {
        val response =
            restClient
                .get()
                .uri("/v1/asset/$container/$key")
                .retrieve()
                .toEntity(String::class.java)

        return response.body ?: throw RuntimeException("Asset no encontrado")
    }

    fun deleteAsset(
        container: String,
        key: String,
    ): String {
        val response =
            restClient
                .delete()
                .uri("/v1/asset/$container/$key")
                .retrieve()
                .toEntity(String::class.java)

        return "Asset $key eliminado de $container"
    }
}
