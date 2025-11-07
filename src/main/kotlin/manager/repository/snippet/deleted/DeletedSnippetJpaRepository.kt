package manager.repository.snippet.deleted

import manager.entity.DeletedSnippet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DeletedSnippetJpaRepository : JpaRepository<DeletedSnippet, String>
