package manager.service.engine

import manager.inputs.snippet.ParseRequest
import manager.inputs.snippet.RunSnippetInEngineRequest
import manager.outputs.snippet.RunSnippetResponse
import manager.repository.lint.LintConfig
import manager.service.engine.inputs.FormatUniqueInputForEngine
import manager.service.engine.inputs.LintUniqueInputForEngine
import manager.service.engine.inputs.TestInput
import manager.service.engine.response.LintResponse
import manager.service.engine.response.ParseResponse
import manager.service.engine.response.TestResponse
import manager.service.oauth.Auth0Service
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException

@Service
class EngineService(
    private val auth0Service: Auth0Service,
) : EngineServiceInterface {
    override fun validateSnippet(
        path: String,
        version: String,
        language: String,
    ): List<String> {
        val m2mToken = auth0Service.getM2MToken()

        val client =
            RestClient
                .builder()
                .baseUrl("http://snippet-engine-service:8080")
                .build()

        try {
            val parseResponse: ParseResponse =
                client
                    .post()
                    .uri("/engine/parse")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken")
                    .body(
                        ParseRequest(
                            assetPath = path,
                            language = language,
                            version = version,
                        ),
                    ).retrieve()
                    .body(ParseResponse::class.java)
                    ?: throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Parser service returned empty response",
                    )
            return parseResponse.parseErrors
        } catch (e: HttpClientErrorException.BadRequest) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Snippet validation failed: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Parser service error: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Parser service unavailable: ${e.message}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error calling parser service: ${e.message}",
                e,
            )
        }
    }

    override fun runSnippet(
        path: String,
        version: String,
        language: String,
        inputs: List<String>,
    ): RunSnippetResponse {
        val m2mToken = auth0Service.getM2MToken()

        val client =
            RestClient
                .builder()
                .baseUrl("http://snippet-engine-service:8080")
                .build()

        try {
            val executeResponse: RunSnippetResponse =
                client
                    .post()
                    .uri("/engine/execute")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken")
                    .body(
                        RunSnippetInEngineRequest(
                            assetPath = path,
                            language = language,
                            version = version,
                            varInputs = inputs,
                        ),
                    ).retrieve()
                    .body(RunSnippetResponse::class.java)
                    ?: throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Runner service returned empty response",
                    )
            return executeResponse
        } catch (e: HttpClientErrorException.BadRequest) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Snippet validation failed: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Runner service error: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Runner service unavailable: ${e.message}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error calling Runner service: ${e.message}",
                e,
            )
        }
    }

    override fun runTest(
        language: String,
        version: String,
        assetPath: String,
        varInputs: List<String>,
        expectedOutputs: List<String>,
    ): TestResponse {
        val m2mToken = auth0Service.getM2MToken()

        val client =
            RestClient
                .builder()
                .baseUrl("http://snippet-engine-service:8080")
                .build()

        try {
            val executeResponse: TestResponse =
                client
                    .post()
                    .uri("/engine/test")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken")
                    .body(
                        TestInput(
                            assetPath = assetPath,
                            language = language,
                            version = version,
                            varInputs = varInputs,
                            expectedOutputs = expectedOutputs,
                        ),
                    ).retrieve()
                    .body(TestResponse::class.java)
                    ?: throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Testing service returned empty response",
                    )
            return executeResponse
        } catch (e: HttpClientErrorException.BadRequest) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Snippet Testing failed: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Testing service error: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Testing service unavailable: ${e.message}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error calling Testing service: ${e.message}",
                e,
            )
        }
    }

    override fun formatUnique(input: FormatUniqueInputForEngine): String {
        val m2mToken = auth0Service.getM2MToken()

        val client =
            RestClient
                .builder()
                .baseUrl("http://snippet-engine-service:8080")
                .build()
        try {
            val formatResponse: String =
                client
                    .post()
                    .uri("/engine/format")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken")
                    .body(
                        FormatUniqueInputForEngine(
                            language = input.language,
                            version = input.version,
                            config = input.config,
                            code = input.code,
                        ),
                    ).retrieve()
                    .body(String::class.java) ?: throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Formatting service returned empty response",
                )
            return formatResponse
        } catch (e: HttpClientErrorException.BadRequest) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Snippet formatting failed: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Formatting service error: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Formatting service unavailable: ${e.message}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error calling Formatting service: ${e.message}",
                e,
            )
        }
    }

    override fun lintUnique(
        config: LintConfig,
        assetPath: String,
        language: String,
        version: String,
    ): LintResponse {
        val m2mToken = auth0Service.getM2MToken()

        val client =
            RestClient
                .builder()
                .baseUrl("http://snippet-engine-service:8080")
                .build()
        try {
            val lintErrorResponses: LintResponse =
                client
                    .post()
                    .uri("/engine/analyze")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $m2mToken")
                    .body(
                        LintUniqueInputForEngine(
                            language = language,
                            version = version,
                            config = config,
                            assetPath = assetPath,
                        ),
                    ).retrieve()
                    .body(LintResponse::class.java)
                    ?: throw ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Linting service returned empty response",
                    )

            return lintErrorResponses
        } catch (e: HttpClientErrorException.BadRequest) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Snippet linting failed: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpClientErrorException) {
            throw ResponseStatusException(
                HttpStatus.valueOf(e.statusCode.value()),
                "Linting service error: ${e.responseBodyAsString}",
                e,
            )
        } catch (e: HttpServerErrorException) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Linting service unavailable: ${e.message}",
                e,
            )
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error calling Linting service: ${e.message}",
                e,
            )
        }
    }
}
