package org.chronopolis.ingest.api;

import com.google.common.primitives.Longs;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.ingest.support.BagFileCSVProcessor;
import org.chronopolis.ingest.support.BagFileCSVProcessor.Headers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Separate controller for FileUploads to keep the logic separated a bit
 *
 * @author shake
 */
@RestController
public class BatchFileController {

    private final String TYPE_CSV = "text/csv";
    private final Logger log = LoggerFactory.getLogger(BatchFileController.class);

    private final PagedDAO dao;
    private final BagFileCSVProcessor processor;
    private final ConcurrentSkipListSet<Long> processing;

    @Autowired
    public BatchFileController(PagedDAO dao, BagFileCSVProcessor processor) {
        this.dao = dao;
        this.processor = processor;
        this.processing = new ConcurrentSkipListSet<>();
    }

    protected BatchFileController(PagedDAO dao,
                                  BagFileCSVProcessor processor,
                                  ConcurrentSkipListSet<Long> processing) {
        this.dao = dao;
        this.processor = processor;
        this.processing = processing;
    }

    /**
     * Create many files for a Bag
     * <p>
     * Need to determine
     * - output: are our responses ok? should we do something other than an ok on success?
     * - async: should we use DeferredResponse or other async utils?
     *
     * <p>
     * Can we even do batch processing of inserts right now? I think spring will prevent us unless
     * we do things only through it.
     *
     * @param bagId the id of the {@link Bag} to create {@link BagFile}s for
     */
    @PostMapping("/api/bags/{bag_id}/files")
    public ResponseEntity createBagFiles(Principal principal,
                                         @PathVariable("bag_id") Long bagId,
                                         @RequestParam("file") MultipartFile file) {
        boolean valid = true;
        ResponseEntity entity = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        // a few things we check early, to avoid unnecessary processing
        // bag exists
        Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(bagId));
        if (bag == null) {
            log.error("Invalid bag id specified: {}", bagId);
            return entity;
        }

        // authorized to update the bag
        if (!dao.authorized(principal, bag)) {
            log.error("User {} is not allowed to update bag {}", principal.getName(), bag.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // upload file type
        final String uuid = UUID.randomUUID().toString();
        boolean csv = TYPE_CSV.equals(file.getContentType());
        boolean plainText = MediaType.TEXT_PLAIN_VALUE.equals(file.getContentType());
        boolean correctType = csv || plainText;
        if (file.isEmpty() || !correctType) {
            // hey add something to the ResponseEntity too
            // log + return
            log.error("Invalid upload: Check if the file is empty? {} or content type {}",
                    file.isEmpty(), file.getContentType());
            return entity;
        }

        // finally check if we're already processing said bag (last bc we only need to remove once)
        boolean add = processing.add(bagId);
        if (!add) {
            log.warn("Bag {} is already being processed for batch file ingestion", bagId);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // need a way to get our temp resource
        Path tmpCsv = Paths.get("/tmp/", uuid + ".csv");
        // at the moment this is rather naive but we generally don't deal with files that large
        // e.g. 100k files is ~15MiB
        // we can look at optimizing later if we really need it, but reading and writing seems good
        // enough atm
        try (BufferedWriter writer = Files.newBufferedWriter(tmpCsv,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            InputStreamReader isr;
            BufferedReader reader;

            isr = new InputStreamReader(file.getInputStream());
            reader = new BufferedReader(isr);
            CSVParser parser = CSVFormat.RFC4180
                    .withHeader(Headers.class)
                    .withIgnoreHeaderCase()
                    .withIgnoreEmptyLines()
                    .withIgnoreSurroundingSpaces()
                    .parse(reader);
            List<CSVRecord> records = parser.getRecords();
            for (CSVRecord record : records) {
                // check that the record is consistent
                if (!record.isConsistent()) {
                    valid = false;
                    long number = record.getRecordNumber();
                    log.error("Uploaded csv is not consistent! Error at line {}", number);
                    // might turn this into json
                    entity = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Inconsistent csv! Error at line " + number);

                    break;
                }

                // check that the record is valid within our own context
                if (record.getRecordNumber() > 1 && !validate(record)) {
                    valid = false;
                    long number = record.getRecordNumber();
                    log.error("Unable to validate record {}", number);
                    entity = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Invalid record at line " + number);
                    break;
                }

                writer.write(record.get(Headers.FILENAME.ordinal()));
                writer.write(",");
                writer.write(record.get(Headers.SIZE.ordinal()));
                writer.write(",");
                writer.write(record.get(Headers.FIXITY_VALUE.ordinal()));
                writer.write(",");
                writer.write(record.get(Headers.FIXITY_ALGORITHM.ordinal()));
                writer.newLine();
            }
        } catch (IOException e) {
            log.error("Error with parser or writer", e);
            entity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (valid) {
                entity = processor.apply(bagId, tmpCsv);
            }

            processing.remove(bagId);
            deleteCsv(tmpCsv);
        }

        return entity;
    }

    private boolean validate(CSVRecord record) {
        String size = record.get(Headers.SIZE);
        String algorithm = record.get(Headers.FIXITY_ALGORITHM);

        boolean isNumber = Longs.tryParse(size) != null;
        FixityAlgorithm parsedAlgorithm = FixityAlgorithm.Companion.fromString(algorithm);
        return isNumber && !parsedAlgorithm.equals(FixityAlgorithm.UNSUPPORTED);
    }

    private void deleteCsv(Path tmpCsv) {
        try {
            Files.delete(tmpCsv);
        } catch (IOException e) {
            log.error("Unable to remove csv!", tmpCsv);
        }
    }

}
