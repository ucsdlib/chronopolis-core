package org.chronopolis.common.storage;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * @author shake
 */
public class TokenStagingPropertiesValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return TokenStagingProperties.class == clazz;
    }

    @Override
    public void validate(Object target, Errors errors) {
        PosixValidator validator = new PosixValidator();
        TokenStagingProperties properties = (TokenStagingProperties) target;
        if (properties.getPosix() == null) {
            errors.reject("posix", "No staging area defined");
        } else {
            validator.validate(properties.getPosix(), errors);
        }
    }
}
