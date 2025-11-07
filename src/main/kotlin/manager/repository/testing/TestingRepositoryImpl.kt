package manager.repository.testing

import manager.entity.TestEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class TestingRepositoryImpl(
    private val jpaRepository: TestingJpaRepository,
) : TestingRepositoryInterface {
    override fun saveTest(
        snippetId: String,
        name: String,
        input: List<String>,
        output: List<String>,
    ): String {
        val result =
            jpaRepository.save(
                TestEntity(
                    snippetId = snippetId,
                    name = name,
                    input = input,
                    output = output,
                ),
            )
        return result.id
    }

    override fun getAllTestsBySnippetId(snippetId: String): List<TestEntity> = jpaRepository.findAllBySnippetId(snippetId)

    override fun updateTest(
        testId: String,
        name: String,
        input: List<String>,
        output: List<String>,
    ): String {
        val test = jpaRepository.findByIdOrNull(testId) ?: throw Exception("Test not found")

        test.name = name
        test.input = input
        test.output = output

        jpaRepository.save(test)

        return test.id
    }

    override fun deleteTest(testId: String) {
        jpaRepository.deleteById(testId)
    }

    override fun getTestById(testId: String): TestEntity? = jpaRepository.findByIdOrNull(testId)
}
