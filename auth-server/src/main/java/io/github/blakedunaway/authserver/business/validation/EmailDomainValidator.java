package io.github.blakedunaway.authserver.business.validation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@Component
public class EmailDomainValidator {

    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public boolean isValidEmail(final String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        return SIMPLE_EMAIL_PATTERN.matcher(email).matches() && hasResolvableDomain(email);
    }

    private boolean hasResolvableDomain(final String email) {
        final String domain = email.substring(email.indexOf("@") + 1);
        try {
            return InetAddress.getByName(domain) != null;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
