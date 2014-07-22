package org.chronopolis.common.restore;

import java.nio.file.Path;

/**
 * Created by shake on 7/22/14.
 */
public interface CollectionRestore {

    Path restore(String depositor, String collection);
}
