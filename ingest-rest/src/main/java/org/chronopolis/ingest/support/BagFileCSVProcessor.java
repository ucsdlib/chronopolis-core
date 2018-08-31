package org.chronopolis.ingest.support;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
 * Processor for CSV files containing FileCreate elementals
 * <p>
 * This might be good to have somewhere else so that other services can make use of the headers.
 * Maybe provide utils for reading and writing idk.
 *
 * @author shake
 */
public class BagFileCSVProcessor implements BiFunction<Long, Path, ResponseEntity> {

    private final Logger log = LoggerFactory.getLogger(BagFileCSVProcessor.class);

    public enum Headers {
        FILENAME, SIZE, FIXITY_VALUE, FIXITY_ALGORITHM
    }

    private final PagedDAO dao;

    public BagFileCSVProcessor(PagedDAO dao) {
        this.dao = dao;
    }

    @Override
    public ResponseEntity apply(Long bagId, Path csvIn) {
        ResponseEntity response = ResponseEntity.ok().build();
        // leading slash
        final String LS = "/";
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
            log.info("Processing all records");
            for (CSVRecord record : parse.getRecords()) {
                // how to handle inconsistent records?
                if (record.isConsistent()) {
                    log.trace("Processing {}", record.toString());
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
                        log.trace("Inserting entity for {}", filename);
                        bagFile = new BagFile();
                        bagFile.setBag(bag);
                        bagFile.setSize(size);
                        bagFile.setFilename(filename);

                        Fixity fixityEnt =
                                new Fixity(ZonedDateTime.now(), fixity, algorithm.getCanonical());
                        bagFile.getFixities().add(fixityEnt);

                        bag.addFile(bagFile);
                    } else {
                        checkUpdate(bagFile, fixity, algorithm);
                    }

                    num++;
                }

                // no idea if this batches correctly or not
                // it would be nice to be able to flush and clear here too
                if (num % 20 == 0) {
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
            // ouch
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
            Fixity fixityEnt =
                    new Fixity(ZonedDateTime.now(), fixity, algorithm.getCanonical());
            bagFile.getFixities().add(fixityEnt);
        }
    }
}
