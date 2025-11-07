package manager.inputs.snippet

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.UUID

data class ShareSnippetRequest(
    @field:NotBlank
    @field:UUID
    val snippetId: String,
    @field:NotBlank
    val targetUserId: String,
)
