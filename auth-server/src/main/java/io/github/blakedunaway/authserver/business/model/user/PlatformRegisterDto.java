package io.github.blakedunaway.authserver.business.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.blakedunaway.authserver.business.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.HashSet;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlatformRegisterDto {

    @ValidEmail
    @NotBlank(message = "Email is required")
    private String email;

    @JsonIgnore
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;


    public UsernamePasswordWithPlatformAuthenticationToken toAuthenticationToken() {
        return UsernamePasswordWithPlatformAuthenticationToken.unauthenticated(email, password);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UsernamePasswordWithPlatformAuthenticationToken extends AbstractAuthenticationToken {

        @Setter
        @Getter
        private String email;

        @JsonIgnore
        private String password;

        public UsernamePasswordWithPlatformAuthenticationToken() {
            super(new HashSet<>());
            super.setAuthenticated(false);
        }

        private UsernamePasswordWithPlatformAuthenticationToken(String email,
                                                                String password,
                                                                Collection<? extends GrantedAuthority> authorities,
                                                                boolean authenticated) {
            super(authorities != null ? authorities : new HashSet<>());
            this.email = email;
            this.password = password;
            super.setAuthenticated(authenticated);
        }

        /** For initial login attempt (contains password, unauthenticated) */
        public static UsernamePasswordWithPlatformAuthenticationToken unauthenticated(String email,
                                                                                      String password) {
            return new UsernamePasswordWithPlatformAuthenticationToken(email, password, new HashSet<>(), false);
        }

        /** For post-auth success (no password, authenticated, includes authorities) */
        public static UsernamePasswordWithPlatformAuthenticationToken authenticated(String email,
                                                                                    Collection<? extends GrantedAuthority> authorities) {
            return new UsernamePasswordWithPlatformAuthenticationToken(email, null, authorities, true);
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
