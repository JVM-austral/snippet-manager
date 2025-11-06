package manager.service

import manager.entity.Snippet
import manager.entity.TestEntity
import manager.inputs.testing.CreateTestRequest
import manager.inputs.testing.RunTestRequest
import manager.repository.snippet.SnippetRepositoryInterface
import manager.repository.testing.TestingRepositoryInterface
import manager.service.engine.EngineService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class TestingService(
    private val snippetRepository: SnippetRepositoryInterface,
    private val testingRepository: TestingRepositoryInterface,
    private val engineService: EngineService,
    private val authorizationService: AuthorizationService,
) {
    fun createTest(
        request: CreateTestRequest,
        userId: String,
        userToken: String,
    ): String {
        authorizationService.checkWritePermises(
            token = userToken,
            userId = userId,
            snippetId = request.snippetId,
        )
        validateSnippetExists(request.snippetId)
        val testId =
            testingRepository.saveTest(
                snippetId = request.snippetId,
                name = request.name,
                input = request.input,
                output = request.output,
            )

        return testId
    }

    fun runTest(
        request: RunTestRequest,
        userId: String,
        userToken: String,
    ): String {
        val test = validateTestExists(request.testId)
        val snippet = validateSnippetExists(test.snippetId)

        authorizationService.checkWritePermises(
            token = userToken,
            userId = userId,
            snippetId = snippet.id,
        )
        val response = engineService.runTest(
            assetPath = snippet.bucketId,
            version = snippet.version,
            language = snippet.language.name,
            varInputs =  test.input,
            expectedOutputs = test.output,
        )
        if (response.passed) {
            return "Test passed successfully"
        } else {
            return "Test failed."
        }
    }

    private fun validateSnippetExists(snippetId: String): Snippet {
        val snippet =
            snippetRepository.getSnippetById(snippetId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found with id: $snippetId")
        return snippet
    }

    private fun validateTestExists(testId: String): TestEntity =
        testingRepository.getTestById(testId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found with id: $testId")
}
