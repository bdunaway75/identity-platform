package io.github.blakedunaway.authserver.business.api.controller;
import io.github.blakedunaway.authserver.business.api.dto.request.ClientUserRequest;
import io.github.blakedunaway.authserver.business.api.dto.request.RegisteredClientRequest;
import io.github.blakedunaway.authserver.business.api.dto.response.AdminDashboardResponse;
import io.github.blakedunaway.authserver.business.api.dto.response.AuthTokenResponse;
import io.github.blakedunaway.authserver.business.api.dto.response.ClientUserActivity;
import io.github.blakedunaway.authserver.business.api.dto.response.ClientUserActivityResponse;
import io.github.blakedunaway.authserver.business.api.dto.response.ClientUserResponse;
import io.github.blakedunaway.authserver.business.api.dto.response.DemoAccessCodeDetailsResponse;
import io.github.blakedunaway.authserver.business.api.dto.response.PlatformUserDetailsReponse;
import io.github.blakedunaway.authserver.business.api.dto.response.PlatformUserTierResponse;
import io.github.blakedunaway.authserver.business.api.dto.response.RegisteredClientResponse;
import io.github.blakedunaway.authserver.business.model.Authority;
import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.model.user.ClientUser;
import io.github.blakedunaway.authserver.business.model.user.PlatformUser;
import io.github.blakedunaway.authserver.business.service.AuthTokenService;
import io.github.blakedunaway.authserver.business.service.DemoAccessCodeService;
import io.github.blakedunaway.authserver.business.service.PlatformUserTierService;
import io.github.blakedunaway.authserver.business.service.RegisteredClientService;
import io.github.blakedunaway.authserver.business.service.UserService;
import io.github.blakedunaway.authserver.config.redis.RedisStore;
import io.github.blakedunaway.authserver.mapper.DemoAccessCodeMapper;
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper;
import io.github.blakedunaway.authserver.util.RedisUtility;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Slf4j
public class PlatformApiController {

    private final DemoAccessCodeMapper demoAccessCodeMapper;

    private final RedisStore redisStore;

    private final UserService userService;

    private final AuthTokenService authTokenService;

    private final RegisteredClientService registeredClientService;

    private final PlatformUserTierService platformUserTierService;

    private final RegisteredClientMapper registeredClientMapper;

