package io.github.blakedunaway.authserver.security.token;

import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;

public class PlatformUserJwtAuthenticationToken extends JwtAuthenticationToken {

    private final PlatformUser platformUser;

    public PlatformUserJwtAuthenticationToken(final Jwt jwt,
                                              final Collection<? extends GrantedAuthority> authorities,
                                              final String name,
                                              final PlatformUser platformUser) {
        super(jwt, authorities, name);
        this.platformUser = platformUser;
    }

    @Override
    public Object getPrincipal() {
        return platformUser;
    }

    public PlatformUser getPlatformUser() {
        return platformUser;
    }
}
