package org.chronopolis.ingest.api;

import org.chronopolis.ingest.models.filter.BagFileFilter;
import org.chronopolis.ingest.repository.dao.DataFileDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.DataFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.QDataFile;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.models.create.FileCreate;
import org.chronopolis.rest.models.create.FixityCreate;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
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

import java.security.Principal;
import java.util.Set;

/**
 * Controller defining various operations for {@link BagFile}s in Chronopolis
 *
 * todo: how to do datafiles?
 *
 * @author shake
 */
@RestController
public class FileController {

    private final DataFileDao dao;

    @Autowired
    public FileController(DataFileDao dataFileDao) {
        this.dao = dataFileDao;
    }

    /**
     * Retrieve a paginated list of all {@link BagFile}s
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
     *
     * @param bagId  the id of the {@link Bag} to query on
     * @param filter other QueryParameters for filtering the results
     * @return HTTP 200 with a paginated list of results
     *         HTTP 400 if the bag_id does not correlate to a known Bag
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
     *         HTTP 400 if the {@link Bag} specified by the bag_id does not exist
     *         HTTP 401 if the user is not authenticated
     *         HTTP 404 if the {@link BagFile} does not exist
     */
    @GetMapping("/api/bags/{bag_id}/files/{file_id}")
    public ResponseEntity<BagFile> getBagFile(@PathVariable("bag_id") Long bagId,
                                              @PathVariable("file_id") Long fileId) {
        ResponseEntity<BagFile> response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        // is this the best way?
        Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(bagId));
        BagFile file = dao.findOne(QBagFile.bagFile,
                QBagFile.bagFile.bag.id.eq(bagId).and(QBagFile.bagFile.id.eq(fileId)));

        if (bag == null) {
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } else if (file != null) {
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
     *         HTTP 403 if the user is not allowed to update the resource
     *         HTTP 409 if the {@link BagFile} already exists
     */
    @PostMapping("/api/bags/{bag_id}/file")
    public ResponseEntity<BagFile> createBagFile(Principal principal,
                                                 @PathVariable("bag_id") Long bagId,
                                                 @RequestBody FileCreate create) {
        create.setBag(bagId);
        return dao.createBagFile(principal, create).response();
    }


    /**
     * Get all {@link Fixity}s for a {@link BagFile}
     *
     * @param bagId  the id of the {@link Bag}
     * @param fileId the id of the {@link BagFile}
     * @return HTTP 200 with a view of all {@link Fixity} for a {@link BagFile}
     *         HTTP 400 if the {@link Bag} or {@link BagFile} ids specified do not exist
     */
    @GetMapping("/api/bags/{bag_id}/files/{file_id}/fixity")
    public ResponseEntity<Set<Fixity>> getBagFileFixities(@PathVariable("bag_id") Long bagId,
                                                          @PathVariable("file_id") Long fileId) {
        ResponseEntity<Set<Fixity>> response;
        Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(bagId));
        BagFile file = dao.findOne(QBagFile.bagFile,
                QBagFile.bagFile.id.eq(fileId).and(QBagFile.bagFile.bag.id.eq(bagId)));
        if (bag == null || file == null) {
            response = ResponseEntity.badRequest().build();
        } else {
            response = ResponseEntity.ok(file.getFixities());
        }

        return response;
    }

    /**
     * Get a single {@link Fixity}s for a {@link BagFile} specified by a {@code algorithm}
     *
     * @param bagId     the id of the {@link Bag}
     * @param fileId    the id of the {@link BagFile}
     * @param algorithm the name of the {@link FixityAlgorithm} to retrieve
     * @return HTTP 200 with the {@link Fixity} specified
     *         HTTP 400 if the {@link Bag} or {@link BagFile} do not exist
     *         HTTP 404 if no {@link Fixity} exists for the specified {@code algorithm}
     */
    @GetMapping("/api/bags/{bag_id}/files/{file_id}/fixity/{algorithm}")
    public ResponseEntity<Fixity> getBagFileFixity(@PathVariable("bag_id") Long bagId,
                                                   @PathVariable("file_id") Long fileId,
                                                   @PathVariable("algorithm") String algorithm) {
        return dao.fixityFor(bagId, fileId, algorithm).response();
    }

    /**
     * Create a Fixity associated with a File
     *
     * @param bagId  the id of the bag
     * @param fileId the id of the file
     * @param create the values of the Fixity to create
     * @return HTTP 201 if the FixityCreate was successful
     *         HTTP 400 if the bag or file was not found
     *         HTTP 401 if the user was not authenticated
     *         HTTP 403 if the user is now allowed to update the Bag
     *         HTTP 409 if a Fixity already exists for the given algorithm
     */
    @PutMapping("/api/bags/{bag_id}/files/{file_id}/fixity")
    public ResponseEntity<Fixity> createBagFileFixity(Principal principal,
                                                      @PathVariable("bag_id") Long bagId,
                                                      @PathVariable("file_id") Long fileId,
                                                      @RequestBody FixityCreate create) {
        return dao.createFixity(principal, bagId, fileId, create).response();
    }

}
