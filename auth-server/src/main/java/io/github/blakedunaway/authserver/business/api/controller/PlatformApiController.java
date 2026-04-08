package io.github.blakedunaway.authserver.business.api.controller;

import io.github.blakedunaway.authserver.business.api.dto.RegisteredClientRequest;
import io.github.blakedunaway.authserver.business.api.dto.RegisteredClientIdsRequest;
import io.github.blakedunaway.authserver.business.api.dto.RegisteredClientView;
import io.github.blakedunaway.authserver.business.api.dto.ClientUserView;
import io.github.blakedunaway.authserver.business.api.dto.ClientUserUpdateRequest;
import io.github.blakedunaway.authserver.business.model.CreatedRegisteredClient;
import io.github.blakedunaway.authserver.business.api.dto.AuthTokenView;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.enums.MetaDataKeys;
import io.github.blakedunaway.authserver.business.model.enums.Tier;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.service.AuthTokenService;
import io.github.blakedunaway.authserver.business.service.RegisteredClientService;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/platform/register-client")
@RequiredArgsConstructor
public class PlatformApiController {

    private final UserService userService;

    private final AuthTokenService authTokenService;

    private final RegisteredClientService registeredClientService;

    private final RegisteredClientMapper registeredClientMapper;

    private <T> ResponseEntity<T> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<RegisteredClientView> createClient(@AuthenticationPrincipal final Jwt jwt,
                                                             @RequestBody final RegisteredClientRequest registeredClientRequest) {
        try {
            final CreatedRegisteredClient result =
                    registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientRequestToRegisteredClientModel(registeredClientRequest));
            final RegisteredClientModel model = result.getRegisteredClient();
            userService.attachRegisteredClientToPlatformUser(jwt.getSubject(), model.getId());
            return ResponseEntity.ok(RegisteredClientView.fromCreatedRegisteredClient(result));
        } catch (final Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/total-user-count")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<Integer> getTotalUserCount(@AuthenticationPrincipal final Jwt jwt) {
        return ResponseEntity.ok(userService.getTotalUserCount(jwt.getSubject()));
    }

    @GetMapping("/total-client-count")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<Integer> getTotalClientCount(@AuthenticationPrincipal final Jwt jwt) {
        return ResponseEntity.ok(userService.getTotalClientCount(jwt.getSubject()));
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<Set<RegisteredClientView>> getRegisteredClients(@AuthenticationPrincipal final Jwt jwt) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        final Set<UUID> registeredClientIds = platformUser.getRegisteredClientIds() == null
                                             ? Collections.emptySet()
                                             : platformUser.getRegisteredClientIds();

        return ResponseEntity.ok(registeredClientService.findRegisteredClientsByIds(registeredClientIds)
                                                        .stream()
                                                        .map(RegisteredClientView::fromModel)
                                                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    @GetMapping("/{registeredClientId}")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<RegisteredClientView> getRegisteredClient(@AuthenticationPrincipal final Jwt jwt,
                                                                    @PathVariable final UUID registeredClientId) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        if (!userService.ownsRegisteredClient(platformUser, registeredClientId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final RegisteredClientModel registeredClient = registeredClientService.findRegisteredClientById(registeredClientId);
        if (registeredClient == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(RegisteredClientView.fromModel(registeredClient));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<Set<ClientUserView>> getClientUsers(@AuthenticationPrincipal final Jwt jwt,
                                                              @RequestBody final RegisteredClientIdsRequest request) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(userService.findClientUsersByRegisteredClientIds(
                                                    userService.filterOwnedRegisteredClientIds(platformUser, request.getRegisteredClientIds()))
                                            .stream()
                                            .map(ClientUserView::fromModel)
                                            .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    @PatchMapping("/users/{clientUserId}")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<ClientUserView> updateClientUser(@AuthenticationPrincipal final Jwt jwt,
                                                           @PathVariable final UUID clientUserId,
                                                           @RequestBody final ClientUserUpdateRequest request) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        final io.github.blakedunaway.authserver.business.model.user.ClientUser updatedClientUser = userService.updateClientUser(
                clientUserId,
                platformUser.getRegisteredClientIds(),
                request
        );

        if (updatedClientUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(ClientUserView.fromModel(updatedClientUser));
    }

    @PostMapping("/tokens")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<Set<AuthTokenView>> getClientAuthTokens(@AuthenticationPrincipal final Jwt jwt,
                                                                  @RequestBody final RegisteredClientIdsRequest request) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        return ResponseEntity.ok(authTokenService.findAllByRegisteredClientIds(
                                                    userService.filterOwnedRegisteredClientIds(platformUser, request.getRegisteredClientIds()))
                                                 .stream()
                                                 .map(AuthTokenView::fromModel)
                                                 .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    @PostMapping("/tokens/{authTokenId}/invalidate")
    @PreAuthorize("hasRole('PLATFORM_USER')")
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
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<Integer> invalidateRegisteredClientTokens(@AuthenticationPrincipal final Jwt jwt,
                                                                    @PathVariable final UUID registeredClientId) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }

        if (!userService.ownsRegisteredClient(platformUser, registeredClientId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(authTokenService.invalidateAllByRegisteredClientId(registeredClientId));
    }

    @GetMapping("/tier-status")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<String> getTierStatus(@AuthenticationPrincipal final Jwt jwt) {
        final PlatformUser platformUser = userService.loadPlatformUserByEmail(jwt.getSubject());
        if (platformUser == null) {
            return unauthorized();
        }
        final Map<String, String> attributes =
                (HashMap<String, String>) platformUser.getUserAttributes().get(MetaDataKeys.IDENTITY_PLATFORM.getValue());
        final Tier resolvedTier = Tier.findByName(attributes.get("tier"));
        return ResponseEntity.ok(resolvedTier.getName());
    }

}
