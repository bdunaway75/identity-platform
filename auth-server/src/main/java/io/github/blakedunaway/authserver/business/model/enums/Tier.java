package io.github.blakedunaway.authserver.business.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Tier {
    AUTHORIZATION_CODE("Free"),
    CLIENT_CREDENTIALS("Paid");

    private final String name;

    public static Tier findByName(final String name) {
        for (Tier tier : values()) {
            if (tier.name.equalsIgnoreCase(name)) {
                return tier;
            }
        }
        throw new IllegalArgumentException("No such tier " + name);
    }

}
