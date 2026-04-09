package io.github.blakedunaway.authserver.business.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ClientUserActivityResponse {

    private final List<ClientUserActivity> logins;

    private final List<ClientUserActivity> signups;

}
