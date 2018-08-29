package org.chronopolis.ingest.api;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.models.filter.BagFileFilter;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.DataFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.QDataFile;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.QFixity;
import org.chronopolis.rest.models.create.FileCreate;
import org.chronopolis.rest.models.create.FixityCreate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Set;

/**
 * Controller defining various operations for {@link BagFile}s in Chronopolis
 *
 * @author shake
 */
@RestController
public class FileController {

    private final PagedDAO dao;

    @Autowired
    public FileController(PagedDAO dao) {
        this.dao = dao;
    }

    /**
     * Retrieve a paginated list of all {@link BagFile}s
     * <p>
     * todo: data file?
     *
     * @param filter QueryParameters to be used when searching
     * @return a paginated list of results
     */
    @GetMapping("/api/files")
    public ResponseEntity<Iterable<BagFile>> getFiles(@ModelAttribute BagFileFilter filter) {
        return ResponseEntity.ok(dao.findPage(QBagFile.bagFile, filter));
    }

    /**
     * Retrieve a paginated list of {@link BagFile}s for a given {@link Bag} identified by its id
     * <p>
     * todo: 404 if the bag does not exist?
     *
     * @param bagId  the id of the {@link Bag} to query on
     * @param filter other QueryParameters for filtering the results
     * @return a paginated list of results
     */
    @GetMapping("/api/bags/{bag_id}/files")
    public ResponseEntity<Iterable<BagFile>> getBagFiles(@PathVariable("bag_id") Long bagId,
                                                         @ModelAttribute BagFileFilter filter) {
        filter.setBag(bagId);
        return ResponseEntity.ok(dao.findPage(QBagFile.bagFile, filter));
    }

    /**
     * Retrieve a single {@link BagFile}
     *
     * @param fileId the id of the {@link BagFile} to query
     * @return HTTP 200 if the {@link BagFile} is found
     *         HTTP 404 if the {@link BagFile} is not found
     */
    @GetMapping("/api/files/{file_id}")
    public ResponseEntity<DataFile> getFile(@PathVariable("file_id") Long fileId) {
        ResponseEntity<DataFile> response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        DataFile file = dao.findOne(QDataFile.dataFile, QDataFile.dataFile.id.eq(fileId));
        if (file != null) {
            response = ResponseEntity.ok(file);
        }

        return response;
    }

    /**
     * Retrieve an individual {@link BagFile} identified by its id and its associated {@link Bag}
     *
     * @param bagId  the id of the {@link Bag} to query on
     * @param fileId the id of the {@link BagFile} to query on
     * @return HTTP 200 if the {@link BagFile} is found
     *         HTTP 401 if the user is not authenticated
     *         HTTP 404 if either the {@link Bag} or {@link BagFile} does not exist
     */
    @GetMapping("/api/bags/{bag_id}/files/{file_id}")
    public ResponseEntity<BagFile> getBagFile(@PathVariable("bag_id") Long bagId,
                                              @PathVariable("file_id") Long fileId) {
        ResponseEntity<BagFile> response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        BagFile file = dao.findOne(QBagFile.bagFile,
                QBagFile.bagFile.bag.id.eq(bagId).and(QBagFile.bagFile.id.eq(fileId)));

        if (file != null) {
            response = ResponseEntity.ok(file);
        }

        return response;
    }

