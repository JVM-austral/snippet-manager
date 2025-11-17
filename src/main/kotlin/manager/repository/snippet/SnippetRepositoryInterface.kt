package manager.repository.snippet

import manager.entity.CompilantState
import manager.entity.Snippet

interface SnippetRepositoryInterface {
    fun saveSnippet(
        name: String,
        bucketId: String,
        language: String,
        description: String,
        version: String,
        userId: String,
        author: String,
    ): String

    fun getSnippetById(snippetId: String): Snippet?

    fun getAllSnippetsByUserId(userId: String): List<Snippet>

    fun getPaginatedSnippetsByUserId(
        userId: String,
        page: Int,
        pageSize: Int,
    ): List<Snippet>

    fun getPaginatedSnippetsByUserIdAndFilter(
        userId: String,
        page: Int,
        pageSize: Int,
        filter: String,
    ): List<Snippet>

    fun countSnippetsByUserIdWithFilter(
        userId: String,
        filter: String,
    ): Int

    fun countSnippetsByUserId(userId: String): Int

    fun updateSnippet(
        snippetId: String,
        name: String?,
        language: String?,
        description: String?,
        version: String?,
    ): String

    fun updateBucketIdForSnippets(
        snippetId: String,
        newBucketId: String,
    )

    fun setSnippetState(
        snippetId: String,
        state: CompilantState,
    )

    fun deleteSnippet(
        snippetId: String,
    )
}
