package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.DataFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.QTokenStore;
import org.chronopolis.rest.entities.storage.QFixity;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.create.StagingCreate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.persistence.EntityManager;
import java.security.Principal;
import java.util.Optional;

/**
 * Data access object giving a few convenience methods for {@link StagingStorage} resources
 *
 * @author shake
 */
public class StagingDao extends PagedDAO {
    // I don't know where to define these, should be looked at before the release
    // Might be nice to put in a sealed class
    public static final String DISCRIMINATOR_BAG = "BAG";
    public static final String DISCRIMINATOR_TOKEN = "TOKEN_STORE";

    public StagingDao(EntityManager em) {
        super(em);
    }

    /**
     * Get the active storage object for a bag
     *
     * @param bag           the bag whose staging_storage we're retrieving
     * @param discriminator the discriminator to join on
     * @return the StagingStorage entity, if found
     */
    public Optional<StagingStorage> activeStorageForBag(Bag bag, String discriminator) {
        return activeStorageForBag(bag.getId(), discriminator);
    }

    /**
     * Get the active storage object for a bag given its id
     *
     * @param bag           the id of the bag
     * @param discriminator the discriminator to join on
     * @return the StagingStorage entity, if found
     */
    public Optional<StagingStorage> activeStorageForBag(Long bag, String discriminator) {
        JPAQueryFactory factory = getJPAQueryFactory();
        QBag b = QBag.bag;
        QStagingStorage storage = QStagingStorage.stagingStorage;
        // once again not the best query but that's ok
        // todo: what if there is no file for a given staging storage?
        return Optional.ofNullable(factory.from(b)
                .innerJoin(b.storage, storage)
                .where(storage.active.isTrue()
                        .and(storage.file.dtype.eq(discriminator)
                                .and(storage.bag.id.eq(bag))))
                .select(storage)
                .fetchOne());
    }

    /**
     * Remove a fixity value for a given StagingStorage entity
     *
     * @param fixityId the id of the fixity to remove
     */
    public void deleteFixity(Long fixityId) {
        JPAQueryFactory factory = getJPAQueryFactory();
        QFixity fixity = QFixity.fixity;
        factory.delete(fixity)
                .where(fixity.id.eq(fixityId))
                .execute();
    }

    /**
     * Implementation for creation of a {@link StagingStorage} resource given a few parameters
     * <p>
     * todo: some updates from the file-api branch will be useful here
     * todo: validate against the node in the storage region eventually
     * <p>
     * Some restrictions on the request:
     * - user must be able to edit the Bag
     * - the type must correspond to a known discriminator
     * - the validation file must exist
     * - no other active storage must exist
     * - i like big butts and i cannot lie
     * - that's about it I guess
     *
     * @param principal the security principal of the user
     * @param id        the id of the {@link Bag}
     * @param type      the type of StagingStorage to create
     * @param create    the values to use when creating the staging resource
     * @return who put the screw in the tuna
     */
    public ResponseEntity<StagingStorage> createStaging(Principal principal,
                                                        Long id,
                                                        String type,
                                                        StagingCreate create) {
        JPAQueryFactory qf = getJPAQueryFactory();

        StorageRegion region = qf.selectFrom(QStorageRegion.storageRegion)
                .where(QStorageRegion.storageRegion.id.eq(create.getStorageRegion()))
                .fetchOne();

        Bag bag = qf.selectFrom(QBag.bag)
                .where(QBag.bag.id.eq(id))
                .fetchOne();

        Optional<StagingStorage> existing = activeStorageForBag(id, type);

        if (bag == null || region == null) {
            return ResponseEntity.badRequest().build();
        } else if (!authorized(principal, bag)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } else if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Optional<DataFile> file = fetchFile(id, type, create.getValidationFile(), qf);
        return file.map(dataFile -> {
            StagingStorage theStorage = new StagingStorage(region, bag,
                    create.getSize(),
                    create.getTotalFiles(),
                    create.getLocation(),
                    true);
            theStorage.setFile(dataFile);
            save(theStorage);
            return ResponseEntity.status(HttpStatus.CREATED).body(theStorage);
        }).orElse(ResponseEntity.badRequest().build());
    }

    private Optional<DataFile> fetchFile(Long id, String type, String validationFile, JPAQueryFactory qf) {
        Optional<DataFile> file;

        DataFile fetch = null;
        if (DISCRIMINATOR_BAG.equalsIgnoreCase(type)) {
            fetch = qf.selectFrom(QBagFile.bagFile)
                    .where(QBagFile.bagFile.bag.id.eq(id)
                            .and(QBagFile.bagFile.filename.eq(validationFile)))
                    .fetchOne();
        } else if (DISCRIMINATOR_TOKEN.equalsIgnoreCase(type)) {
            fetch = qf.selectFrom(QTokenStore.tokenStore)
                    .where(QTokenStore.tokenStore.bag.id.eq(id)
                            .and(QTokenStore.tokenStore.filename.eq(validationFile)))
                    .fetchOne();
        }

        file = Optional.ofNullable(fetch);
        return file;
    }
}
