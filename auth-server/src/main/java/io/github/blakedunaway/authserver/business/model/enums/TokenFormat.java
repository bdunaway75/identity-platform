package io.github.blakedunaway.authserver.business.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;

@AllArgsConstructor
@Getter
public enum TokenFormat {

    @JsonProperty("self-contained")
    SELF("self-contained") {
        @Override
        public OAuth2TokenFormat getOAuth2TokenFormat() {
            return OAuth2TokenFormat.SELF_CONTAINED;
        }
    },
    @JsonProperty("reference")
    TOKEN("reference") {
        @Override
        public OAuth2TokenFormat getOAuth2TokenFormat() {
            return OAuth2TokenFormat.REFERENCE;
        }
    };

    private final String value;

    public abstract OAuth2TokenFormat getOAuth2TokenFormat();

    public static TokenFormat from(final String value) {
        for (TokenFormat tokenFormat : values()) {
            if (tokenFormat.getValue().equals(value)) {
                return tokenFormat;
            }
        }
        return null;
    }

}
