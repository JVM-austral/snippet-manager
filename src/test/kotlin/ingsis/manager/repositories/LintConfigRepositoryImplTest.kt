package ingsis.manager.repositories

import manager.entity.LintConfigEntity
import manager.repository.lint.LintConfig
import manager.repository.lint.LintConfigJpaRepository
import manager.repository.lint.LintConfigRepositoryImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class LintConfigRepositoryImplTest {
    @Mock
    lateinit var jpaRepository: LintConfigJpaRepository

    lateinit var repository: LintConfigRepositoryImpl

    @BeforeEach
    fun setup() {
        repository = LintConfigRepositoryImpl(jpaRepository)
    }

    @Test
    fun `saveLintConfigForUser debe guardar y devolver userId`() {
        val userId = "user123"
        val config = LintConfig()

        val entity = LintConfigEntity(userId = userId, config = config)

        Mockito.`when`(jpaRepository.save(any())).thenReturn(entity)

        val result = repository.saveLintConfigForUser(userId, config)

        assertEquals(userId, result)
        Mockito.verify(jpaRepository).save(any())
    }

    @Test
    fun `getLintConfigForUser devuelve config cuando existe`() {
        val userId = "user123"
        val config = LintConfig()
        val entity = LintConfigEntity(userId = userId, config = config)

        Mockito.`when`(jpaRepository.findByUserId(userId)).thenReturn(entity)

        val result = repository.getLintConfigForUser(userId)

        assertEquals(config, result)
    }

    @Test
    fun `getLintConfigForUser devuelve null cuando no existe`() {
        Mockito.`when`(jpaRepository.findByUserId("missing")).thenReturn(null)

        val result = repository.getLintConfigForUser("missing")

        assertNull(result)
    }

    @Test
    fun `editLintConfigForUser edita y devuelve nueva config si existe`() {
        val userId = "user123"
        val oldConfig = LintConfig()
        val newConfig = LintConfig()

        val entity = LintConfigEntity(userId = userId, config = oldConfig)

        Mockito.`when`(jpaRepository.findByUserId(userId)).thenReturn(entity)

        val result = repository.editLintConfigForUser(userId, newConfig)

        assertEquals(newConfig, result)
        assertEquals(newConfig, entity.config)
        Mockito.verify(jpaRepository).save(entity)
    }

    @Test
    fun `editLintConfigForUser devuelve null si el usuario no existe`() {
        Mockito.`when`(jpaRepository.findByUserId("missing")).thenReturn(null)

        val result = repository.editLintConfigForUser("missing", LintConfig())

        assertNull(result)
    }
}
