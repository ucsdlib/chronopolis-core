package org.chronopolis.common.storage;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import javax.annotation.Nullable;

/**
 * @author shake
 */
public class PreservationPropertiesValidator implements Validator {

    @Override
    public boolean supports(@Nullable Class<?> clazz) {
        return PreservationProperties.class == clazz;
    }

    @Override
    public void validate(@Nullable Object target, @Nullable Errors errors) {
        PosixValidator posixValidator = new PosixValidator();
        PreservationProperties properties = (PreservationProperties) target;
        if (properties.getPosix().isEmpty()) {
            errors.reject("posix", "No storage defined");
        }

        for (Posix posix : properties.getPosix()) {
            posixValidator.validate(posix, errors);
        }
    }
}
