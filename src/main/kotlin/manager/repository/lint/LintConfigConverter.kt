package manager.repository.lint

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class LintConfigConverter : AttributeConverter<LintConfig, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: LintConfig?): String = objectMapper.writeValueAsString(attribute)

    override fun convertToEntityAttribute(dbData: String?): LintConfig = objectMapper.readValue(dbData, LintConfig::class.java)
}
