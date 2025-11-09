package manager.service.app

import manager.entity.Snippet
import manager.entity.TestEntity
import manager.inputs.testing.CreateTestRequest
import manager.inputs.testing.EditTestRequest
import manager.inputs.testing.IdTestRequest
import manager.repository.snippet.SnippetRepositoryInterface
import manager.repository.testing.TestingRepositoryInterface
import manager.service.authorization.AuthorizationServiceInterface
import manager.service.engine.EngineServiceInterface
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class TestingService(
    private val snippetRepository: SnippetRepositoryInterface,
    private val testingRepository: TestingRepositoryInterface,
    private val engineService: EngineServiceInterface,
    private val authorizationService: AuthorizationServiceInterface,
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
        request: IdTestRequest,
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
        val response =
            engineService.runTest(
                assetPath = snippet.content,
                version = snippet.version,
                language = snippet.language.name,
                varInputs = test.input,
                expectedOutputs = test.output,
            )
        if (response.passed) {
            return "Test passed successfully"
        } else {
            return "Test failed."
        }
    }

    fun deleteTest(
        request: IdTestRequest,
        userId: String,
        userToken: String,
    ) {
        val test = validateTestExists(request.testId)
        val snippet = validateSnippetExists(test.snippetId)

        authorizationService.checkWritePermises(
            token = userToken,
            userId = userId,
            snippetId = snippet.id,
        )
        testingRepository.deleteTest(request.testId)
    }

    fun editTest(
        request: EditTestRequest,
        userId: String,
        userToken: String,
    ) {
        val test = validateTestExists(request.testId)
        val snippet = validateSnippetExists(test.snippetId)

        if (test.snippetId != request.snippetId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change snippetId of the test")
        }

        authorizationService.checkWritePermises(
            token = userToken,
            userId = userId,
            snippetId = snippet.id,
        )
        testingRepository.updateTest(
            testId = request.testId,
            name = request.name,
            input = request.input,
            output = request.output,
        )
    }

    fun getAllTestsBySnippetId(
        snippetId: String,
        userId: String,
        userToken: String,
    ): List<TestEntity> {
        validateSnippetExists(snippetId)
        authorizationService.checkReadPermises(
            token = userToken,
            userId = userId,
            snippetId = snippetId,
        )
        return testingRepository.getAllTestsBySnippetId(snippetId)
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
