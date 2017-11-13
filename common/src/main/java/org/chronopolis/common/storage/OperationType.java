package org.chronopolis.common.storage;

/**
 * Some StorageOperation types
 *
 * Only rsync for now, might want separate things
 * for staging/https/ftp/etc
 *
 * @author shake
 */
public enum OperationType {

    RSYNC, NOP

}
