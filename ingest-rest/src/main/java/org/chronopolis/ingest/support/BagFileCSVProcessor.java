package org.chronopolis.ingest.support;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.csv.BagFileHeaders;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDataFile;
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
import java.util.ArrayList;
import java.util.List;
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

    private final PagedDao dao;
    private final IngestProperties properties;

    public BagFileCSVProcessor(PagedDao dao, IngestProperties properties) {
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
                    .withHeader(BagFileHeaders.class)
                    .withIgnoreEmptyLines()
                    .withIgnoreHeaderCase()
                    .withSkipHeaderRecord()
                    .withIgnoreSurroundingSpaces()
                    .parse(Files.newBufferedReader(csvIn));
            log.info("[{}] Attempting ingest of BagFiles from csv", bag.getName());
            JPAQueryFactory query = dao.getJPAQueryFactory();
            List<BagFile> files = new ArrayList<>(properties.getFileIngestBatchSize());
            for (CSVRecord record : parse.getRecords()) {
                // how to handle inconsistent records?
                if (record.isConsistent()) {
                    // coerce leading /
                    String filename = record.get(BagFileHeaders.FILENAME).startsWith(LS)
                            ? record.get(BagFileHeaders.FILENAME)
                            : LS + record.get(BagFileHeaders.FILENAME);
                    String sizeRecord = record.get(BagFileHeaders.SIZE);
                    String fixity = record.get(BagFileHeaders.FIXITY_VALUE);
                    String algorithmRecord = record.get(BagFileHeaders.FIXITY_ALGORITHM);

                    // I think these can error
                    Long size = Long.parseLong(sizeRecord);
                    FixityAlgorithm algorithm = FixityAlgorithm.Companion.fromString(algorithmRecord);

                    // Not sure what type of impact this will have
                    // Since most records should not exist, hopefully very little
                    Long count = query.selectFrom(QDataFile.dataFile)
                            .where(QDataFile.dataFile.filename.eq(filename)
                                    .and(QDataFile.dataFile.bag.id.eq(bagId))).fetchCount();
                    BagFile bagFile = null;

                    if (count == 0) {
                        log.debug("Inserting entity for {}", filename);
                        bagFile = new BagFile();
                        bagFile.setBag(bag);
                        bagFile.setSize(size);
                        bagFile.setFilename(filename);

                        String canonical = algorithm.getCanonical();
                        bagFile.addFixity(
                                new Fixity(ZonedDateTime.now(), bagFile, fixity, canonical));

                        files.add(bagFile);
                    } /* update later? maybe 3.1.0
                    else {
                        checkUpdate(bagFile, fixity, algorithm);
                    }*/

                    num++;
                }

                // no idea if this batches correctly or not
                // it doesn't appear to be and becomes very memory heavy
                // also this _could_ potentially overflow, but we would need 2^31 files to ingest
                // so I think we'll be ok
                if (num % properties.getFileIngestBatchSize() == 0) {
                    bag.getFiles().addAll(files);
                    log.info("saving bag");
                    dao.save(bag);
                    files.clear();
                }
            }

            // catch remaining entities
            bag.getFiles().addAll(files);
            dao.save(bag);
            files.clear();
        } catch (NumberFormatException e) {
            log.error("Invalid number in csv: ", e);

            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Previously validated csv found to be invalid");
        } catch (IOException e) {
            log.error("Unable to parse csv", e);
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to read csv");
        } catch (Exception e) {
            log.error("Unexpected exception processing csv!", e);
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected exception processing csv!");
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
    @SuppressWarnings("unused")
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
