package io.github.blakedunaway.authserver.business.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreatedRegisteredClient {

    private final RegisteredClientModel registeredClient;

    private final String rawClientSecret;

    public static CreatedRegisteredClient create(final RegisteredClientModel registeredClient,
                                                 final String rawClientSecret) {
        return new CreatedRegisteredClient(registeredClient, rawClientSecret);
    }

}
