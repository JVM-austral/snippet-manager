package manager.repository

import manager.entity.Languages
import manager.entity.Snippet
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class SnippetRepositoryImpl(
    private val jpaRepository: SnippetJpaRepository,
) : SnippetRepositoryInterface {
    override fun saveSnippet(
        name: String,
        code: String,
        language: String,
        description: String,
        version: String,
        userId: String,
    ): String {
        val snippet =
            Snippet(
                name = name,
                description = description,
                language = Languages.valueOf(language),
                version = version,
                code = code,
                userId = userId,
            )

        jpaRepository.save(snippet)

        return snippet.id
    }

    override fun getSnippetById(snippetId: String): Snippet? = jpaRepository.findByIdOrNull(snippetId)

    override fun getAllSnippetsByUserId(userId: String): List<Snippet> =
        jpaRepository.findAllByUserId(userId)

    override fun updateSnippet(
        snippetId: String,
        name: String?,
        code: String?,
        language: String?,
        description: String?,
        version: String?,
    ): String {
        val snippet = jpaRepository.findByIdOrNull(snippetId) ?: throw Exception("Snippet not found")

        name?.let { snippet.name = it }
        code?.let { snippet.code = it }
        language?.let { snippet.language = Languages.valueOf(it) }
        description?.let { snippet.description = it }
        version?.let { snippet.version = it }

        jpaRepository.save(snippet)

        return snippet.id
    }
}
