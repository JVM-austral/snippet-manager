package manager.repository.snippet.deleted

import manager.entity.CompilantState
import org.springframework.stereotype.Repository

@Repository
class DeletedSnippetRepositoryImpl (
    private val jpaRepository: DeletedSnippetJpaRepository
) : DeletedSnippetRepositoryInterface  {
    override fun saveDeletedSnippet(id: String, name: String, bucketId: String, language: String, description: String, version: String, userId: String, creationDate: String, compilantState: CompilantState) {
        jpaRepository.save(
            manager.entity.DeletedSnippet(
                id = id,
                name = name,
                description = description,
                language = manager.entity.Languages.valueOf(language),
                version = version,
                bucketId = bucketId,
                userId = userId,
                creationDate = java.time.LocalDateTime.parse(creationDate),
                state = compilantState
            )
        )
    }

}