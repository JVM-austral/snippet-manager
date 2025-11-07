package manager.repository.snippet.deleted

import manager.entity.CompilantState

interface DeletedSnippetRepositoryInterface {
    fun saveDeletedSnippet(
        id: String,
        name: String,
        bucketId: String,
        language: String,
        description: String,
        version: String,
        userId: String,
        creationDate: String,
        compilantState: CompilantState = CompilantState.PENDING,
    )
}
