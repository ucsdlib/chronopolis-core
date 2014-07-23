package org.chronopolis.common.restore;

import java.nio.file.Path;

/**
 * Created by shake on 7/22/14.
 */
public interface CollectionRestore {

    /**
     * Restore a collection that is held in Chronopolis
     *
     * @param depositor The depositor who owns the collection
     * @param collection The collection to restore
     * @return The relative path of the restored collection
     */
    Path restore(String depositor, String collection);
}
