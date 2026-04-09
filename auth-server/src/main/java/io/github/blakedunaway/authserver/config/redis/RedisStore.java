package io.github.blakedunaway.authserver.config.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RedisStore {

    private final RedisTemplate<String, Object> redis;

    public void put(String key, Object value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    public void pushToList(String key, Object value, Duration ttl) {
        try {
            redis.opsForList().rightPush(key, value);
        } catch (final RuntimeException ignored) {
            // Backward compatibility for keys previously stored as a single value.
            redis.delete(key);
            redis.opsForList().rightPush(key, value);
        }
        redis.expire(key, ttl);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) redis.opsForValue().get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T consume(String key) {
        return (T) redis.opsForValue().getAndDelete(key);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        try {
            final List<Object> values = redis.opsForList().range(key, 0, -1);
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }
            return values.stream().map(value -> (T) value).toList();
        } catch (final RuntimeException ignored) {
            // Backward compatibility for keys previously stored as a single value.
            final T single = get(key);
            return single == null ? Collections.emptyList() : List.of(single);
        }
    }

    public boolean exists(String key) {
        return redis.hasKey(key);
    }

}
