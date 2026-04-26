package io.github.blakedunaway.authserver.business.model;

import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DemoAccessCode {

    private final UUID id;

    private final String accessCode;

    private final PlatformUser user;

    private final int useLimit;

    private final int useCount;

    public boolean isExhausted() {
        return useCount >= useLimit;
    }

    public DemoAccessCode recordUse() {
        return toBuilder()
                .useCount(useCount + 1)
                .build();
    }
}
