package io.github.blakedunaway.authserver.business.api.controller;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.blakedunaway.authserver.business.api.dto.AuthTokenResponse;
import io.github.blakedunaway.authserver.business.api.dto.ClientUserActivity;
import io.github.blakedunaway.authserver.business.api.dto.ClientUserActivityResponse;
import io.github.blakedunaway.authserver.business.api.dto.ClientUserRequest;
import io.github.blakedunaway.authserver.business.api.dto.ClientUserResponse;
import io.github.blakedunaway.authserver.business.api.dto.PlatformUserDetailsReponse;
import io.github.blakedunaway.authserver.business.api.dto.PlatformUserTierResponse;
import io.github.blakedunaway.authserver.business.api.dto.RegisteredClientRequest;
import io.github.blakedunaway.authserver.business.api.dto.RegisteredClientResponse;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUserTier;
import io.github.blakedunaway.authserver.business.service.AuthTokenService;
import io.github.blakedunaway.authserver.business.service.PlatformUserTierService;
import io.github.blakedunaway.authserver.business.service.RegisteredClientService;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper;
import io.github.blakedunaway.authserver.util.RedisUtility;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/platform/api")
@RequiredArgsConstructor
public class PlatformApiController {

    private final RedisStore redisStore;

    private final UserService userService;

    private final AuthTokenService authTokenService;

    private final RegisteredClientService registeredClientService;

    private final PlatformUserTierService platformUserTierService;

    private final RegisteredClientMapper registeredClientMapper;

    private final StripeClient stripeClient;

