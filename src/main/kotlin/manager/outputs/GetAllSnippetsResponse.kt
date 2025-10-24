package manager.outputs

import manager.entity.Snippet

data class GetAllSnippetsResponse(
    val snippets: List<Snippet>,
)
