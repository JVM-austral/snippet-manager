package manager.repository.format

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class FormatConfigConverter : AttributeConverter<FormatConfig, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: FormatConfig?): String = objectMapper.writeValueAsString(attribute)

    override fun convertToEntityAttribute(dbData: String?): FormatConfig = objectMapper.readValue(dbData, FormatConfig::class.java)
}
