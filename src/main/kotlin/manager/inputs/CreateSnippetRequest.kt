package manager.inputs

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateSnippetRequest(
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
