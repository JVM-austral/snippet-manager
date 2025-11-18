package ingsis.manager.repositories

import manager.entity.FormatConfigEntity
import manager.repository.format.FormatConfig
import manager.repository.format.FormatConfigJpaRepository
import manager.repository.format.FormatConfigRepositoryImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class FormatConfigRepositoryImplTest {
    @Mock
    lateinit var jpaRepository: FormatConfigJpaRepository

    lateinit var repository: FormatConfigRepositoryImpl

    @BeforeEach
    fun setup() {
        repository = FormatConfigRepositoryImpl(jpaRepository)
    }

    @Test
    fun `saveFormatConfigForUser debe guardar y devolver userId`() {
        val userId = "user123"
        val config = FormatConfig()

        val entity = FormatConfigEntity(userId = userId, config = config)

        Mockito.`when`(jpaRepository.save(any())).thenReturn(entity)

        val result = repository.saveFormatConfigForUser(userId, config)

        assertEquals(userId, result)
        Mockito.verify(jpaRepository).save(any())
    }

    @Test
    fun `getFormatConfigForUser devuelve config cuando existe`() {
        val userId = "user123"
        val config = FormatConfig()
        val entity = FormatConfigEntity(userId = userId, config = config)

        Mockito.`when`(jpaRepository.findByUserId(userId)).thenReturn(entity)

        val result = repository.getFormatConfigForUser(userId)

        assertEquals(config, result)
    }

    @Test
    fun `getFormatConfigForUser devuelve null cuando no existe`() {
        Mockito.`when`(jpaRepository.findByUserId("notfound")).thenReturn(null)

        val result = repository.getFormatConfigForUser("notfound")

        assertNull(result)
    }

    @Test
    fun `editFormatConfigForUser edita y devuelve nueva config si existe`() {
        val userId = "user123"
        val oldConfig = FormatConfig()
        val newConfig = FormatConfig()

        val entity = FormatConfigEntity(userId = userId, config = oldConfig)

        Mockito.`when`(jpaRepository.findByUserId(userId)).thenReturn(entity)

        val result = repository.editFormatConfigForUser(userId, newConfig)

        assertEquals(newConfig, result)
        assertEquals(newConfig, entity.config)
        Mockito.verify(jpaRepository).save(entity)
    }

    @Test
    fun `editFormatConfigForUser devuelve null si el usuario no existe`() {
        Mockito.`when`(jpaRepository.findByUserId("notfound")).thenReturn(null)

        val result = repository.editFormatConfigForUser("notfound", FormatConfig())

        assertNull(result)
    }
}
