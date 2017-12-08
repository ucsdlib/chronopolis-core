package org.chronopolis.common.storage;

/**
 * Some StorageOperation types
 * <p>
 * Only rsync for now, might want separate things
 * for staging/https/ftp/etc
 *
 * @author shake
 */
public enum OperationType {

    RSYNC, NOP;

    public static OperationType from(String string) {
        switch (string) {
            case "rsync":
            case "RSYNC":
                return RSYNC;
            default:
                return NOP;
        }
    }

    }
