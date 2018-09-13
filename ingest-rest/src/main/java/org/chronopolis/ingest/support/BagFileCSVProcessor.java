package org.chronopolis.ingest.support;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.function.BiFunction;

/**
 * Processor for ingesting {@link BagFile}s from CSV files containing what is essentially a mapping
 * to a FileCreate.
 * <p>
 * Headers are defined in the rest-models module and must match the same ordering as their ordinals
 * i.e. filename, size, fixity_value, fixity_algorithm.
 *
 * @author shake
 */
public class BagFileCSVProcessor implements BiFunction<Long, Path, ResponseEntity> {

    private final Logger log = LoggerFactory.getLogger(BagFileCSVProcessor.class);

    public enum Headers {
        FILENAME, SIZE, FIXITY_VALUE, FIXITY_ALGORITHM
    }

    private final PagedDAO dao;
    private final IngestProperties properties;

    public BagFileCSVProcessor(PagedDAO dao, IngestProperties properties) {
        this.dao = dao;
        this.properties = properties;
    }

    @Override
    public ResponseEntity apply(Long bagId, Path csvIn) {
        ResponseEntity response = ResponseEntity.ok().build();
        final String LS = "/"; // leading slash
        try {
            int num = 0;
            Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(bagId));

            CSVParser parse = CSVFormat.RFC4180
                    .withHeader(Headers.class)
                    .withIgnoreEmptyLines()
                    .withIgnoreHeaderCase()
                    .withSkipHeaderRecord()
                    .withIgnoreSurroundingSpaces()
                    .parse(Files.newBufferedReader(csvIn));
            log.info("[{}] Attempting ingest of BagFiles from csv", bag.getName());
            for (CSVRecord record : parse.getRecords()) {
                // how to handle inconsistent records?
                if (record.isConsistent()) {
                    // coerce leading /
                    String filename = record.get(Headers.FILENAME).startsWith(LS)
                            ? record.get(Headers.FILENAME)
                            : LS + record.get(Headers.FILENAME);
                    String sizeRecord = record.get(Headers.SIZE);
                    String fixity = record.get(Headers.FIXITY_VALUE);
                    String algorithmRecord = record.get(Headers.FIXITY_ALGORITHM);

                    // I think these can error
                    Long size = Long.parseLong(sizeRecord);
                    FixityAlgorithm algorithm = FixityAlgorithm.Companion.fromString(algorithmRecord);

                    // Not sure what type of impact this will have
                    // Since most records should not exist, hopefully very little
                    BagFile bagFile = dao.findOne(QBagFile.bagFile,
                            QBagFile.bagFile.filename.eq(filename)
                                    .and(QBagFile.bagFile.bag.id.eq(bagId)));

                    if (bagFile == null) {
                        log.debug("Inserting entity for {}", filename);
                        bagFile = new BagFile();
                        bagFile.setBag(bag);
                        bagFile.setSize(size);
                        bagFile.setFilename(filename);

                        String canonical = algorithm.getCanonical();
                        bagFile.addFixity(
                                new Fixity(ZonedDateTime.now(), bagFile, fixity, canonical));

                        bag.addFile(bagFile);
                    } else {
                        checkUpdate(bagFile, fixity, algorithm);
                    }

                    num++;
                }

                // no idea if this batches correctly or not
                // also this _could_ potentially overflow, but we would need 2^31 files to ingest
                // so I think we'll be ok
                if (num % properties.getFileIngestBatchSize() == 0) {
                    dao.save(bag);
                }
            }

            // catch remaining entities
            dao.save(bag);
        } catch (NumberFormatException e) {
            log.error("Invalid number in csv: ", e);

            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Previously validated csv found to be invalid");
        } catch (IOException e) {
            log.error("Unable to parse csv", e);
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to read csv");
        }

        return response;
    }

    /**
     * Check an existing {@link BagFile} to see if a new {@link Fixity} entity should be created
     *
     * @param bagFile   the {@link BagFile} to check
     * @param fixity    the string representation of the {@link Fixity#value}
     * @param algorithm the type of {@link FixityAlgorithm} to persist
     */
    private void checkUpdate(BagFile bagFile, String fixity, FixityAlgorithm algorithm) {
        boolean containsFixity = bagFile.getFixities()
                .stream()
                .anyMatch(pFixity -> pFixity.getAlgorithm().equals(algorithm.getCanonical()));

        if (!containsFixity) {
            bagFile.addFixity(
                    new Fixity(ZonedDateTime.now(), bagFile, fixity, algorithm.getCanonical()));
        }
    }
}
