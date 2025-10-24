package manager.inputs

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.UUID

data class UpdateSnippetRequest(
    @field:NotBlank
    @field:UUID
    val snippetId: String,
    @field:Size(min = 3, max = 50)
    val name: String? = null,
    @field:Size(max = 255)
    val description: String? = null,
    val snippet: String? = null,
    val language: String? = null,
    val version: String? = null,
)
