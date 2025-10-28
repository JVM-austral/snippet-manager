package manager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["manager"])
class SnippetManagerApplication

fun main(args: Array<String>) {
    runApplication<SnippetManagerApplication>(*args)
}
