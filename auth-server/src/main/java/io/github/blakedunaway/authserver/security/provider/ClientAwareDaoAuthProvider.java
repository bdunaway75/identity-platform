package io.github.blakedunaway.authserver.security.provider;

import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto.UsernamePasswordWithClientAuthenticationToken;
import io.github.blakedunaway.authserver.business.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@RequiredArgsConstructor
@Component
public class ClientAwareDaoAuthProvider implements AuthenticationProvider {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    private UserDetails retrieveUser(final UsernamePasswordWithClientAuthenticationToken authentication) {
        Assert.notNull(authentication, "authentication cannot be null");
        Assert.hasText(String.valueOf(authentication.getClientId()), "clientId cannot be empty");
        Assert.hasText(authentication.getEmail(), "email cannot be empty");
        return userService.loadUserDetailsByEmailAndClientId(authentication.getClientId(), authentication.getEmail());
    }

    private void defaultPreAuthenticationChecks(final UserDetails user) {
        if (!user.isAccountNonLocked()) {
            throw new LockedException("User account is locked");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }
        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("User account has expired");
        }

        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("User credentials have expired");
        }
    }

    private void defaultPostAuthenticationChecks(final UserDetails user) {
        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("ClientUser credentials have expired");
        }
    }

    private void additionalAuthenticationChecks(final UserDetails userDetails,
                                                final UsernamePasswordWithClientAuthenticationToken authentication)
            throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            throw new BadCredentialsException("Bad credentials");
        }
        final String presentedPassword = authentication.getCredentials().toString();
        if (!passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }
        // Important: never keep credentials past verification
        authentication.eraseCredentials();
    }

    private Authentication createSuccessAuthentication(final Authentication authentication,
                                                       final UserDetails user) {
        final UsernamePasswordWithClientAuthenticationToken token =
                (UsernamePasswordWithClientAuthenticationToken) authentication;

        final UsernamePasswordWithClientAuthenticationToken result =
                UsernamePasswordWithClientAuthenticationToken.authenticated(
                        token.getEmail(),
                        token.getClientId(),
                        user.getAuthorities()
                );

        result.setDetails(authentication.getDetails());
        return result;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(UsernamePasswordWithClientAuthenticationToken.class, authentication,
                            "Only UsernamePasswordWithClientAuthenticationToken is supported");

        final UsernamePasswordWithClientAuthenticationToken token =
                (UsernamePasswordWithClientAuthenticationToken) authentication;

        final UserDetails user;
        try {
            user = retrieveUser(token);
        } catch (UsernameNotFoundException exception) {
            throw new InternalAuthenticationServiceException(exception.getMessage(), exception);
        }

        Assert.notNull(user, "retrieveUser returned null");
        defaultPreAuthenticationChecks(user);
        additionalAuthenticationChecks(user, token);
        defaultPostAuthenticationChecks(user);

        return createSuccessAuthentication(authentication, user);
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordWithClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
