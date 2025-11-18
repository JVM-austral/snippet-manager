package manager.repository.snippet

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import manager.entity.CompilantState
import manager.entity.Languages
import manager.entity.Snippet
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.repository.findByIdOrNull
import kotlin.test.assertEquals

class SnippetRepositoryImplTest {
    private lateinit var jpaRepository: SnippetJpaRepository
    private lateinit var repository: SnippetRepositoryImpl

    @BeforeEach
    fun setup() {
        jpaRepository = mockk(relaxed = true)
        repository = SnippetRepositoryImpl(jpaRepository)
    }

    @Test
    fun `saveSnippet guarda y devuelve id`() {
        val snippet =
            Snippet(
                id = "abc",
                name = "MySnippet",
                description = "desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                bucketId = "b1",
                userId = "u1",
                author = "auth",
            )

        every { jpaRepository.save(any()) } returns snippet

        val id =
            repository.saveSnippet(
                name = "MySnippet",
                bucketId = "b1",
                language = "PRINTSCRIPT",
                description = "desc",
                version = "V1",
                userId = "u1",
                author = "auth",
            )

        verify { jpaRepository.save(any()) }
    }

    @Test
    fun `getSnippetById devuelve snippet`() {
        val snippet =
            Snippet(
                id = "10",
                name = "Code",
                description = "",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                bucketId = "b",
                userId = "u",
                author = "me",
            )

        every { jpaRepository.findByIdOrNull("10") } returns snippet

        val result = repository.getSnippetById("10")

        assertNotNull(result)
        assertEquals("10", result!!.id)
        verify { jpaRepository.findByIdOrNull("10") }
    }

    @Test
    fun `getAllSnippetsByUserId devuelve lista`() {
        val list =
            listOf(
                Snippet("1", "A", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
                Snippet("2", "B", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
            )

        every { jpaRepository.findAllByUserId("u1") } returns list

        val result = repository.getAllSnippetsByUserId("u1")

        assertEquals(2, result.size)
        verify { jpaRepository.findAllByUserId("u1") }
    }

    @Test
    fun `getPaginatedSnippetsByUserId devuelve pagina`() {
        val pageContent =
            listOf(
                Snippet("1", "A", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
            )
        val page: Page<Snippet> = PageImpl(pageContent)

        every { jpaRepository.findAllByUserId("u1", any()) } returns page

        val result = repository.getPaginatedSnippetsByUserId("u1", 0, 10)

        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `getPaginatedSnippetsByUserIdAndFilter filtra resultados`() {
        val pageContent =
            listOf(
                Snippet("1", "Hello", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
                Snippet("2", "World", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
                Snippet("3", "HELLO AGAIN", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
            )

        val page: Page<Snippet> = PageImpl(pageContent)

        every { jpaRepository.findAllByUserId("u1", any()) } returns page

        val result = repository.getPaginatedSnippetsByUserIdAndFilter("u1", 0, 10, "hello")

        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "1" })
        assertTrue(result.any { it.id == "3" })
    }

    @Test
    fun `countSnippetsByUserIdWithFilter cuenta correctamente`() {
        val all =
            listOf(
                Snippet("1", "abc", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
                Snippet("2", "abcd", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
                Snippet("3", "nope", "", Languages.PRINTSCRIPT, "V1", "b", "u2", "a"),
            )

        every { jpaRepository.findAll() } returns all

        val count = repository.countSnippetsByUserIdWithFilter("u1", "abc")

        assertEquals(2, count)
    }

    @Test
    fun `countSnippetsByUserId cuenta correctamente`() {
        val all =
            listOf(
                Snippet("1", "A", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
                Snippet("2", "B", "", Languages.PRINTSCRIPT, "V1", "b", "u1", "a"),
                Snippet("3", "C", "", Languages.PRINTSCRIPT, "V1", "b", "u2", "a"),
            )

        every { jpaRepository.findAll() } returns all

        val count = repository.countSnippetsByUserId("u1")

        assertEquals(2, count)
    }

    @Test
    fun `updateSnippet actualiza campos`() {
        val snippet =
            Snippet(
                id = "X",
                name = "Old",
                description = "old desc",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                bucketId = "b",
                userId = "u",
                author = "a",
            )

        every { jpaRepository.findByIdOrNull("X") } returns snippet
        every { jpaRepository.save(any()) } returns snippet

        val result =
            repository.updateSnippet(
                snippetId = "X",
                name = "New",
                language = "PRINTSCRIPT",
                description = "new desc",
                version = "V2",
            )

        assertEquals("X", result)
        assertEquals("New", snippet.name)
        assertEquals("new desc", snippet.description)
        assertEquals("V2", snippet.version)
        verify { jpaRepository.save(snippet) }
    }

    @Test
    fun `updateSnippet lanza error si no existe`() {
        every { jpaRepository.findByIdOrNull("404") } returns null

        val ex =
            assertThrows(Exception::class.java) {
                repository.updateSnippet("404", "n", "PRINTSCRIPT", "d", "V1")
            }

        assertTrue(ex.message!!.contains("Snippet not found"))
    }

    @Test
    fun `updateBucketIdForSnippets actualiza bucket`() {
        val snippet =
            Snippet(
                id = "X",
                name = "X",
                description = "",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                bucketId = "old",
                userId = "u",
                author = "a",
            )

        every { jpaRepository.findByIdOrNull("X") } returns snippet
        every { jpaRepository.save(any()) } returns snippet

        repository.updateBucketIdForSnippets("X", "new")

        assertEquals("new", snippet.bucketId)
        verify { jpaRepository.save(snippet) }
    }

    @Test
    fun `setSnippetState cambia estado`() {
        val snippet =
            Snippet(
                id = "X",
                name = "",
                description = "",
                language = Languages.PRINTSCRIPT,
                version = "V1",
                bucketId = "b",
                userId = "u",
                author = "a",
            )

        every { jpaRepository.findByIdOrNull("X") } returns snippet
        every { jpaRepository.save(any()) } returns snippet

        repository.setSnippetState("X", CompilantState.NON_COMPILANT)

        assertEquals(CompilantState.NON_COMPILANT, snippet.state)
        verify { jpaRepository.save(snippet) }
    }

    @Test
    fun `deleteSnippet llama al deleteById`() {
        every { jpaRepository.deleteById("88") } returns Unit

        repository.deleteSnippet("88")

        verify { jpaRepository.deleteById("88") }
    }
}
