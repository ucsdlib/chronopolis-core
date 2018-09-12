package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.support.QueryResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.QDataFile;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.QFixity;
import org.chronopolis.rest.models.create.FileCreate;
import org.chronopolis.rest.models.create.FixityCreate;

import javax.persistence.EntityManager;
import java.security.Principal;
import java.time.ZonedDateTime;

/**
 * Just a test for now but will probably be committed an used
 *
 * @author shake
 */
public class DataFileDao extends PagedDAO {

    public DataFileDao(EntityManager em) {
        super(em);
    }

    /**
     * Create a {@link BagFile} for a {@link Bag} from a {@link FileCreate} request
     *
     * @param principal the security principal of the user
     * @param create    the create request
     * @return A {@link QueryResult} encapsulating the status of the operation
     */
    public QueryResult<BagFile> createBagFile(Principal principal, FileCreate create) {
        Long bagId = create.getBag();
        if (bagId == null) {
            return new QueryResult<>(QueryResult.Status.BAD_REQUEST, "bag_id cannot be null!");
        }

        QueryResult<BagFile> result;
        Bag bag = findOne(QBag.bag, QBag.bag.id.eq(bagId));

        JPAQueryFactory queryFactory = getJPAQueryFactory();
        Long count = queryFactory.from(QDataFile.dataFile)
                .select(QDataFile.dataFile.id)
                .where(QDataFile.dataFile.bag.id.eq(bagId)
                        .and(QDataFile.dataFile.filename.eq(create.getFilename())))
                .fetchCount();

        // bag dne -> bad request
        // not authorized -> forbidden
        // file is not null -> conflict
        // otherwise -> ok, try to create
        if (bag == null) {
            result = new QueryResult<>(QueryResult.Status.BAD_REQUEST, "Bag cannot be null!");
        } else if (!authorized(principal, bag)) {
            result = new QueryResult<>(QueryResult.Status.FORBIDDEN,
                    "User cannot update this resource!");
        } else if (count > 0) {
            result = new QueryResult<>(QueryResult.Status.CONFLICT,
                    "File with name " + create.getFilename() + " already exists!");
        } else {
            BagFile file = new BagFile();
            file.setBag(bag);
            file.setSize(create.getSize());
            file.setFilename(create.getFilename());
            file.addFixity(new Fixity(ZonedDateTime.now(),
                    file,
                    create.getFixity(),
                    create.getFixityAlgorithm().getCanonical()));

            bag.addFile(file);
            save(bag);
            result = new QueryResult<>(file);
        }

        return result;
    }

    /**
     * Create a {@link Fixity} for a {@link BagFile} from a {@link FixityCreate} request
     *
     * @param principal the security principal of the use
     * @param bagId     the id of the {@link Bag}, used for validation of the request
     * @param fileId    the id of the {@link BagFile} to associate the {@link Fixity} with
     * @param create    the values of the {@link Fixity} to create
     * @return A {@link QueryResult} encapsulating the result of the operation
     */
    public QueryResult<Fixity> createFixity(Principal principal,
                                            Long bagId,
                                            Long fileId,
                                            FixityCreate create) {
        QueryResult<Fixity> result;
        String algorithm = create.getAlgorithm().getCanonical();

        // Need to check bag not null
        Bag bag = findOne(QBag.bag, QBag.bag.id.eq(bagId));
        BagFile file = findOne(QBagFile.bagFile,
                QBagFile.bagFile.id.eq(fileId).and(QBagFile.bagFile.bag.id.eq(bagId)));

        Fixity stored = justQuery(bagId, fileId, algorithm);

        if (bag == null || file == null) {
            result = new QueryResult<>(QueryResult.Status.BAD_REQUEST,
                    "Bag and BagFile are not allowed to be null!");
        } else if (!authorized(principal, bag)) {
            result = new QueryResult<>(QueryResult.Status.FORBIDDEN,
                    "User is not allowed to update this resource!");
        } else if (stored != null) {
            result = new QueryResult<>(QueryResult.Status.CONFLICT,
                    "Fixity already exists for algorithm!");
        } else {
            Fixity fixity = new Fixity(ZonedDateTime.now(), file, create.getValue(), algorithm);
            file.addFixity(fixity);
            save(file);
            result = new QueryResult<>(fixity);
        }

        return result;
    }

    /**
     * Retrieve the {@link Fixity} for a given bag, file, and algorithm
     *
     * @param bag       the id of the {@link Bag}, used for validation of the request
     * @param file      the id of the {@link BagFile} which the {@link Fixity} belongs to
     * @param algorithm the algorithm of the {@link Fixity} to query on
     * @return A {@link QueryResult} with the result of the database query
     */
    public QueryResult<Fixity> fixityFor(Long bag, Long file, String algorithm) {
        QueryResult<Fixity> result;
        JPAQueryFactory query = getJPAQueryFactory();

        long bagExists = query.from(QBag.bag)
                .where(QBag.bag.id.eq(bag))
                .fetchCount();
        long fileExists = query.from(QBagFile.bagFile)
                .where(QBagFile.bagFile.bag.id.eq(bag).and(QBagFile.bagFile.id.eq(file)))
                .fetchCount();

        if (bagExists == 0 || fileExists == 0) {
            result = new QueryResult<>(QueryResult.Status.BAD_REQUEST, "Bag and File must exist!");
        } else {
            Fixity fixity = justQuery(bag, file, algorithm);
            result = fixity != null
                    ? new QueryResult<>(fixity, QueryResult.Status.OK)
                    : new QueryResult<>(QueryResult.Status.NOT_FOUND,
                    "Fixity not found for algorithm " + algorithm);
        }

        return result;
    }

    /**
     * Just the query
     *
     * @param bag       the id of the Bag
     * @param file      the id of the DataFile
     * @param algorithm the name of the algorithm
     * @return the {@link Fixity}, if found
     */
    private Fixity justQuery(Long bag, Long file, String algorithm) {
        JPAQueryFactory query = getJPAQueryFactory();
        return query.select(QFixity.fixity)
                .from(QBagFile.bagFile)
                .join(QBagFile.bagFile.fixities, QFixity.fixity)
                .where(QBagFile.bagFile.bag.id.eq(bag)
                        .and(QBagFile.bagFile.id.eq(file)
                                .and(QFixity.fixity.algorithm.eq(algorithm))))
                .fetchOne();
    }
}
