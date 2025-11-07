package manager.inputs.testing

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.UUID

data class EditTestRequest(
    @field:NotBlank
    @field:UUID
    var testId: String,
    @field:NotBlank
    @field:UUID
    var snippetId: String,
    @field:NotBlank
    var name: String,
    var input: List<String> = emptyList(),
    var output: List<String> = emptyList(),
)
