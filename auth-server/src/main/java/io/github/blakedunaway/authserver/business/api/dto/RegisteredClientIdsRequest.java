package io.github.blakedunaway.authserver.business.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class RegisteredClientIdsRequest {

    private Set<UUID> registeredClientIds;
}
