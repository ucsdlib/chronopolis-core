package org.chronopolis.db;

import org.chronopolis.db.ingest.IngestDB;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by shake on 4/9/14.
 */
public class DatabaseManager {
    @Autowired
    IngestDB ingestDatabase;

    public IngestDB getIngestDatabase() {
        return ingestDatabase;
    }
}
