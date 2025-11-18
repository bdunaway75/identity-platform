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
public final class Authorities {

    private final UUID authorityId;

    private final String name;

    public static Authorities from(final GrantedAuthority simpleGrantedAuthority) {
        return Authorities.builder().name(simpleGrantedAuthority.getAuthority().toUpperCase()).build();
    }

    public static Authorities from(final String authorityName) {
        return Authorities.builder().name(authorityName.toUpperCase()).build();
    }

    public SimpleGrantedAuthority toSimpleGrantedAuthority() {
        return new SimpleGrantedAuthority(this.getName().toUpperCase());
    }


}
