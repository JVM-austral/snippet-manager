package manager.service

import manager.repository.lint.LintConfigRepositoryInterface
import manager.repository.snippet.SnippetRepositoryInterface
import org.springframework.stereotype.Service

@Service
class ConfigService(
    private val snippetRepository: SnippetRepositoryInterface,
    private val lintConfigRepository: LintConfigRepositoryInterface,
)
