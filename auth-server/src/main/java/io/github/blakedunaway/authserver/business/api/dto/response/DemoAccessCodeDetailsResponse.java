package io.github.blakedunaway.authserver.business.api.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@Getter
public class DemoAccessCodeDetailsResponse {

    private final String accessCode;

    private final int useLimit;

    private final int useCount;

}
