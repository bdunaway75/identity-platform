package io.github.blakedunaway.authserver.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuthorityUtility {

    private AuthorityUtility() {
    }

    public static Set<String> normalizeAuthorities(final Set<String> values) {
        if (values == null) {
            return Collections.emptySet();
        }

        return values.stream()
                     .filter(value -> value != null && !value.isBlank())
                     .map(String::trim)
                     .map(String::toUpperCase)
                     .filter(value -> !value.startsWith("ROLE_"))
                     .collect(Collectors.toCollection(HashSet::new));
    }

    public static Set<String> normalizeRoles(final Set<String> values) {
        if (values == null) {
            return Collections.emptySet();
        }

        return values.stream()
                     .filter(value -> value != null && !value.isBlank())
                     .map(String::trim)
                     .map(String::toUpperCase)
                     .map(value -> value.startsWith("ROLE_") ? value : "ROLE_" + value)
                     .collect(Collectors.toCollection(HashSet::new));
    }

    public static Set<String> normalizeAuthorityAndRoleNames(final Set<String> values) {
        if (values == null) {
            return Collections.emptySet();
        }

        return values.stream()
                     .filter(value -> value != null && !value.isBlank())
                     .map(String::trim)
                     .map(String::toUpperCase)
                     .map(value -> value.startsWith("ROLE_") ? value : value)
                     .collect(Collectors.toCollection(HashSet::new));
    }

    public static Set<String> extractAuthorities(final Set<String> values) {
        return normalizeAuthorityAndRoleNames(values).stream()
                                                     .filter(value -> !value.startsWith("ROLE_"))
                                                     .collect(Collectors.toCollection(HashSet::new));
    }

    public static Set<String> extractRoles(final Set<String> values) {
        return normalizeAuthorityAndRoleNames(values).stream()
                                                     .filter(value -> value.startsWith("ROLE_"))
                                                     .collect(Collectors.toCollection(HashSet::new));
    }
}
