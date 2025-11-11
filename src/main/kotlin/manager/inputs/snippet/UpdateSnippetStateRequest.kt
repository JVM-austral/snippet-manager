package manager.inputs.snippet

import jakarta.validation.constraints.NotNull
import manager.entity.CompilantState
import org.hibernate.validator.constraints.UUID

data class UpdateSnippetStateRequest(
    @param:NotNull
    val state: CompilantState,
    @param:NotNull
    @param:UUID
    val snippetId: String,
)
