package org.chronopolis.db;

import org.chronopolis.db.ingest.IngestDB;
import org.chronopolis.db.ingest.ReplicationFlowTable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by shake on 4/9/14.
 */
public class DatabaseManager {

    @Autowired
    private IngestDB ingestDatabase;

    @Autowired
    private ReplicationFlowTable replicationFlowTable;

    public IngestDB getIngestDatabase() {
        return ingestDatabase;
    }

    public void setIngestDatabase(final IngestDB ingestDatabase) {
        this.ingestDatabase = ingestDatabase;
    }

    public ReplicationFlowTable getReplicationFlowTable() {
        return replicationFlowTable;
    }

}
