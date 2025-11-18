package io.github.blakedunaway.authserver.business.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ClientRegistrationValidator.class)
@Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidClient {
    String message() default "Invalid client";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
