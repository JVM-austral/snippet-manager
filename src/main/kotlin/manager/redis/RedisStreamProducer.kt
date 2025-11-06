package manager.redis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate

abstract class RedisStreamProducer(
    val streamKey: String,
    val redis: RedisTemplate<String, String>,
) {
    inline fun <reified Value : Any> emit(value: Value): RecordId? {
        val objectMapper = jacksonObjectMapper()
        val json = objectMapper.writeValueAsString(value)

        val record =
            StreamRecords
                .newRecord()
                .ofMap(mapOf("value" to json))
                .withStreamKey(streamKey)

        println("Emitting to stream $streamKey: $record")

        return redis.opsForStream<String, String>().add(record)
    }

    inline fun <reified Value : Any> emitAll(values: List<Value>) {
        values.forEach { emit(it) }
    }
}
