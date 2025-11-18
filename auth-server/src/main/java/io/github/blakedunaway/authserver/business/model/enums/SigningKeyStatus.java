package io.github.blakedunaway.authserver.business.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SigningKeyStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    DELETED("deleted");

    private final String string;
}
