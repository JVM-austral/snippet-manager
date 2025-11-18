package ingsis.manager.manager

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import manager.common.interceptor.CurrentUserTokenResolver
import manager.controller.ManagerController
import manager.entity.CompilantState
import manager.inputs.snippet.CreateSnippetRequest
import manager.inputs.snippet.RunSnippetRequest
import manager.inputs.snippet.ShareSnippetRequest
import manager.inputs.snippet.UpdateSnippetRequest
import manager.inputs.snippet.UpdateSnippetStateRequest
import manager.outputs.PaginationResponse
import manager.outputs.snippet.CreateSnippetResponse
import manager.outputs.snippet.GetPaginatedSnippetsResponse
import manager.outputs.snippet.RunSnippetResponse
import manager.outputs.snippet.SnippetResponse
import manager.security.CurrentUserIdResolver
import manager.service.app.ManagerService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ManagerControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var managerService: ManagerService
    private lateinit var controller: ManagerController
    private lateinit var objectMapper: ObjectMapper

    private val testUserId = "auth0|123456"
    private val testUserToken = "test-token-123"
    private val testSnippetId = "snippet-123"

    @BeforeEach
    fun setup() {
        managerService = mockk()
        controller = ManagerController(managerService)
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
    fun `createSnippet should return created snippet response`() {
        val request =
            CreateSnippetRequest(
                name = "Test Snippet",
                description = "A test snippet",
                snippet = "println('Hello World')",
                language = "PRINTSCRIPT",
                version = "1.8",
            )

        val expectedResponse =
            CreateSnippetResponse(
                id = testSnippetId,
                name = request.name,
                description = request.description,
                language = "PRINTSCRIPT",
                version = request.version,
                errorMessage = emptyList(),
                author = "testuser",
            )

        every {
            managerService.createSnippet(request, testUserId, testUserToken)
        } returns expectedResponse

        mockMvc
            .perform(
                post("/snippets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testSnippetId))
            .andExpect(jsonPath("$.name").value(request.name))
            .andExpect(jsonPath("$.language").value("PRINTSCRIPT"))

        verify(exactly = 1) {
            managerService.createSnippet(request, testUserId, testUserToken)
        }
    }

    @Test
    fun `updateSnippet should return updated snippet response`() {
        val request =
            UpdateSnippetRequest(
                snippetId = testSnippetId,
                name = "Updated Snippet",
                description = "Updated description",
                snippet = "println('Updated')",
                language = "kotlin",
                version = "1.8",
            )

        val expectedResponse =
            CreateSnippetResponse(
                id = testSnippetId,
                errorMessage = emptyList(),
            )

        every {
            managerService.updateSnippet(request, testUserId, testUserToken)
        } returns expectedResponse

        mockMvc
            .perform(
                patch("/snippets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testSnippetId))

        verify(exactly = 1) {
            managerService.updateSnippet(request, testUserId, testUserToken)
        }
    }

    @Test
    fun `getSnippet should return snippet details`() {
        val expectedResponse =
            SnippetResponse(
                id = testSnippetId,
                name = "Test Snippet",
                description = "A test snippet",
                snippet = "println('Hello')",
                language = "KOTLIN",
                version = "1.8",
                compliance = "COMPILANT",
                author = "testuser",
            )

        every {
            managerService.getSnippet(testSnippetId, testUserId, testUserToken)
        } returns expectedResponse

        mockMvc
            .perform(
                get("/snippets/$testSnippetId")
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testSnippetId))
            .andExpect(jsonPath("$.name").value("Test Snippet"))
            .andExpect(jsonPath("$.language").value("KOTLIN"))

        verify(exactly = 1) {
            managerService.getSnippet(testSnippetId, testUserId, testUserToken)
        }
    }

    @Test
    fun `getAllSnippets should return paginated snippets`() {
        val snippets =
            listOf(
                SnippetResponse(
                    id = "1",
                    name = "Snippet 1",
                    description = "First snippet",
                    snippet = "code1",
                    language = "KOTLIN",
                    version = "1.8",
                    compliance = "COMPILANT",
                    author = "user1",
                ),
                SnippetResponse(
                    id = "2",
                    name = "Snippet 2",
                    description = "Second snippet",
                    snippet = "code2",
                    language = "JAVA",
                    version = "17",
                    compliance = "NON_COMPILANT",
                    author = "user2",
                ),
            )

        val expectedResponse =
            GetPaginatedSnippetsResponse(
                snippets = snippets,
                pagination =
                    PaginationResponse(
                        page = 0,
                        pageSize = 10,
                        count = 2,
                    ),
            )

        every {
            managerService.getAllSnippets(testUserId, testUserToken, 0, 10, null)
        } returns expectedResponse

        mockMvc
            .perform(
                get("/snippets")
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .param("page", "0")
                    .param("page_size", "10"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.snippets.length()").value(2))
            .andExpect(jsonPath("$.pagination.count").value(2))

        verify(exactly = 1) {
            managerService.getAllSnippets(testUserId, testUserToken, 0, 10, null)
        }
    }

    @Test
    fun `getAllSnippets with filter should return filtered snippets`() {
        val filter = "kotlin"
        val expectedResponse =
            GetPaginatedSnippetsResponse(
                snippets = emptyList(),
                pagination = PaginationResponse(page = 0, pageSize = 10, count = 0),
            )

        every {
            managerService.getAllSnippets(testUserId, testUserToken, 0, 10, filter)
        } returns expectedResponse

        mockMvc
            .perform(
                get("/snippets")
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .param("filter", filter),
            ).andExpect(status().isOk)

        verify(exactly = 1) {
            managerService.getAllSnippets(testUserId, testUserToken, 0, 10, filter)
        }
    }

    @Test
    fun `shareSnippet should return success message`() {
        val request =
            ShareSnippetRequest(
                snippetId = "3f8b2c16-4e41-4bd1-9f7a-2c6d8f2e915a",
                targetUserId = "auth0|target-user",
            )

        every {
            managerService.shareSnippet(request, testUserId, testUserToken)
        } returns Unit

        mockMvc
            .perform(
                post("/snippets/share")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().string("Snippet shared successfully"))

        verify(exactly = 1) {
            managerService.shareSnippet(request, testUserId, testUserToken)
        }
    }

    @Test
    fun `runSnippet should return execution output`() {
        val request =
            RunSnippetRequest(
                snippetId = testSnippetId,
                varInputs = listOf("input1", "input2"),
            )

        val expectedResponse =
            RunSnippetResponse(
                output = listOf("Hello World\n"),
                errors = emptyList(),
            )

        every {
            managerService.runSnippet(request, testUserId, testUserToken)
        } returns expectedResponse

        mockMvc
            .perform(
                post("/snippets/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.output").value("Hello World\n"))

        verify(exactly = 1) {
            managerService.runSnippet(request, testUserId, testUserToken)
        }
    }

    @Test
    fun `deleteSnippet should return success message`() {
        every {
            managerService.deleteSnippet(testSnippetId, testUserId, testUserToken)
        } returns Unit

        mockMvc
            .perform(
                delete("/snippets/$testSnippetId")
                    .requestAttr("userId", testUserId)
                    .requestAttr("token", testUserToken),
            ).andExpect(status().isOk)
            .andExpect(content().string("Snippet deleted successfully"))

        verify(exactly = 1) {
            managerService.deleteSnippet(testSnippetId, testUserId, testUserToken)
        }
    }

    @Test
    fun `changeSnippetState should return success message`() {
        val request =
            UpdateSnippetStateRequest(
                snippetId = testSnippetId,
                state = CompilantState.COMPILANT,
            )

        every {
            managerService.changeSnippetState(request)
        } returns Unit

        mockMvc
            .perform(
                put("/snippets/compiling-state")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().string("Snippet state updated successfully"))

        verify(exactly = 1) {
            managerService.changeSnippetState(request)
        }
    }
}
