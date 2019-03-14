package org.chronopolis.common.storage;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import javax.annotation.Nullable;

/**
 * @author shake
 */
public class TokenStagingPropertiesValidator implements Validator {
    @Override
    public boolean supports(@Nullable Class<?> clazz) {
        return TokenStagingProperties.class == clazz;
    }

    @Override
    public void validate(@Nullable Object target, @Nullable Errors errors) {
        PosixValidator validator = new PosixValidator();
        TokenStagingProperties properties = (TokenStagingProperties) target;
        if (properties.getPosix() == null) {
            errors.reject("posix", "No staging area defined");
        } else {
            validator.validate(properties.getPosix(), errors);
        }
    }
}
