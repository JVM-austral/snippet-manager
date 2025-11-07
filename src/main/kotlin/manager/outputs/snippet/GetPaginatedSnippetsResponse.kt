package manager.outputs.snippet

import manager.outputs.PaginationResponse

data class GetPaginatedSnippetsResponse(
    val snippets: List<SnippetResponse>,
    val pagination: PaginationResponse,
)
