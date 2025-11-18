package ingsis.manager.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import manager.common.interceptor.CurrentUserTokenResolver
import manager.controller.ConfigController
import manager.inputs.snippet.FormatUniqueInput
import manager.redis.FormatStreamProducer
import manager.redis.LintStreamProducer
import manager.repository.format.FormatConfig
import manager.repository.lint.LintConfig
import manager.security.CurrentUserIdResolver
import manager.service.app.ConfigService
import manager.service.engine.EngineServiceInterface
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ConfigControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var configService: ConfigService
    private lateinit var formatProducer: FormatStreamProducer
    private lateinit var lintProducer: LintStreamProducer
    private lateinit var engineService: EngineServiceInterface
    private lateinit var controller: ConfigController
    private lateinit var objectMapper: ObjectMapper

    private val testUserId = "auth0|user123"

    @BeforeEach
    fun setup() {
        configService = mockk()
        formatProducer = mockk()
        lintProducer = mockk()
        engineService = mockk()
        controller = ConfigController(configService, formatProducer, lintProducer, engineService)
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
    fun `getFormatConfig should return user format config`() {
        val formatConfig =
            FormatConfig(
                enforceSpacingAroundEquals = true,
                indentInsideIf = 4,
            )

        every { configService.getFormatConfigForUser(testUserId) } returns formatConfig

        mockMvc
            .perform(
                get("/config/format")
                    .requestAttr("userId", testUserId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.enforceSpacingAroundEquals").value(true))
            .andExpect(jsonPath("$.indentInsideIf").value(4))

        verify(exactly = 1) { configService.getFormatConfigForUser(testUserId) }
    }

    @Test
    fun `getLintingConfig should return user linting config`() {
        val lintConfig =
            LintConfig(
                namingConvention = "camelCase",
                usePrintlnAnalyzer = true,
                useReadInputAnalyzer = false,
            )

        every { configService.getLintConfigForUser(testUserId) } returns lintConfig

        mockMvc
            .perform(
                get("/config/linting")
                    .requestAttr("userId", testUserId),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.namingConvention").value("camelCase"))
            .andExpect(jsonPath("$.usePrintlnAnalyzer").value(true))
            .andExpect(jsonPath("$.useReadInputAnalyzer").value(false))

        verify(exactly = 1) { configService.getLintConfigForUser(testUserId) }
    }

    @Test
    fun `formatCode should format code and return result`() {
        val request =
            FormatUniqueInput(
                snippetId = "snippet123",
                code = "print(1)",
            )

        val formatRequest =
            manager.service.engine.inputs.FormatUniqueInputForEngine(
                code = "print(1)",
                language = "PRINTSCRIPT",
                version = "V1",
                config = FormatConfig(),
            )

        val formattedCode = "print(1);\n"

        every { configService.createFormatRequest(testUserId, request) } returns formatRequest
        every { engineService.formatUnique(formatRequest) } returns formattedCode

        mockMvc
            .perform(
                post("/config/format-unique")
                    .contentType(MediaType.APPLICATION_JSON)
                    .requestAttr("userId", testUserId)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(content().string(formattedCode))

        verify(exactly = 1) { configService.createFormatRequest(testUserId, request) }
        verify(exactly = 1) { engineService.formatUnique(formatRequest) }
    }
}