    /**
     * Create a single {@link BagFile} for a {@link Bag}
     *
     * @param bagId  the id of the {@link Bag}
     * @param create the request body
     * @return HTTP 201 with the {@link BagFile} if successful
     *         HTTP 400 if the {@link Bag} does not exist or the {@link FileCreate} is invalid
     *         HTTP 401 if the user is not authenticated
     *         HTTP 403 if the user is not allowed to update the resource (todo)
     *         HTTP 409 if the {@link BagFile} already exists (todo)
     */
    @PostMapping("/api/bags/{bag_id}/file")
    public ResponseEntity<BagFile> createBagFile(@PathVariable("bag_id") Long bagId,
                                                 @RequestBody FileCreate create) {
        ResponseEntity<BagFile> response = ResponseEntity.badRequest().build();
        Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(bagId));
        if (bag != null) {
            Fixity fixity = new Fixity(ZonedDateTime.now(),
                    create.getFixity(),
                    create.getFixityAlgorithm().getCanonical());

            BagFile file = new BagFile();
            file.setBag(bag);
            file.setSize(create.getSize());
            file.getFixities().add(fixity);
            file.setFilename(create.getFilename());

            bag.addFile(file);
            dao.save(bag);

            response = ResponseEntity.status(HttpStatus.CREATED).body(file);
        }

        return response;
    }



    @GetMapping("/api/bags/{bag_id}/files/{file_id}/fixity")
    public ResponseEntity<Set<Fixity>> getBagFileFixities(@PathVariable("bag_id") Long bagId,
                                                          @PathVariable("file_id") Long fileId) {
        ResponseEntity<Set<Fixity>> response =
                ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        BagFile file = dao.findOne(QBagFile.bagFile,
                QBagFile.bagFile.id.eq(fileId).and(QBagFile.bagFile.bag.id.eq(bagId)));
        if (file != null) {
            response = ResponseEntity.ok(file.getFixities());
        }

        return response;
    }

    @GetMapping("/api/bags/{bag_id}/files/{file_id}/fixity/{algorithm}")
    public ResponseEntity<Fixity> getBagFileFixity(@PathVariable("bag_id") Long bagId,
                                                   @PathVariable("file_id") Long fileId,
                                                   @PathVariable("algorithm") String algorithm) {
        ResponseEntity<Fixity> response =
                ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        // join instead
        // push to dao? testing here will be painful
        Fixity fixity = fixityFor(bagId, fileId, algorithm);
        if (fixity != null) {
            response = ResponseEntity.ok(fixity);
        }

        return response;
    }

    /**
     * Create a Fixity associated with a File
     * <p>
     * todo: http 403
     *
     * @param bagId  the id of the bag
     * @param fileId the id of the file
     * @param create the values of the Fixity to create
     * @return HTTP 201 if the FixityCreate was successful
     *         HTTP 400 if the bag or file was not found
     *         HTTP 401 if the user was not authenticated
     *         HTTP 409 if a Fixity already exists for the given algorithm
     */
    @PutMapping("/api/bags/{bag_id}/files/{file_id}/fixity")
    public ResponseEntity<Fixity> createBagFileFixity(@PathVariable("bag_id") Long bagId,
                                                      @PathVariable("file_id") Long fileId,
                                                      @RequestBody FixityCreate create) {
        ResponseEntity<Fixity> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        String algorithm = create.getAlgorithm().getCanonical();
        BagFile file = dao.findOne(QBagFile.bagFile,
                QBagFile.bagFile.id.eq(fileId).and(QBagFile.bagFile.bag.id.eq(bagId)));

        Fixity stored = fixityFor(bagId, fileId, algorithm);

        if (stored != null) {
            response = ResponseEntity.status(HttpStatus.CONFLICT).build();
        } else if (file != null) {
            Fixity fixity = new Fixity(ZonedDateTime.now(), create.getValue(), algorithm);
            file.getFixities().add(fixity);
            dao.save(file);
            response = ResponseEntity.status(HttpStatus.CREATED).body(fixity);
        }

        return response;
    }

    private Fixity fixityFor(Long bagId, Long fileId, String algorithm) {
        JPAQueryFactory query = dao.getJPAQueryFactory();
        return query.select(QFixity.fixity)
                .from(QBagFile.bagFile)
                .join(QBagFile.bagFile.fixities, QFixity.fixity)
                .where(QBagFile.bagFile.bag.id.eq(bagId)
                        .and(QBagFile.bagFile.id.eq(fileId)
                                .and(QFixity.fixity.algorithm.eq(algorithm))))
                .fetchOne();
    }

}
