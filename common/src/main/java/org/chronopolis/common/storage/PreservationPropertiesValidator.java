package org.chronopolis.common.storage;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * @author shake
 */
public class PreservationPropertiesValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return PreservationProperties.class == clazz;
    }

    @Override
    public void validate(Object target, Errors errors) {
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
