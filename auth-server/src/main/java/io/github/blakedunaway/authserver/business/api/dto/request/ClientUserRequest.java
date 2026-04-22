package io.github.blakedunaway.authserver.business.api.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class ClientUserRequest {

    private String email;

    private Boolean verified;

    private Boolean locked;

    private Boolean expired;

    private Boolean credentialsExpired;

    private Map<String, Object> userAttributes;

    private Set<String> authorities;

}
