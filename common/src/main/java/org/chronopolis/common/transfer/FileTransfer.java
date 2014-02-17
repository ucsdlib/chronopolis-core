package org.chronopolis.common.transfer;

import java.nio.file.Path;

/**
 * Created by shake on 2/17/14.
 */
public interface FileTransfer {
    public Path getFile(String uri, Path localStorage);
}
