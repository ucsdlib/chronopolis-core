package org.chronopolis.rest.api;

/**
 * Interface to create the various services for use when communicating with the
 * Ingest server
 *
 * @author shake
 */
public interface ServiceGenerator {

    /**
     * Create a {@link BagService} for querying the Ingest Server
     *
     * @return a new BagService
     */
    BagService bags();

    /**
     * Create a {@link TokenService} for querying the Ingest Server
     *
     * @return a new TokenService
     */
    TokenService tokens();

    /**
     * Create a {@link RepairService} for querying the Ingest Server
     *
     * @return a new RepairService
     */
    RepairService repairs();

    /**
     * Create a {@link StagingService} for querying the Ingest Server
     *
     * @return a new StagingService
     */
    StagingService staging();

    /**
     * Create a {@link StorageService} for querying the Ingest Server
     *
     * @return a new StorageService
     */
    StorageService storage();

    /**
     * Create a {@link ReplicationService} for querying the Ingest Server
     *
     * @return a new ReplicationService
     */
    ReplicationService replications();

}
