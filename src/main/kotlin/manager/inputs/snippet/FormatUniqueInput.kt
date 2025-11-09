package manager.inputs.snippet

import jakarta.validation.constraints.NotBlank

data class FormatUniqueInput(
    @field:NotBlank val code: String,
    @field:NotBlank val snippetId: String,
    )
