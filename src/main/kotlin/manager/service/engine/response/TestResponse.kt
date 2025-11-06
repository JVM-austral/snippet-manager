package manager.service.engine.response

data class TestResponse(
    val passed: Boolean,
    val failedAt: Int? = 0,
)
