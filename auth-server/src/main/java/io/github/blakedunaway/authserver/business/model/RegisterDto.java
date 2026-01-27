// FILE: io/github/blakedunaway/authserver/business/model/RegisterDto.java
package io.github.blakedunaway.authserver.business.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.blakedunaway.authserver.business.validation.ValidEmail;
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
public class RegisterDto {

    @ValidEmail
    private String email;

    @JsonIgnore
    private String password;

    private String clientId;

    public UsernamePasswordWithClientAuthenticationToken toAuthenticationToken() {
        // ✅ FIXED: constructor arg order is (email, clientId, password)
        return UsernamePasswordWithClientAuthenticationToken.unauthenticated(email, clientId, password);
    }

    /**
     * Redis/OAuth2-safe Authentication token:
     * - Only email/clientId should ever be serialized.
     * - Password is runtime-only and must never be serialized.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsernamePasswordWithClientAuthenticationToken extends AbstractAuthenticationToken {

        @Getter
        private String email;

        @Getter
        private String clientId;

        @JsonIgnore
        private String password; // runtime-only

        /** Jackson / frameworks */
        public UsernamePasswordWithClientAuthenticationToken() {
            super(new HashSet<>());
            super.setAuthenticated(false);
        }

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
            // ✅ password must be null on successful Authentication
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

        /**
         * AbstractAuthenticationToken#setAuthenticated(true) is allowed, but if you want to
         * enforce “only via authenticated(...) factory”, you can override setAuthenticated
         * to throw when true. (Optional)
         */
        // @Override
        // public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        //     if (isAuthenticated) {
        //         throw new IllegalArgumentException("Use authenticated(...) factory method");
        //     }
        //     super.setAuthenticated(false);
        // }
    }
}
