package manager.redis

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class LintStreamProducer(
    redisTemplate: RedisTemplate<String, String>,
) : RedisStreamProducer("lint-stream", redisTemplate)
