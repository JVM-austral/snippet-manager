package ingsis.manager.config

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import manager.entity.Languages
import manager.entity.Snippet
import manager.inputs.config.AnalyzeCodeRequest
import manager.inputs.config.FormatCodeRequest
import manager.inputs.snippet.FormatUniqueInput
import manager.repository.format.FormatConfig
import manager.repository.format.FormatConfigRepositoryInterface
import manager.repository.lint.LintConfig
import manager.repository.lint.LintConfigRepositoryInterface
import manager.repository.snippet.SnippetRepositoryInterface
import manager.service.app.ConfigService
import manager.service.engine.EngineServiceInterface
import manager.service.engine.response.LintErrorResponse
import manager.service.engine.response.LintResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConfigServiceTest {
    @MockK
    lateinit var snippetRepository: SnippetRepositoryInterface

    @MockK
    lateinit var lintConfigRepository: LintConfigRepositoryInterface

    @MockK
    lateinit var formatConfigRepository: FormatConfigRepositoryInterface

    @MockK
    lateinit var engineService: EngineServiceInterface

    lateinit var configService: ConfigService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        configService =
            ConfigService(
                snippetRepository = snippetRepository,
                lintConfigRepository = lintConfigRepository,
                formatConfigRepository = formatConfigRepository,
                engineService = engineService,
            )
    }

    @Test
    fun `saveLintingConfig should create new config when none exists`() {
        val userId = "auth0|user123"
        val request =
            AnalyzeCodeRequest(
                namingConvention = "camelCase",
                usePrintlnAnalyzer = true,
                useReadInputAnalyzer = false,
            )

        every { lintConfigRepository.getLintConfigForUser(userId) } returns null
        every { lintConfigRepository.saveLintConfigForUser(userId, any()) } returns "Unit"

        configService.saveLintingConfig(userId, request)

        verify(exactly = 1) { lintConfigRepository.getLintConfigForUser(userId) }
        verify(exactly = 1) { lintConfigRepository.saveLintConfigForUser(userId, any()) }
        verify(exactly = 0) { lintConfigRepository.editLintConfigForUser(any(), any()) }
    }

    @Test
    fun `saveLintingConfig should update existing config`() {
        val userId = "auth0|user123"
        val request =
            AnalyzeCodeRequest(
                namingConvention = "snake_case",
                usePrintlnAnalyzer = false,
                useReadInputAnalyzer = true,
            )

        val existingConfig =
            LintConfig(
                namingConvention = "camelCase",
                usePrintlnAnalyzer = true,
                useReadInputAnalyzer = false,
            )

        every { lintConfigRepository.getLintConfigForUser(userId) } returns existingConfig
        every { lintConfigRepository.editLintConfigForUser(userId, any()) } returns LintConfig()

        configService.saveLintingConfig(userId, request)

        verify(exactly = 1) { lintConfigRepository.getLintConfigForUser(userId) }
        verify(exactly = 1) { lintConfigRepository.editLintConfigForUser(userId, any()) }
        verify(exactly = 0) { lintConfigRepository.saveLintConfigForUser(any(), any()) }
    }

    @Test
    fun `saveLintingConfig should fail with invalid naming convention`() {
        val userId = "auth0|user123"
        val request =
            AnalyzeCodeRequest(
                namingConvention = "invalid_convention",
                usePrintlnAnalyzer = true,
                useReadInputAnalyzer = false,
            )

        val exception =
            assertThrows<ResponseStatusException> {
                configService.saveLintingConfig(userId, request)
            }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("Invalid naming convention: invalid_convention", exception.reason)
    }

    @Test
    fun `saveFormatConfig should create new config when none exists`() {
        val userId = "auth0|user123"
        val request =
            FormatCodeRequest(
                enforceNoSpacingAroundEquals = false,
                enforceSpacingAroundEquals = true,
                enforceSpacingAfterColonInDeclaration = true,
                enforceSpacingBeforeColonInDeclaration = false,
                mandatorySingleSpaceSeparation = false,
                mandatorySpaceSurroundingOperations = false,
                mandatoryLineBreakAfterStatement = true,
                lineBreakAfterPrintLn = 1,
                ifBraceSameLine = true,
                ifBraceBelowLine = false,
                indentInsideIf = 4,
            )

        every { formatConfigRepository.getFormatConfigForUser(userId) } returns null
        every { formatConfigRepository.saveFormatConfigForUser(userId, any()) } returns "hola"

        configService.saveFormatConfig(userId, request)

        verify(exactly = 1) { formatConfigRepository.getFormatConfigForUser(userId) }
        verify(exactly = 1) { formatConfigRepository.saveFormatConfigForUser(userId, any()) }
        verify(exactly = 0) { formatConfigRepository.editFormatConfigForUser(any(), any()) }
    }

    @Test
    fun `saveFormatConfig should update existing config`() {
        val userId = "auth0|user123"
        val request =
            FormatCodeRequest(
                enforceNoSpacingAroundEquals = false,
                enforceSpacingAroundEquals = true,
                enforceSpacingAfterColonInDeclaration = true,
                enforceSpacingBeforeColonInDeclaration = false,
                mandatorySingleSpaceSeparation = false,
                mandatorySpaceSurroundingOperations = false,
                mandatoryLineBreakAfterStatement = true,
                lineBreakAfterPrintLn = 1,
                ifBraceSameLine = true,
                ifBraceBelowLine = false,
                indentInsideIf = 4,
            )

        val existingConfig = FormatConfig()

        every { formatConfigRepository.getFormatConfigForUser(userId) } returns existingConfig
        every { formatConfigRepository.editFormatConfigForUser(userId, any()) } returns FormatConfig()

        configService.saveFormatConfig(userId, request)

        verify(exactly = 1) { formatConfigRepository.getFormatConfigForUser(userId) }
        verify(exactly = 1) { formatConfigRepository.editFormatConfigForUser(userId, any()) }
        verify(exactly = 0) { formatConfigRepository.saveFormatConfigForUser(any(), any()) }
    }

    @Test
    fun `saveFormatConfig should fail with conflicting spacing rules`() {
        val userId = "auth0|user123"
        val request =
            FormatCodeRequest(
                enforceNoSpacingAroundEquals = true,
                enforceSpacingAroundEquals = true,
                enforceSpacingAfterColonInDeclaration = true,
                enforceSpacingBeforeColonInDeclaration = false,
                mandatorySingleSpaceSeparation = false,
                mandatorySpaceSurroundingOperations = false,
                mandatoryLineBreakAfterStatement = true,
                lineBreakAfterPrintLn = 1,
                ifBraceSameLine = true,
                ifBraceBelowLine = false,
                indentInsideIf = 4,
            )

        val exception =
            assertThrows<ResponseStatusException> {
                configService.saveFormatConfig(userId, request)
            }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("Conflicting rules for spacing around equals", exception.reason)
    }

    @Test
    fun `saveFormatConfig should fail with conflicting operation rules`() {
        val userId = "auth0|user123"
        val request =
            FormatCodeRequest(
                enforceNoSpacingAroundEquals = false,
                enforceSpacingAroundEquals = false,
                enforceSpacingAfterColonInDeclaration = true,
                enforceSpacingBeforeColonInDeclaration = false,
                mandatorySingleSpaceSeparation = true,
                mandatorySpaceSurroundingOperations = true,
                mandatoryLineBreakAfterStatement = true,
                lineBreakAfterPrintLn = 1,
                ifBraceSameLine = true,
                ifBraceBelowLine = false,
                indentInsideIf = 4,
            )

        val exception =
            assertThrows<ResponseStatusException> {
                configService.saveFormatConfig(userId, request)
            }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertEquals("Conflicting rules for spacing around operations", exception.reason)
    }

    @Test
    fun `getListOfFormatRequests should return list of requests for user snippets`() {
        val userId = "auth0|user123"
        val snippets =
            listOf(
                Snippet(
                    id = "1",
                    name = "Snippet1",
                    description = "desc",
                    language = Languages.PRINTSCRIPT,
                    version = "V1",
                    userId = userId,
                    bucketId = "/v1/asset/user123/1",
                    author = "User",
                ),
                Snippet(
                    id = "2",
                    name = "Snippet2",
                    description = "desc",
                    language = Languages.PRINTSCRIPT,
                    version = "V2",
                    userId = userId,
                    bucketId = "/v1/asset/user123/2",
                    author = "User",
                ),
            )

        val formatConfig = FormatConfig()

        every { snippetRepository.getAllSnippetsByUserId(userId) } returns snippets
        every { formatConfigRepository.getFormatConfigForUser(userId) } returns formatConfig

        val result = configService.getListOfFormatRequests(userId)

        assertEquals(2, result.size)
        assertEquals("PRINTSCRIPT", result[0].language)
        assertEquals("V1", result[0].version)
        assertEquals("/v1/asset/user123/1", result[0].assetPath)

        verify(exactly = 1) { snippetRepository.getAllSnippetsByUserId(userId) }
        verify(exactly = 1) { formatConfigRepository.getFormatConfigForUser(userId) }
    }

    @Test
    fun `getListOfLintRequests should return list of requests for user snippets`() {
        val userId = "auth0|user123"
        val snippets =
            listOf(
                Snippet(
                    id = "1",
                    name = "Snippet1",
                    description = "desc",
                    language = Languages.PRINTSCRIPT,
                    version = "V1",
                    userId = userId,
                    bucketId = "/v1/asset/user123/1",
                    author = "User",
                ),
            )

        val lintConfig = LintConfig()

        every { snippetRepository.getAllSnippetsByUserId(userId) } returns snippets
        every { lintConfigRepository.getLintConfigForUser(userId) } returns lintConfig

        val result = configService.getListOfLintRequests(userId)

        assertEquals(1, result.size)
        assertEquals("PRINTSCRIPT", result[0].language)
        assertEquals("V1", result[0].version)
        assertEquals("/v1/asset/user123/1", result[0].assetPath)
        assertEquals("1", result[0].snippetId)

        verify(exactly = 1) { snippetRepository.getAllSnippetsByUserId(userId) }
        verify(exactly = 1) { lintConfigRepository.getLintConfigForUser(userId) }
    }

    @Test
    fun `getFormatConfigForUser should return existing config`() {
        val userId = "auth0|user123"
        val config = FormatConfig(enforceSpacingAroundEquals = true)

        every { formatConfigRepository.getFormatConfigForUser(userId) } returns config

        val result = configService.getFormatConfigForUser(userId)

        assertEquals(true, result.enforceSpacingAroundEquals)
        verify(exactly = 1) { formatConfigRepository.getFormatConfigForUser(userId) }
    }

    @Test
    fun `getFormatConfigForUser should return default config when none exists`() {
        val userId = "auth0|user123"

        every { formatConfigRepository.getFormatConfigForUser(userId) } returns null

        val result = configService.getFormatConfigForUser(userId)

        assertNotNull(result)
        verify(exactly = 1) { formatConfigRepository.getFormatConfigForUser(userId) }
    }

    @Test
    fun `getLintConfigForUser should return existing config`() {
        val userId = "auth0|user123"
        val config = LintConfig(namingConvention = "camelCase")

        every { lintConfigRepository.getLintConfigForUser(userId) } returns config

        val result = configService.getLintConfigForUser(userId)

        assertEquals("camelCase", result.namingConvention)
        verify(exactly = 1) { lintConfigRepository.getLintConfigForUser(userId) }
    }

    @Test
    fun `getLintConfigForUser should return default config when none exists`() {
        val userId = "auth0|user123"

        every { lintConfigRepository.getLintConfigForUser(userId) } returns null

        val result = configService.getLintConfigForUser(userId)

        assertNotNull(result)
        verify(exactly = 1) { lintConfigRepository.getLintConfigForUser(userId) }
    }

    @Test
    fun `createFormatRequest should create request successfully`() {
        val userId = "auth0|user123"
        val request =
            FormatUniqueInput(
                snippetId = "snippet123",
                code = "print(1)",
            )

        val snippet =
            Snippet(
                id = "snippet123",
                name = "Test",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                userId = userId,
                bucketId = "/v1/asset/user123/snippet123",
                author = "User",
            )

        val formatConfig = FormatConfig()

        every { formatConfigRepository.getFormatConfigForUser(userId) } returns formatConfig
        every { snippetRepository.getSnippetById("snippet123") } returns snippet

        val result = configService.createFormatRequest(userId, request)

        assertEquals("print(1)", result.code)
        assertEquals("PRINTSCRIPT", result.language)
        assertEquals("V1", result.version)
        assertNotNull(result.config)

        verify(exactly = 1) { snippetRepository.getSnippetById("snippet123") }
        verify(exactly = 1) { formatConfigRepository.getFormatConfigForUser(userId) }
    }

    @Test
    fun `createFormatRequest should fail when snippet not found`() {
        val userId = "auth0|user123"
        val request =
            FormatUniqueInput(
                snippetId = "non-existent",
                code = "print(1)",
            )

        val formatConfig = FormatConfig()

        every { formatConfigRepository.getFormatConfigForUser(userId) } returns formatConfig
        every { snippetRepository.getSnippetById("non-existent") } returns null

        val exception =
            assertThrows<ResponseStatusException> {
                configService.createFormatRequest(userId, request)
            }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals("Snippet not found with id: $request.id", exception.reason)
    }

    @Test
    fun `lintUniqueWithPath should return lint errors`() {
        val userId = "auth0|user123"
        val path = "/v1/asset/user123/snippet1"
        val language = "PRINTSCRIPT"
        val version = "V1"

        val lintConfig = LintConfig()
        val lintResponse = LintResponse(lintErrors = listOf(LintErrorResponse("hola", 0, 0)))

        every { lintConfigRepository.getLintConfigForUser(userId) } returns lintConfig
        every {
            engineService.lintUnique(lintConfig, path, language, version)
        } returns lintResponse

        val result = configService.lintUniqueWithPath(userId, path, language, version)

        assertEquals(1, result.lintErrors.size)
        assertEquals("hola", result.lintErrors[0].message)

        verify(exactly = 1) { lintConfigRepository.getLintConfigForUser(userId) }
        verify(exactly = 1) { engineService.lintUnique(lintConfig, path, language, version) }
    }
}
