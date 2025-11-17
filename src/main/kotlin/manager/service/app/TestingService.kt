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
    private val log = org.slf4j.LoggerFactory.getLogger(TestingService::class.java)

    fun createTest(
        request: CreateTestRequest,
        userId: String,
        userToken: String,
    ): String {
        log.info("Creating test for id: ${request.snippetId} by userId: $userId")
        try {
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

            log.info("Successfully created test with ID: $testId for id: ${request.snippetId}")
            return testId
        } catch (e: Exception) {
            log.warn("Error creating test for id: ${request.snippetId} - ${e.message}", e)
            throw e
        }
    }

    fun runTest(
        request: IdTestRequest,
        userId: String,
        userToken: String,
    ): String {
        log.info("Running test with testId: ${request.testId} by userId: $userId")
        try {
            val test = validateTestExists(request.testId)
            val snippet = validateSnippetExists(test.snippetId)

            authorizationService.checkWritePermises(
                token = userToken,
                userId = userId,
                snippetId = snippet.id,
            )
            val response =
                engineService.runTest(
                    assetPath = snippet.bucketId,
                    version = snippet.version,
                    language = snippet.language.name,
                    varInputs = test.input,
                    expectedOutputs = test.output,
                )
            if (response.passed) {
                log.info("Test ${request.testId} passed successfully")
                return "Test passed successfully"
            } else {
                log.warn("Test ${request.testId} failed")
                return "Test failed."
            }
        } catch (e: Exception) {
            log.warn("Error running test ${request.testId} - ${e.message}", e)
            throw e
        }
    }

    fun deleteTest(
        request: IdTestRequest,
        userId: String,
        userToken: String,
    ) {
        log.info("Deleting test with testId: ${request.testId} by userId: $userId")
        try {
            val test = validateTestExists(request.testId)
            val snippet = validateSnippetExists(test.snippetId)

            authorizationService.checkWritePermises(
                token = userToken,
                userId = userId,
                snippetId = snippet.id,
            )
            testingRepository.deleteTest(request.testId)
            log.info("Successfully deleted test ${request.testId}")
        } catch (e: Exception) {
            log.warn("Error deleting test ${request.testId} - ${e.message}", e)
            throw e
        }
    }

    fun editTest(
        request: EditTestRequest,
        userId: String,
        userToken: String,
    ) {
        log.info("Editing test with testId: ${request.testId} by userId: $userId")
        try {
            val test = validateTestExists(request.testId)
            val snippet = validateSnippetExists(test.snippetId)

            if (test.snippetId != request.snippetId) {
                log.warn("Attempt to change id for test ${request.testId}")
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change id of the test")
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
            log.info("Successfully edited test ${request.testId}")
        } catch (e: Exception) {
            log.warn("Error editing test ${request.testId} - ${e.message}", e)
            throw e
        }
    }

    fun getAllTestsBySnippetId(
        snippetId: String,
        userId: String,
        userToken: String,
    ): List<TestEntity> {
        log.info("Getting all tests for id: $snippetId by userId: $userId")
        try {
            validateSnippetExists(snippetId)
            authorizationService.checkReadPermises(
                token = userToken,
                userId = userId,
                snippetId = snippetId,
            )
            val tests = testingRepository.getAllTestsBySnippetId(snippetId)
            log.info("Retrieved ${tests.size} tests for id: $snippetId")
            return tests
        } catch (e: Exception) {
            log.warn("Error getting tests for id: $snippetId - ${e.message}", e)
            throw e
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
