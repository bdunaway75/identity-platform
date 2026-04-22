package io.github.blakedunaway.authserver.business.api.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Builder
@Jacksonized
@Getter
public class ClientUserActivity {

    private final String email;

    private final LocalDateTime activityTs;

}
