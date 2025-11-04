package manager.outputs.snippet

import manager.entity.Snippet

data class GetAllSnippetsResponse(
    val snippets: List<Snippet>,
)
