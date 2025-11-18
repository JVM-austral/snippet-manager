package ingsis.manager.testing

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import manager.common.interceptor.CurrentUserTokenResolver
import manager.controller.TestingController
import manager.entity.TestEntity
import manager.inputs.testing.CreateTestRequest
import manager.inputs.testing.EditTestRequest
import manager.inputs.testing.IdTestRequest
import manager.security.CurrentUserIdResolver
import manager.service.app.TestingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class TestingControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var testingService: TestingService
    private lateinit var controller: TestingController
    private lateinit var objectMapper: ObjectMapper

    private val testUserId = "auth0|user123"
    private val testUserToken = "test-token-123"

    @BeforeEach
    fun setup() {
        testingService = mockk()
        controller = TestingController(testingService)
        objectMapper = ObjectMapper()

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(
                    CurrentUserIdResolver(),
                    CurrentUserTokenResolver(),
                ).build()
    }

    @Test
    fun `createOrUpdateTest should create test and return test ID`() {
        val request =
            CreateTestRequest(
                snippetId = "550e8400-e29b-41d4-a716-446655440000",
                name = "Test 1",
                input = listOf("input1", "input2"),
                output = listOf("output1"),
            )

        val testId = "test123"

        every {
            testingService.createTest(request, testUserId, testUserToken)
        } returns testId

        mockMvc
            .perform(
                post("/testing/save")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().string(testId))

        verify(exactly = 1) {
            testingService.createTest(request, testUserId, testUserToken)
        }
    }

    @Test
    fun `runTest should execute test and return result`() {
        val request = IdTestRequest(testId = "test123")

        every {
            testingService.runTest(request, testUserId, testUserToken)
        } returns "Test passed successfully"

        mockMvc
            .perform(
                post("/testing/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().string("Test passed successfully"))

        verify(exactly = 1) {
            testingService.runTest(request, testUserId, testUserToken)
        }
    }

    @Test
    fun `runTest should return failure message when test fails`() {
        val request = IdTestRequest(testId = "test123")

        every {
            testingService.runTest(request, testUserId, testUserToken)
        } returns "Test failed."

        mockMvc
            .perform(
                post("/testing/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().string("Test failed."))

        verify(exactly = 1) {
            testingService.runTest(request, testUserId, testUserToken)
        }
    }

    @Test
    fun `deleteTest should delete test and return success message`() {
        val request = IdTestRequest(testId = "test123")

        every {
            testingService.deleteTest(request, testUserId, testUserToken)
        } returns Unit

        mockMvc
            .perform(
                delete("/testing")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().string("Test deleted successfully"))

        verify(exactly = 1) {
            testingService.deleteTest(request, testUserId, testUserToken)
        }
    }

    @Test
    fun `updateTest should update test and return success message`() {
        val request =
            EditTestRequest(
                testId = "550e8400-e29b-41d4-a716-446655440000",
                snippetId = "550e8400-e29b-41d4-a716-446655440000",
                name = "Updated Test",
                input = listOf("new input"),
                output = listOf("new output"),
            )

        every {
            testingService.editTest(request, testUserId, testUserToken)
        } returns Unit

        mockMvc
            .perform(
                put("/testing/edit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().string("Test updated successfully"))

        verify(exactly = 1) {
            testingService.editTest(request, testUserId, testUserToken)
        }
    }

    @Test
    fun `getAllTestsForSnippet should return list of tests`() {
        val snippetId = "snippet123"

        val tests =
            listOf(
                TestEntity(
                    id = "test1",
                    snippetId = snippetId,
                    name = "Test 1",
                    input = listOf("input1"),
                    output = listOf("output1"),
                ),
                TestEntity(
                    id = "test2",
                    snippetId = snippetId,
                    name = "Test 2",
                    input = listOf("input2"),
                    output = listOf("output2"),
                ),
            )

        every {
            testingService.getAllTestsBySnippetId(snippetId, testUserId, testUserToken)
        } returns tests

        mockMvc
            .perform(
                get("/testing/$snippetId")
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("test1"))
            .andExpect(jsonPath("$[0].name").value("Test 1"))
            .andExpect(jsonPath("$[1].id").value("test2"))
            .andExpect(jsonPath("$[1].name").value("Test 2"))

        verify(exactly = 1) {
            testingService.getAllTestsBySnippetId(snippetId, testUserId, testUserToken)
        }
    }

    @Test
    fun `getAllTestsForSnippet should return empty list when no tests exist`() {
        val snippetId = "snippet123"

        every {
            testingService.getAllTestsBySnippetId(snippetId, testUserId, testUserToken)
        } returns emptyList()

        mockMvc
            .perform(
                get("/testing/$snippetId")
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        verify(exactly = 1) {
            testingService.getAllTestsBySnippetId(snippetId, testUserId, testUserToken)
        }
    }
}
