package io.github.blakedunaway.authserver.business.api.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
@Getter
public class AdminDashboardResponse {

    private final List<DemoAccessCodeDetailsResponse> demoCodes;

}
