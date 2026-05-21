package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.config.redis.RedisStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class EventStreamService {

    private static final long EMITTER_TIMEOUT_MILLIS = Duration.ofMinutes(6).toMillis();

    private final RedisStore redisStore;

    private final Map<String, List<SseEmitter>> emittersByChannelId = new ConcurrentHashMap<>();

    public String createSessionId() {
        return UUID.randomUUID().toString();
    }

    public SseEmitter subscribe(final String channelId,
                                final String completedEventName,
                                final StreamEvent initialEvent) {
        Assert.hasText(channelId, "channelId must not be empty");
        Assert.hasText(completedEventName, "completedEventName must not be empty");

        final SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        // Send any already-completed event before registering this emitter so late subscribers do not miss it.
        final Object existingData = redisStore.get(channelId);
        final StreamEvent existingEvent = existingData == null ? null : new StreamEvent(completedEventName, existingData, true);
        if (existingEvent != null) {
            sendEvent(emitter, existingEvent, channelId);
            return emitter;
        }

        emittersByChannelId.computeIfAbsent(channelId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(channelId, emitter));
        emitter.onTimeout(() -> removeEmitter(channelId, emitter));
        emitter.onError(ignored -> removeEmitter(channelId, emitter));

        if (initialEvent != null) {
            sendEvent(emitter, initialEvent, channelId);
        }

        return emitter;
    }

    public void sendToSubscribers(final String channelId, final StreamEvent event) {
        Assert.hasText(channelId, "channelId must not be empty");
        Assert.notNull(event, "event must not be null");

        final List<SseEmitter> emitters = event.completeAfterSend()
                                          ? emittersByChannelId.remove(channelId)
                                          : emittersByChannelId.get(channelId);
        if (emitters == null) {
            return;
        }

        emitters.forEach(emitter -> sendEvent(emitter, event, channelId));
    }

    public void storeAndSend(final String channelId,
                             final Object data,
                             final Duration ttl,
                             final String completedEventName) {
        Assert.hasText(channelId, "channelId must not be empty");
        Assert.notNull(ttl, "ttl must not be null");
        Assert.hasText(completedEventName, "completedEventName must not be empty");

        // Persist the completed payload first so subscribers that connect after the event still receive it on subscribe.
        redisStore.put(channelId, data, ttl);
        // Then notify any subscribers that are already connected right now.
        sendToSubscribers(channelId, new StreamEvent(completedEventName, data, true));
    }

    private void sendEvent(final SseEmitter emitter,
                           final StreamEvent event,
                           final String channelId) {
        try {
            emitter.send(SseEmitter.event().name(event.name()).data(event.data()));
            if (event.completeAfterSend()) {
                emitter.complete();
            }
        } catch (final IOException ex) {
            removeEmitter(channelId, emitter);
            emitter.completeWithError(ex);
        }
    }

    private void removeEmitter(final String channelId, final SseEmitter emitter) {
        final List<SseEmitter> emitters = emittersByChannelId.get(channelId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByChannelId.remove(channelId, emitters);
        }
    }

    public record StreamEvent(String name, Object data, boolean completeAfterSend) {

        public StreamEvent {
            Assert.hasText(name, "name must not be empty");
        }
    }

}
