package manager.repository

import manager.entity.Snippet

interface SnippetRepositoryInterface {
    fun saveSnippet(
        name: String,
        bucketId: String,
        language: String,
        description: String,
        version: String,
        userId: String,
    ): String

    fun getSnippetById(snippetId: String): Snippet?

    fun getAllSnippetsByUserId(userId: String): List<Snippet>

    fun updateSnippet(
        snippetId: String,
        name: String?,
        bucketId: String?,
        language: String?,
        description: String?,
        version: String?,
    ): String
}
