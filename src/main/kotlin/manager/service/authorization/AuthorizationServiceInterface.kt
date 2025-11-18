package manager.service.authorization

import manager.outputs.snippet.SnippetPermisesResponse

interface AuthorizationServiceInterface {
    fun grantReadPermises(
        token: String,
        userId: String,
        snippetId: String,
    ): SnippetPermisesResponse

    fun grantWritePermises(
        token: String,
        userId: String,
        snippetId: String,
    ): SnippetPermisesResponse

    fun checkWritePermises(
        token: String,
        userId: String,
        snippetId: String,
    )

    fun checkReadPermises(
        token: String,
        userId: String,
        snippetId: String,
    )

    fun getSharedSnippets(
        token: String,
        userId: String,
    ): List<String>
}