    private final DemoAccessCodeService demoAccessCodeService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<?> createClient(@AuthenticationPrincipal final PlatformUser platformUser,
                                          @RequestBody final RegisteredClientRequest registeredClientRequest) {
        try {
            final RegisteredClientModel requestedRegisteredClient =
                    registeredClientMapper.registeredClientRequestToRegisteredClientModel(registeredClientRequest);

            final Set<RegisteredClientModel> registeredClientModels =
                    registeredClientService.findRegisteredClientsByIds(platformUser.registeredClientIdsOrEmpty());

            platformUser.replaceRegisteredClientAndValidateTier(registeredClientModels, requestedRegisteredClient);

            final RegisteredClientModel model = registeredClientService.saveRegisteredClient(requestedRegisteredClient);
            userService.attachRegisteredClientToPlatformUser(platformUser.getEmail(), model.getId());
            return ResponseEntity.ok(RegisteredClientResponse.fromCreatedModel(model));
        } catch (final ValidationException | IllegalArgumentException e) {
            log.warn("Create client validation failed for platform user {}.", platformUser.getEmail(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (final Exception e) {
            log.error("Create client failed unexpectedly for platform user {}.", platformUser.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('PLATFORM_USER')")
    public ResponseEntity<PlatformUserDetailsReponse> getDashboard(@AuthenticationPrincipal final PlatformUser platformUser) {
        final Set<RegisteredClientResponse> registeredClients =
                registeredClientService.findRegisteredClientsByIds(platformUser.registeredClientIdsOrEmpty())
                                       .stream()
                                       .map(RegisteredClientResponse::fromModel)
                                       .collect(Collectors.toCollection(HashSet::new));

        final PlatformUserDetailsReponse platformUserDetailsReponse =
                PlatformUserDetailsReponse.from(registeredClients, platformUser.getTier())
                                          .totalUsers(userService.getTotalUserCount(platformUser.getEmail()))
                                          .isDemoUser(platformUser.isDemoUser())
                                          .isAdmin(platformUser.isPlatformAdmin())
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
    public ResponseEntity<Set<ClientUserResponse>> getClientUsers(@AuthenticationPrincipal final PlatformUser platformUser,
                                                                  @RequestBody Set<UUID> registeredClientIds) {
        return ResponseEntity.ok(userService.findClientUsersByRegisteredClientIds(
                                                    platformUser.filterOwnedRegisteredClientIds(registeredClientIds))
                                            .stream()
                                            .map(ClientUserResponse::fromModel)
                                            .collect(Collectors.toCollection(HashSet::new)));
    }

    @PatchMapping("/users/{clientUserId}")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<ClientUserResponse> updateClientUser(@AuthenticationPrincipal final PlatformUser platformUser,
                                                               @PathVariable final UUID clientUserId,
                                                               @RequestBody final ClientUserRequest request) {
        final ClientUser updatedClientUser =
                userService.updateClientUser(
                        clientUserId,
                        platformUser.getRegisteredClientIds(),
                        request
                );

        if (updatedClientUser == null) {
            log.warn("Client user {} was not found or not owned by platform user {}.", clientUserId, platformUser.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(ClientUserResponse.fromModel(updatedClientUser));
    }

    @PostMapping("/tokens")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<Set<AuthTokenResponse>> getClientAuthTokens(@AuthenticationPrincipal final PlatformUser platformUser,
                                                                      @RequestBody Set<UUID> registeredClientIds) {
        return ResponseEntity.ok(authTokenService.findAllByRegisteredClientIds(
                                                         platformUser.filterOwnedRegisteredClientIds(registeredClientIds))
                                                 .stream()
                                                 .map(AuthTokenResponse::fromModel)
                                                 .collect(Collectors.toCollection(HashSet::new)));
    }

    @PatchMapping("/{registeredClientId}/update")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<?> updateRegisteredClient(@AuthenticationPrincipal final PlatformUser platformUser,
                                                    @PathVariable final UUID registeredClientId,
                                                    @RequestBody final RegisteredClientRequest registeredClientRequest) {
        final RegisteredClientModel updated;
        try {
            updated = registeredClientMapper.registeredClientRequestToRegisteredClientModel(registeredClientRequest);
        } catch (final ValidationException | IllegalArgumentException e) {
            log.warn("Registered client update validation failed for client {} by platform user {}.", registeredClientId, platformUser.getEmail(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        if (!platformUser.ownsRegisteredClientId(registeredClientId)) {
            log.warn("Platform user {} attempted to update unowned registered client {}.", platformUser.getEmail(), registeredClientId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final Set<RegisteredClientModel> clients = new HashSet<>(
                registeredClientService.findRegisteredClientsByIds(platformUser.registeredClientIdsOrEmpty())
        );
        final RegisteredClientModel existingRegisteredClient = platformUser.findOwnedRegisteredClientById(clients, registeredClientId);
        if (existingRegisteredClient == null) {
            log.warn("Registered client {} could not be resolved for platform user {} during update.", registeredClientId, platformUser.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final RegisteredClientModel resolvedUpdatedRegisteredClient;
        try {
            resolvedUpdatedRegisteredClient =
                    registeredClientService.previewUpdatedRegisteredClient(existingRegisteredClient, updated);
            if (resolvedUpdatedRegisteredClient == null) {
                log.warn("Registered client {} preview update returned null for platform user {}.", registeredClientId, platformUser.getEmail());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            platformUser.replaceRegisteredClientAndValidateTier(clients, resolvedUpdatedRegisteredClient);
        } catch (final ValidationException e) {
            log.warn("Registered client {} update failed tier validation for platform user {}.", registeredClientId, platformUser.getEmail(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        userService.removeRemovedRegisteredClientAuthorities(existingRegisteredClient, resolvedUpdatedRegisteredClient);

        final RegisteredClientModel savedUpdatedRegisteredClient =
                registeredClientService.updateRegisteredClient(existingRegisteredClient, updated);

        if (savedUpdatedRegisteredClient == null) {
            log.warn("Registered client {} update did not persist for platform user {}.", registeredClientId, platformUser.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(RegisteredClientResponse.fromModel(savedUpdatedRegisteredClient));
    }

    @PostMapping("/tokens/{authTokenId}/invalidate")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<Void> invalidateClientAuthToken(@AuthenticationPrincipal final PlatformUser platformUser,
                                                          @PathVariable final UUID authTokenId) {
        return authTokenService.invalidateByIdAndRegisteredClientIds(authTokenId, platformUser.getRegisteredClientIds())
               ? ResponseEntity.noContent().build()
               : logInvalidateTokenNotFound(authTokenId, platformUser.getEmail());
    }

    @PostMapping("/{registeredClientId}/tokens/invalidate")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<Integer> invalidateRegisteredClientTokens(@AuthenticationPrincipal final PlatformUser platformUser,
                                                                    @PathVariable final UUID registeredClientId) {
        if (!platformUser.ownsRegisteredClientId(registeredClientId)) {
            log.warn("Platform user {} attempted to invalidate tokens for unowned registered client {}.", platformUser.getEmail(), registeredClientId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(authTokenService.invalidateAllByRegisteredClientId(registeredClientId));
    }

    @PostMapping("/recent-user-activity")
    @PreAuthorize("hasRole('PLATFORM_USER') and hasAuthority('PLATFORM_TIER_PAID')")
    public ResponseEntity<ClientUserActivityResponse> getRecentUserActivity(@AuthenticationPrincipal final PlatformUser platformUser,
                                                                            @RequestBody final Set<String> clientIds) {
        final Set<String> requestedClientIds = clientIds == null ? Collections.emptySet() : clientIds;
        if (requestedClientIds.isEmpty()) {
            return ResponseEntity.ok(ClientUserActivityResponse.builder()
                                                               .logins(Collections.emptyList())
                                                               .signups(Collections.emptyList())
                                                               .build());
        }

        final Set<RegisteredClientModel> ownedClients = registeredClientService.findRegisteredClientsByIds(platformUser.registeredClientIdsOrEmpty());
        if (!platformUser.ownsAnyClientIds(ownedClients, requestedClientIds)) {
            log.warn("Platform user {} requested recent activity for unowned or unknown client ids {}.", platformUser.getEmail(), requestedClientIds);
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

    @PostMapping("/admin/dashboard")
    @PreAuthorize("hasRole('PLATFORM_ADMIN') and hasAuthority('PLATFORM_ADMIN_ACCESS')")
    public ResponseEntity<AdminDashboardResponse> getAdminDashboard(@AuthenticationPrincipal final PlatformUser platformUser) {
        final List<DemoAccessCodeDetailsResponse> responses = demoAccessCodeService.findAll()
                                                                                   .stream()
                                                                                   .map(demoAccessCodeMapper::demoAccessCodeToDemoAccessCodeDetailsResponse)
                                                                                   .toList();

        return ResponseEntity.ok(AdminDashboardResponse.builder().demoCodes(responses).build());
    }

    private ResponseEntity<Void> logInvalidateTokenNotFound(final UUID authTokenId,
                                                            final String platformUserEmail) {
        log.warn("Platform user {} attempted to invalidate unknown or unowned auth token {}.", platformUserEmail, authTokenId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

}
