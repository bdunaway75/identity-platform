package io.github.blakedunaway.authserver.business.validation;

import io.github.blakedunaway.authserver.business.model.user.ClientRegisterDto;
import io.github.blakedunaway.authserver.business.service.RegisteredClientService;
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
public class ClientCredentialValidator implements Validator {

    private final EmailDomainValidator emailDomainValidator;

    private final RegisteredClientService registeredClientService;

    private final UserService userService;

    private final MessageSource messageSource;

    @Value("${auth-server.validation.password-min-length}")
    private int passwordMinLength;

    @Value("${auth-server.validation.password-max-length}")
    private int passwordMaxLength;

    @Override
    public boolean supports(@NonNull final Class<?> cls) {
        return ClientRegisterDto.class.isAssignableFrom(cls);
    }

    @Override
    public void validate(final Object target, @NonNull final Errors errors) {
        validateForLogin((ClientRegisterDto) target, errors);
    }

    public boolean validateClientId(final ClientRegisterDto clientRegisterDto, @NonNull final Errors errors) {
        if (StringUtils.isBlank(clientRegisterDto.getClientId()) || !registeredClientService.existsByClientId(clientRegisterDto.getClientId())) {
            log.warn(resolveMessage("validation.clientId.required.log", clientRegisterDto.getEmail()));
            errors.rejectValue("clientId", "validation.clientId.required");
            return false;
        }
        return true;
    }

    public void validateForLogin(final ClientRegisterDto clientRegisterDto, @NonNull final Errors errors) {
        final boolean validClientId = validateClientId(clientRegisterDto, errors);
        if (!validClientId) {
            return;
        }
        final boolean validCredentials = validateCredentials(clientRegisterDto, errors);
        if (validCredentials && !userService.existsClientIdAndEmail(clientRegisterDto.getClientId(), clientRegisterDto.getEmail())) {
            log.warn(resolveMessage("validation.email.notFound.log", clientRegisterDto.getEmail(), clientRegisterDto.getClientId()));
            errors.rejectValue("email", "validation.email.notFound");
        }
    }

    public void validateForSignUp(final ClientRegisterDto clientRegisterDto, @NonNull final Errors errors) {
        final boolean validClientId = validateClientId(clientRegisterDto, errors);
        if (!validClientId) {
            return;
        }

        final boolean validCredentials = validateCredentials(clientRegisterDto, errors);

        if (validCredentials && userService.existsClientIdAndEmail(clientRegisterDto.getClientId(), clientRegisterDto.getEmail())) {
            log.warn(resolveMessage("validation.email.duplicate.log", clientRegisterDto.getEmail(), clientRegisterDto.getClientId()));
            errors.rejectValue("email", "validation.email.duplicate");
        }
    }

    private boolean validateCredentials(final ClientRegisterDto clientRegisterDto, @NonNull final Errors errors) {
        final boolean validEmail = validateEmail(clientRegisterDto, errors);
        final boolean validPassword = validatePassword(clientRegisterDto, errors);
        return validEmail && validPassword;
    }

    private boolean validateEmail(final ClientRegisterDto clientRegisterDto, final Errors errors) {
        if (StringUtils.isBlank(clientRegisterDto.getEmail())) {
            errors.rejectValue("email", "validation.email.required");
            return false;
        }

        if (!emailDomainValidator.isValidEmail(clientRegisterDto.getEmail())) {
            errors.rejectValue("email", "validation.email.invalid");
            return false;
        }
        return true;
    }

    private boolean validatePassword(final ClientRegisterDto clientRegisterDto, final Errors errors) {
        if (StringUtils.isBlank(clientRegisterDto.getPassword())) {
            errors.rejectValue("password", "validation.password.required");
            return false;
        }

        if (clientRegisterDto.getPassword().length() < passwordMinLength
            || clientRegisterDto.getPassword().length() > passwordMaxLength) {
            errors.rejectValue("password", "validation.password.length", new Object[] {passwordMinLength, passwordMaxLength}, null);
            return false;
        }
        return true;
    }

    private String resolveMessage(final String code, final Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

}
