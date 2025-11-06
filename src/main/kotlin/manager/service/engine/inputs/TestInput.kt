package manager.service.engine.inputs

data class TestInput(
    val language: String,
    val version: String,
    val assetPath: String,
    val varInputs: List<String> = emptyList(),
    val expectedOutputs: List<String> = emptyList(),
)
