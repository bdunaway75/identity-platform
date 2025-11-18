package io.github.blakedunaway.authserver.business.model;

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
    String email;

    String password;

    String registeredClientId;

    public UsernamePasswordWithClientAuthenticationToken toAuthenticationToken() {
        return new UsernamePasswordWithClientAuthenticationToken(this);
    }


    public static class UsernamePasswordWithClientAuthenticationToken extends AbstractAuthenticationToken {


        @Getter
        private RegisterDto registerDto;

        public UsernamePasswordWithClientAuthenticationToken(final RegisterDto registerDto) {
            super(new HashSet<>());
            this.registerDto = registerDto;
        }

        public UsernamePasswordWithClientAuthenticationToken(RegisterDto registerDto,
                                                             Collection<? extends GrantedAuthority> authorities, boolean authenticated) {
            super(authorities);
            this.registerDto = registerDto;
            super.setAuthenticated(authenticated); // must use super, as we override
        }

        public static UsernamePasswordWithClientAuthenticationToken authenticated(RegisterDto registerDto,
                                                                                  Collection<? extends GrantedAuthority> authorities) {
            return new UsernamePasswordWithClientAuthenticationToken(registerDto, authorities, true);
        }

        public static UsernamePasswordWithClientAuthenticationToken unauthenticated(RegisterDto registerDto,
                                                                                  Collection<? extends GrantedAuthority> authorities) {
            return new UsernamePasswordWithClientAuthenticationToken(registerDto, authorities,  false);
        }

        @Override
        public Object getCredentials() {
            return registerDto.getPassword();
        }

        @Override
        public Object getPrincipal() {
            return registerDto.getEmail();
        }

        @Override
        public String getName() {
            return registerDto.getEmail();
        }

        @Override
        public void eraseCredentials() {
            registerDto.setPassword(null);
        }

    }

}
