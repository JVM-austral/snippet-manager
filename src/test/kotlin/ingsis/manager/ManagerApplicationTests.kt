package ingsis.manager

import manager.SnippetManagerApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [SnippetManagerApplication::class])
@ActiveProfiles("test")
class ManagerApplicationTests {
    @Test
    fun contextLoads() {
    }
}
