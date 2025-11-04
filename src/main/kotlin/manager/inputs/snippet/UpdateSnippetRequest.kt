package manager.inputs.snippet

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateSnippetRequest(
    @field:NotBlank
    val snippetId: String,
    @field:NotBlank
    @field:Size(min = 3, max = 50)
    val name: String,
    @field:NotBlank
    @field:Size(max = 255)
    val description: String,
    @field:NotBlank
    val snippet: String,
    @field:NotBlank
    val language: String,
    @field:NotBlank
    val version: String,
)
