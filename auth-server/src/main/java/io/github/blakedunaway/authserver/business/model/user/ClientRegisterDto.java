package io.github.blakedunaway.authserver.business.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.HashSet;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientRegisterDto {

    private String email;

    @JsonIgnore
    private String password;

    private String clientId;

    public UsernamePasswordWithClientAuthenticationToken toAuthenticationToken() {
        return UsernamePasswordWithClientAuthenticationToken.unauthenticated(email, clientId, password);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsernamePasswordWithClientAuthenticationToken extends AbstractAuthenticationToken {

        @Getter
        private String email;

        @Getter
        private String clientId;

        @JsonIgnore
        private String password;

        private UsernamePasswordWithClientAuthenticationToken(String email,
                                                              String clientId,
                                                              String password,
                                                              Collection<? extends GrantedAuthority> authorities,
                                                              boolean authenticated) {
            super(authorities != null ? authorities : new HashSet<>());
            this.email = email;
            this.clientId = clientId;
            this.password = password;
            super.setAuthenticated(authenticated);
        }

        /** For initial login attempt (contains password, unauthenticated) */
        public static UsernamePasswordWithClientAuthenticationToken unauthenticated(String email,
                                                                                    String clientId,
                                                                                    String password) {
            return new UsernamePasswordWithClientAuthenticationToken(email, clientId, password, new HashSet<>(), false);
        }

        /** For post-auth success (no password, authenticated, includes authorities) */
        public static UsernamePasswordWithClientAuthenticationToken authenticated(String email,
                                                                                  String clientId,
                                                                                  Collection<? extends GrantedAuthority> authorities) {
            return new UsernamePasswordWithClientAuthenticationToken(email, clientId, null, authorities, true);
        }

        @Override
        @JsonIgnore
        public Object getCredentials() {
            return password;
        }

        @Override
        public Object getPrincipal() {
            return email;
        }

        @Override
        public String getName() {
            return email;
        }

        @Override
        public void eraseCredentials() {
            this.password = null;
        }
    }
}
