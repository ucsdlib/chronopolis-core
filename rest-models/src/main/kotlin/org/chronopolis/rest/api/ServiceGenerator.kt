package org.chronopolis.rest.api

/**
 * Interface to create the various services for use when communicating with the
 * Ingest server
 *
 * @author shake
 */
interface ServiceGenerator {

    /**
     * Create a [BagService] for querying Bags in the Ingest Server
     *
     * @return a new BagService
     */
    fun bags(): BagService

    /**
     * Create a [FileService] for querying Bags in the Ingest Server
     *
     * @return a new BagService
     */
    fun files(): FileService

    /**
     * Create a [TokenService] for querying Bag Tokens in the Ingest Server
     *
     * @return a new TokenService
     */
    fun tokens(): TokenService

    /**
     * Create a [RepairService] for querying Repairs in the Ingest Server
     *
     * @return a new RepairService
     */
    fun repairs(): RepairService

    /**
     * Create a [StagingService] for querying Bag Staging information in the Ingest Server
     *
     * @return a new StagingService
     */
    fun staging(): StagingService

    /**
     * Create a [DepositorService] for querying Depositors in the Ingest Server
     *
     * @return a new DepositorService
     */
    fun depositors(): DepositorService

    /**
     * Create a [StorageService] for querying Storage Regions in the Ingest Server
     *
     * @return a new StorageService
     */
    fun storage(): StorageService

    /**
     * Create a [ReplicationService] for querying Replications the Ingest Server
     *
     * @return a new ReplicationService
     */
    fun replications(): ReplicationService

}
