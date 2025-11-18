package ingsis.manager.repositories

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import manager.entity.TestEntity
import manager.repository.testing.TestingJpaRepository
import manager.repository.testing.TestingRepositoryImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.springframework.data.repository.findByIdOrNull

class TestingRepositoryImplTest {
    private lateinit var jpaRepository: TestingJpaRepository
    private lateinit var repository: TestingRepositoryImpl

    @BeforeEach
    fun setup() {
        jpaRepository = mockk(relaxed = true)
        repository = TestingRepositoryImpl(jpaRepository)
    }

    @Test
    fun `saveTest guarda y retorna id`() {
        val entity =
            TestEntity(
                id = "123",
                snippetId = "s1",
                name = "test1",
                input = listOf("1"),
                output = listOf("2"),
            )

        every { jpaRepository.save(any()) } returns entity

        val result =
            repository.saveTest(
                snippetId = "s1",
                name = "test1",
                input = listOf("1"),
                output = listOf("2"),
            )

        assertEquals("123", result)
        verify { jpaRepository.save(any()) }
    }

    @Test
    fun `getAllTestsBySnippetId devuelve lista`() {
        val list =
            listOf(
                TestEntity("1", "s1", "t1", listOf("1"), listOf("2")),
                TestEntity("2", "s1", "t2", listOf("3"), listOf("4")),
            )

        every { jpaRepository.findAllBySnippetId("s1") } returns list

        val result = repository.getAllTestsBySnippetId("s1")

        assertEquals(2, result.size)
        assertEquals("t1", result[0].name)
        verify { jpaRepository.findAllBySnippetId("s1") }
    }

    @Test
    fun `updateTest modifica y retorna id`() {
        val existing =
            TestEntity(
                id = "10",
                snippetId = "s1",
                name = "old",
                input = listOf("x"),
                output = listOf("y"),
            )

        every { jpaRepository.findByIdOrNull("10") } returns existing
        every { jpaRepository.save(any()) } returns existing

        val result =
            repository.updateTest(
                testId = "10",
                name = "new",
                input = listOf("1"),
                output = listOf("2"),
            )

        assertEquals("10", result)
        assertEquals("new", existing.name)
        assertEquals(listOf("1"), existing.input)
        assertEquals(listOf("2"), existing.output)

        verify { jpaRepository.findByIdOrNull("10") }
        verify { jpaRepository.save(existing) }
    }

    @Test
    fun `updateTest lanza excepción si no existe`() {
        every { jpaRepository.findByIdOrNull("999") } returns null

        val ex =
            assertThrows(Exception::class.java) {
                repository.updateTest(
                    "999",
                    "test",
                    listOf("a"),
                    listOf("b"),
                )
            }

        assertTrue(ex.message!!.contains("Test not found"))
    }

    @Test
    fun `deleteTest llama al deleteById`() {
        every { jpaRepository.deleteById("55") } returns Unit

        repository.deleteTest("55")

        verify { jpaRepository.deleteById("55") }
    }

    @Test
    fun `getTestById devuelve entidad`() {
        val entity = TestEntity("200", "s2", "name", listOf(), listOf())

        every { jpaRepository.findByIdOrNull("200") } returns entity

        val result = repository.getTestById("200")

        assertNotNull(result)
        assertEquals("200", result!!.id)

        verify { jpaRepository.findByIdOrNull("200") }
    }

    @Test
    fun `getTestById devuelve null si no existe`() {
        every { jpaRepository.findByIdOrNull("404") } returns null

        val result = repository.getTestById("404")

        assertNull(result)

        verify { jpaRepository.findByIdOrNull("404") }
    }
}
