package manager.outputs

data class PaginationResponse(
    val count: Int,
    val page: Int,
    val pageSize: Int,
)
