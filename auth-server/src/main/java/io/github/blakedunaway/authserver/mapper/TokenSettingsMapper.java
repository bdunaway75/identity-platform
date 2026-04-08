package io.github.blakedunaway.authserver.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.blakedunaway.authserver.business.model.enums.TokenFormat;
import io.github.blakedunaway.authserver.integration.TokenSettingsJson;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TokenSettingsMapper {

    private final ObjectMapper objectMapper;

    public TokenSettings tokenSettingsJsonToOAuthTokenSettings(final TokenSettingsJson tokenSettingsJson) {
        if (tokenSettingsJson == null) {
            return null;
        }
        return TokenSettings.builder()
                            .accessTokenFormat(tokenSettingsJson.getAccessTokenFormat().getOAuth2TokenFormat())
                            .idTokenSignatureAlgorithm(SignatureAlgorithm.from(tokenSettingsJson.getIdTokenSignatureAlgorithm()))
                            .authorizationCodeTimeToLive(tokenSettingsJson.getAuthorizationCodeTimeToLive())
                            .accessTokenTimeToLive(tokenSettingsJson.getAccessTokenTimeToLive())
                            .refreshTokenTimeToLive(tokenSettingsJson.getRefreshTokenTimeToLive())
                            .reuseRefreshTokens(tokenSettingsJson.isReuseRefreshTokens())
                            .x509CertificateBoundAccessTokens(tokenSettingsJson.isX509CertificateBoundAccessTokens())
                            .deviceCodeTimeToLive(tokenSettingsJson.getDeviceCodeTimeToLive())
                            .build();
    }

    public TokenSettingsJson oAuthTokenSettingsToTokenSettingsJson(final TokenSettings oAuthTokenSettings) {
        if (oAuthTokenSettings == null) {
            return null;
        }
        return TokenSettingsJson.builder()
                                .accessTokenFormat(TokenFormat.from(oAuthTokenSettings.getAccessTokenFormat().getValue()))
                                .idTokenSignatureAlgorithm(oAuthTokenSettings.getIdTokenSignatureAlgorithm().getName())
                                .authorizationCodeTimeToLive(oAuthTokenSettings.getAuthorizationCodeTimeToLive())
                                .accessTokenTimeToLive(oAuthTokenSettings.getAccessTokenTimeToLive())
                                .refreshTokenTimeToLive(oAuthTokenSettings.getRefreshTokenTimeToLive())
                                .reuseRefreshTokens(oAuthTokenSettings.isReuseRefreshTokens())
                                .x509CertificateBoundAccessTokens(oAuthTokenSettings.isX509CertificateBoundAccessTokens())
                                .deviceCodeTimeToLive(oAuthTokenSettings.getDeviceCodeTimeToLive())
                                .build();
    }

    public TokenSettingsJson mapToTokenSettingsJson(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        return objectMapper.convertValue(map, TokenSettingsJson.class);
    }

}
