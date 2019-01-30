package org.chronopolis.common.storage;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import javax.annotation.Nullable;

/**
 * @author shake
 */
public class BagStagingPropertiesValidator implements Validator {
    @Override
    public boolean supports(@Nullable Class<?> clazz) {
        return BagStagingProperties.class == clazz;
    }

    @Override
    public void validate(@Nullable Object target, @Nullable Errors errors) {
        PosixValidator validator = new PosixValidator();
        BagStagingProperties properties = (BagStagingProperties) target;
        if (properties.getPosix() == null) {
            errors.reject("posix", "No staging area defined");
        } else {
            validator.validate(properties.getPosix(), errors);
        }
    }
}
