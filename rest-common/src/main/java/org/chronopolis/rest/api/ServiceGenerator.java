package org.chronopolis.rest.api;

/**
 * Interface to create the various services for use when communicating with the
 * Ingest server
 *
 * todo: Service -> API
 *
 * @author shake
 */
public interface ServiceGenerator {

    /**
     * Create a {@link BagService} for querying Bags in the Ingest Server
     *
     * @return a new BagService
     */
    BagService bags();

    /**
     * Create a {@link TokenService} for querying Bag Tokens in the Ingest Server
     *
     * @return a new TokenService
     */
    TokenService tokens();

    /**
     * Create a {@link RepairService} for querying Repairs in the Ingest Server
     *
     * @return a new RepairService
     */
    RepairService repairs();

    /**
     * Create a {@link StagingService} for querying Bag Staging information in the Ingest Server
     *
     * @return a new StagingService
     */
    StagingService staging();

    /**
     * Create a {@link DepositorAPI} for querying Depositors in the Ingest Server
     *
     * @return a new DepositorAPI
     */
    DepositorAPI depositorAPI();

    /**
     * Create a {@link StorageService} for querying Storage Regions in the Ingest Server
     *
     * @return a new StorageService
     */
    StorageService storage();

    /**
     * Create a {@link ReplicationService} for querying Replications the Ingest Server
     *
     * @return a new ReplicationService
     */
    ReplicationService replications();

}