    private <T> ResponseEntity<T> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<?> createClient(@AuthenticationPrincipal final Jwt jwt,
                                          @RequestBody final RegisteredClientRequest registeredClientRequest) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }
        try {
            final RegisteredClientModel requestedRegisteredClient =
                    registeredClientMapper.registeredClientRequestToRegisteredClientModel(registeredClientRequest);

            final Set<RegisteredClientModel> registeredClientModels =
                    registeredClientService.findRegisteredClientsByIds(platformUser.getRegisteredClientIds());

            registeredClientModels.add(requestedRegisteredClient);
            userService.validatePlatformUserTierCompliance(platformUser,
                                                           registeredClientModels);

            final RegisteredClientModel model = registeredClientService.saveRegisteredClient(requestedRegisteredClient);
            userService.attachRegisteredClientToPlatformUser(jwt.getSubject(), model.getId());
            return ResponseEntity.ok(RegisteredClientResponse.fromCreatedModel(model));
        } catch (final ValidationException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (final Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<PlatformUserDetailsReponse> getDashboard(@AuthenticationPrincipal final Jwt jwt) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        final Set<UUID> registeredClientIds = platformUser.getRegisteredClientIds() == null
                                              ? Collections.emptySet()
                                              : platformUser.getRegisteredClientIds();

        final Set<RegisteredClientResponse> registeredClients = registeredClientService.findRegisteredClientsByIds(
                                                                                               registeredClientIds)
                                                                                       .stream()
                                                                                       .map(RegisteredClientResponse::fromModel)
                                                                                       .collect(Collectors.toCollection(HashSet::new));

        final PlatformUserDetailsReponse platformUserDetailsReponse =
                PlatformUserDetailsReponse.from(registeredClients, platformUser.getTier())
                                          .totalUsers(userService.getTotalUserCount(platformUser.getEmail()))
                                          .build();
        return ResponseEntity.ok(platformUserDetailsReponse);
    }

    @GetMapping("/tiers")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<List<PlatformUserTierResponse>> getPlatformUserTiers() {
        return ResponseEntity.ok(platformUserTierService.findAllTiers()
                                                        .stream()
                                                        .map(PlatformUserTierResponse::fromModel)
                                                        .toList());
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<Set<ClientUserResponse>> getClientUsers(@AuthenticationPrincipal final Jwt jwt,
                                                                  @RequestBody Set<UUID> registeredClientIds) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(userService.findClientUsersByRegisteredClientIds(
                                                    userService.filterOwnedRegisteredClientIds(platformUser,
                                                                                               registeredClientIds))
                                            .stream()
                                            .map(ClientUserResponse::fromModel)
                                            .collect(Collectors.toCollection(HashSet::new)));
    }

    @PatchMapping("/users/{clientUserId}")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<ClientUserResponse> updateClientUser(@AuthenticationPrincipal final Jwt jwt,
                                                               @PathVariable final UUID clientUserId,
                                                               @RequestBody final ClientUserRequest request) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        final ClientUser updatedClientUser =
                userService.updateClientUser(
                        clientUserId,
                        platformUser.getRegisteredClientIds(),
                        request
                );

        if (updatedClientUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(ClientUserResponse.fromModel(updatedClientUser));
    }

    @PostMapping("/tokens")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<Set<AuthTokenResponse>> getClientAuthTokens(@AuthenticationPrincipal final Jwt jwt,
                                                                      @RequestBody Set<UUID> registeredClientIds) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(authTokenService.findAllByRegisteredClientIds(
                                                         userService.filterOwnedRegisteredClientIds(platformUser,
                                                                                                    registeredClientIds))
                                                 .stream()
                                                 .map(AuthTokenResponse::fromModel)
                                                 .collect(Collectors.toCollection(HashSet::new)));
    }

    @PatchMapping("/{registeredClientId}/update")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<?> updateRegisteredClient(@AuthenticationPrincipal final Jwt jwt,
                                                    @PathVariable final UUID registeredClientId,
                                                    @RequestBody final RegisteredClientRequest registeredClientRequest) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        final RegisteredClientModel updated;
        try {
            updated = registeredClientMapper.registeredClientRequestToRegisteredClientModel(registeredClientRequest);
        } catch (final ValidationException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        if (CollectionUtils.isEmpty(platformUser.getRegisteredClientIds()) || !platformUser.getRegisteredClientIds().contains(registeredClientId)) {
            //user doesnt own client
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final Set<RegisteredClientModel> clients = new HashSet<>(
                registeredClientService.findRegisteredClientsByIds(platformUser.getRegisteredClientIds())
        );
        final RegisteredClientModel existingRegisteredClient = clients.stream()
                                                                      .filter(client -> registeredClientId.equals(client.getId()))
                                                                      .findFirst()
                                                                      .orElse(null);
        if (existingRegisteredClient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final RegisteredClientModel resolvedUpdatedRegisteredClient;
        try {
            resolvedUpdatedRegisteredClient =
                    registeredClientService.previewUpdatedRegisteredClient(existingRegisteredClient, updated);
            if (resolvedUpdatedRegisteredClient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            clients.removeIf(client -> registeredClientId.equals(client.getId()));
            clients.add(resolvedUpdatedRegisteredClient);
            userService.validatePlatformUserTierCompliance(platformUser, clients);
        } catch (final ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        userService.removeRemovedRegisteredClientAuthorities(existingRegisteredClient, resolvedUpdatedRegisteredClient);

        final RegisteredClientModel savedUpdatedRegisteredClient =
                registeredClientService.updateRegisteredClient(existingRegisteredClient, updated);

        if (savedUpdatedRegisteredClient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(RegisteredClientResponse.fromModel(savedUpdatedRegisteredClient));
    }

    @PostMapping("/tokens/{authTokenId}/invalidate")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<Void> invalidateClientAuthToken(@AuthenticationPrincipal final Jwt jwt,
                                                          @PathVariable final UUID authTokenId) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        return authTokenService.invalidateByIdAndRegisteredClientIds(authTokenId, platformUser.getRegisteredClientIds())
               ? ResponseEntity.noContent().build()
               : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/{registeredClientId}/tokens/invalidate")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<Integer> invalidateRegisteredClientTokens(@AuthenticationPrincipal final Jwt jwt,
                                                                    @PathVariable final UUID registeredClientId) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        if (CollectionUtils.isEmpty(platformUser.getRegisteredClientIds()) || !platformUser.getRegisteredClientIds().contains(registeredClientId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(authTokenService.invalidateAllByRegisteredClientId(registeredClientId));
    }

    @PostMapping("/recent-user-activity")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<ClientUserActivityResponse> getRecentUserActivity(@AuthenticationPrincipal final Jwt jwt,
                                                                            @RequestBody final Set<String> clientIds) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        final Set<String> requestedClientIds = clientIds == null ? Collections.emptySet() : clientIds;
        if (requestedClientIds.isEmpty()) {
            return ResponseEntity.ok(ClientUserActivityResponse.builder()
                                                               .logins(Collections.emptyList())
                                                               .signups(Collections.emptyList())
                                                               .build());
        }

        final Set<UUID> ownedRegisteredClientIds = platformUser.getRegisteredClientIds() == null
                                                   ? Collections.emptySet()
                                                   : platformUser.getRegisteredClientIds();
        final Set<RegisteredClientModel> ownedClients = registeredClientService.findRegisteredClientsByIds(ownedRegisteredClientIds);
        if (CollectionUtils.isEmpty(ownedClients) || ownedClients.stream().map(RegisteredClientModel::getClientId).noneMatch(clientIds::contains)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final List<ClientUserActivity> logins = new ArrayList<>();
        final List<ClientUserActivity> signups = new ArrayList<>();

        for (final String clientId : requestedClientIds) {
            if (StringUtils.isEmpty(clientId)) {
                continue;
            }

            logins.addAll(redisStore.getList(RedisUtility.CLIENT_LOGIN_ATTRIBUTE + clientId));
            signups.addAll(redisStore.getList(RedisUtility.CLIENT_SIGNUP_ATTRIBUTE + clientId));
        }

        return ResponseEntity.ok(ClientUserActivityResponse.builder()
                                                           .logins(logins)
                                                           .signups(signups)
                                                           .build());
    }

}
