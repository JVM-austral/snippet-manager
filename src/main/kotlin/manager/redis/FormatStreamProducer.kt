package manager.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class FormatStreamProducer(
    redisTemplate: RedisTemplate<String, String>,
) : RedisStreamProducer("format-stream", redisTemplate)
