package org.chronopolis.replicate.support;


import org.chronopolis.rest.api.BagService;
import org.chronopolis.rest.api.FileService;
import org.chronopolis.rest.api.DepositorService;
import org.chronopolis.rest.api.RepairService;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.api.StagingService;
import org.chronopolis.rest.api.StorageService;
import org.chronopolis.rest.api.TokenService;

/**
 * Class so we don't need to mock ServiceGenerators, but instead just mock the
 * ReplicationService and return that
 *
 * @author shake
 */
public class ReplGenerator implements ServiceGenerator {

    private final ReplicationService replications;

    public ReplGenerator(ReplicationService replications) {
        this.replications = replications;
    }

    @Override
    public BagService bags() {
        return null;
    }

    @Override
    public FileService files() {
        return null;
    }

    @Override
    public TokenService tokens() {
        return null;
    }

    @Override
    public RepairService repairs() {
        return null;
    }

    @Override
    public StagingService staging() {
        return null;
    }

    @Override
    public DepositorService depositors() {
        return null;
    }

    @Override
    public StorageService storage() {
        return null;
    }

    @Override
    public ReplicationService replications() {
        return replications;
    }
}
