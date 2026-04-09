package io.github.blakedunaway.authserver.business.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Authority {

    private final UUID authorityId;

    private final String name;

    private boolean isNew;

    public static Authority from(final GrantedAuthority simpleGrantedAuthority) {
        return Authority.builder().name(simpleGrantedAuthority.getAuthority().toUpperCase()).build();
    }

    public static Authority from(final String authorityName) {
        return Authority.builder().name(authorityName.toUpperCase()).build();
    }

    public SimpleGrantedAuthority toSimpleGrantedAuthority() {
        return new SimpleGrantedAuthority(this.getName().toUpperCase());
    }

}
