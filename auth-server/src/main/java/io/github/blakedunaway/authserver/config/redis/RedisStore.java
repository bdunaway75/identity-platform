package io.github.blakedunaway.authserver.config.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class RedisStore {

    private final RedisTemplate<String, Object> redis;

    public void put(String key, Object value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redis.opsForValue().get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T consume(String key) {
        return (T) redis.opsForValue().getAndDelete(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

}
