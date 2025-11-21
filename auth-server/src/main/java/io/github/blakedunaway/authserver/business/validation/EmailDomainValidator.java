package io.github.blakedunaway.authserver.business.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class EmailDomainValidator implements ConstraintValidator<ValidEmail, String> {

    private final EmailValidator hibernateEmailValidator = new EmailValidator();

    @Override
    public boolean isValid(final String email, final ConstraintValidatorContext context) {
        if (hibernateEmailValidator.isValid(email, context)) {
            final String domain = email.substring(email.indexOf("@") + 1);
            try {
                return InetAddress.getByName(domain) != null;
            } catch (UnknownHostException e) {
                return false;
            }
        }
        return false;
    }
}
