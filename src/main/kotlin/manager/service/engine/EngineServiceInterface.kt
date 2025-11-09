package manager.service.engine

import manager.outputs.snippet.RunSnippetResponse
import manager.service.engine.inputs.AnalyzeUniqueInput
import manager.service.engine.response.TestResponse

interface EngineServiceInterface {
    fun validateSnippet(
        path: String,
        version: String,
        language: String,
    ): List<String>

    fun runSnippet(
        path: String,
        version: String,
        language: String,
        inputs: List<String>,
    ): RunSnippetResponse

    fun runTest(
        language: String,
        version: String,
        assetPath: String,
        varInputs: List<String>,
        expectedOutputs: List<String>,
    ): TestResponse

    fun formatUnique(
        input: AnalyzeUniqueInput,
    ): String
}
