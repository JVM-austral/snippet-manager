package manager.repository.testing

import manager.entity.TestEntity

interface TestingRepositoryInterface {
    fun saveTest(
        snippetId: String,
        name: String,
        input: List<String>,
        output: List<String>,
    ): String

    fun getAllTestsBySnippetId(snippetId: String): List<TestEntity>

    fun updateTest(
        testId: String,
        name: String,
        input: List<String>,
        output: List<String>,
    ): String

    fun deleteTest(testId: String)

    fun getTestById(testId: String): TestEntity?
}
