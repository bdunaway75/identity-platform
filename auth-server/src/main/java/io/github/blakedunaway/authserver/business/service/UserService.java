package io.github.blakedunaway.authserver.business.service;

import io.github.blakedunaway.authserver.business.model.RegisterDto;
import io.github.blakedunaway.authserver.business.model.User;
import io.github.blakedunaway.authserver.integration.repository.gateway.UserRepository;
import io.github.blakedunaway.authserver.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    public User signUp(final RegisterDto registerDto) {
        if (registerDto.getEmail().isBlank() || registerDto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        return saveUser(userMapper.registerDtoToUser(registerDto));
    }

    public User saveUser(final User user) {
        try {
            Assert.notNull(user, "User cannot be null");
            Assert.notNull(user.getEmail(), "Email cannot be null");
            Assert.notNull(user.getPasswordHash(), "Password hash cannot be null");
            Assert.isTrue(user.getPasswordHash().startsWith("$argon2"), "Password has not been hashed");
            if (userRepository.findByRegisteredClient_IdAndEmail(user.getRegisteredClientId(),
                                                                 user.getEmail()) != null) {
                return userRepository.save(User.fromUser(user).isNew(false).build());
            } else {
                return userRepository.save(user);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("User already exists or constraint failed", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Unexpected error saving user", ex);
        }
    }

    public UserDetails loadUserByUsernameAndClientId(final String username, final String clientId) throws UsernameNotFoundException {
        Assert.notNull(username, "Username cannot be null");
        Assert.notNull(clientId, "ClientId cannot be null");
        final User user = userRepository.findByEmail(username);
        if (user == null) {
            return null;
        }
        return user.toSpring();
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        Assert.notNull(username, "Username cannot be null");
        final User user = userRepository.findByEmail(username);
        if (user == null) {
            return null;
        }
        return user.toSpring();
    }

}
