package io.github.blakedunaway.authserver.business.validation;

import io.github.blakedunaway.authserver.business.model.user.PlatformRegisterDto;
import io.github.blakedunaway.authserver.business.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformCredentialValidator implements Validator {

    private final EmailDomainValidator emailDomainValidator;

    private final UserService userService;

    private final MessageSource messageSource;

    @Value("${auth-server.validation.password-min-length}")
    private int passwordMinLength;

    @Value("${auth-server.validation.password-max-length}")
    private int passwordMaxLength;

    @Override
    public boolean supports(@NonNull final Class<?> cls) {
        return PlatformRegisterDto.class.isAssignableFrom(cls);
    }

    @Override
    public void validate(final Object target, @NonNull final Errors errors) {
        validateForLogin((PlatformRegisterDto) target, errors);
    }

    public void validateForLogin(final PlatformRegisterDto platformRegisterDto, @NonNull final Errors errors) {
        final boolean validCredentials = validateCredentials(platformRegisterDto, errors);
        if (!validCredentials) {
            return;
        }

        if (userService.loadPlatformUserByEmail(platformRegisterDto.getEmail()) == null) {
            log.warn(resolveMessage("validation.email.notFound.log.platform", platformRegisterDto.getEmail()));
            errors.rejectValue("email", "validation.email.notFound");
        }
    }

    public void validateForSignUp(final PlatformRegisterDto platformRegisterDto, @NonNull final Errors errors) {
        final boolean validCredentials = validateCredentials(platformRegisterDto, errors);
        if (!validCredentials) {
            return;
        }

        if (userService.loadPlatformUserByEmail(platformRegisterDto.getEmail()) != null) {
            log.warn(resolveMessage("validation.email.duplicate.log.platform", platformRegisterDto.getEmail()));
            errors.rejectValue("email", "validation.email.duplicate");
        }
    }

    private boolean validateCredentials(final PlatformRegisterDto platformRegisterDto, @NonNull final Errors errors) {
        final boolean validEmail = validateEmail(platformRegisterDto, errors);
        final boolean validPassword = validatePassword(platformRegisterDto, errors);
        return validEmail && validPassword;
    }

    private boolean validateEmail(final PlatformRegisterDto platformRegisterDto, final Errors errors) {
        if (StringUtils.isBlank(platformRegisterDto.getEmail())) {
            errors.rejectValue("email", "validation.email.required");
            return false;
        }

        if (!emailDomainValidator.isValidEmail(platformRegisterDto.getEmail())) {
            errors.rejectValue("email", "validation.email.invalid");
            return false;
        }
        return true;
    }

    private boolean validatePassword(final PlatformRegisterDto platformRegisterDto, final Errors errors) {
        if (StringUtils.isBlank(platformRegisterDto.getPassword())) {
            errors.rejectValue("password", "validation.password.required");
            return false;
        }

        if (platformRegisterDto.getPassword().length() < passwordMinLength
            || platformRegisterDto.getPassword().length() > passwordMaxLength) {
            errors.rejectValue("password", "validation.password.length", new Object[]{(Object) passwordMinLength, (Object) passwordMaxLength}, null);
            return false;
        }
        return true;
    }

    private String resolveMessage(final String code, final Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
