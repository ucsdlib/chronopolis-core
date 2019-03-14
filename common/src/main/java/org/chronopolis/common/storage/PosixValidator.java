package org.chronopolis.common.storage;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Simple validator for a Posix Configuration Property. Ensures that
 * the path exists on disk and has the proper permissions set.
 *
 * Read bit for digesting files
 * Write bit for writing files
 * Execute bit for cd
 *
 * @author shake
 */
public class PosixValidator implements Validator {
    @Override
    public boolean supports(@Nullable Class<?> clazz) {
        return Posix.class == clazz;
    }

    @Override
    public void validate(@Nullable Object target, @Nullable Errors errors) {
        Posix posix = (Posix) target;

        if (posix.getPath() != null) {
            Path path = Paths.get(posix.getPath());
            File file = path.toFile();
            if (!file.exists()) {
                errors.reject("path", "Path does not exist");
            } else if (!file.isDirectory()) {
                errors.reject("path", "Path is not a directory");
            } else if (!file.canWrite() || !file.canRead() || !file.canExecute()) {
                errors.reject("path", "Permissions for path do not allow read/write/execute");
            }
        }
    }
}
