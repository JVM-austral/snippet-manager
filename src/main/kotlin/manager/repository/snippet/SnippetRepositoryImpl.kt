package manager.repository.snippet

import manager.entity.CompilantState
import manager.entity.Languages
import manager.entity.Snippet
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class SnippetRepositoryImpl(
    private val jpaRepository: SnippetJpaRepository,
) : SnippetRepositoryInterface {
    override fun saveSnippet(
        name: String,
        bucketId: String,
        language: String,
        description: String,
        version: String,
        userId: String,
        author: String,
    ): String {
        val snippet =
            Snippet(
                name = name,
                description = description,
                language = Languages.valueOf(language),
                version = version,
                bucketId = bucketId,
                userId = userId,
                author = author,
            )

        jpaRepository.save(snippet)

        return snippet.id
    }

    override fun getSnippetById(snippetId: String): Snippet? = jpaRepository.findByIdOrNull(snippetId)

    override fun getAllSnippetsByUserId(userId: String): List<Snippet> = jpaRepository.findAllByUserId(userId)

    override fun getPaginatedSnippetsByUserId(
        userId: String,
        page: Int,
        pageSize: Int,
    ): List<Snippet> {
        val pageable = PageRequest.of(page, pageSize)
        return jpaRepository.findAllByUserId(userId, pageable).content
    }

    override fun getPaginatedSnippetsByUserIdAndFilter(
        userId: String,
        page: Int,
        pageSize: Int,
        filter: String,
    ): List<Snippet> {
        val pageable = PageRequest.of(page, pageSize)
        return jpaRepository.searchByUserIdAndName(userId, filter, pageable)
    }

    override fun countSnippetsByUserIdWithFilter(
        userId: String,
        filter: String,
    ): Int = jpaRepository.findAll().count { it.userId == userId && it.name.contains(filter, ignoreCase = true) }

    override fun countSnippetsByUserId(userId: String): Int = jpaRepository.findAll().count { it.userId == userId }

    override fun updateSnippet(
        snippetId: String,
        name: String?,
        language: String?,
        description: String?,
        version: String?,
    ): String {
        val snippet = jpaRepository.findByIdOrNull(snippetId) ?: throw Exception("Snippet not found")

        name?.let { snippet.name = it }
        language?.let { snippet.language = Languages.valueOf(it) }
        description?.let { snippet.description = it }
        version?.let { snippet.version = it }

        jpaRepository.save(snippet)

        return snippet.id
    }

    override fun updateBucketIdForSnippets(
        snippetId: String,
        newBucketId: String,
    ) {
        val snippet = jpaRepository.findByIdOrNull(snippetId) ?: throw Exception("Snippet not found")
        snippet.bucketId = newBucketId
        jpaRepository.save(snippet)
    }

    override fun setSnippetState(
        snippetId: String,
        state: CompilantState,
    ) {
        val snippet = jpaRepository.findByIdOrNull(snippetId) ?: throw Exception("Snippet not found")
        snippet.state = state
        jpaRepository.save(snippet)
    }

    override fun deleteSnippet(snippetId: String) {
        jpaRepository.deleteById(snippetId)
    }
}
