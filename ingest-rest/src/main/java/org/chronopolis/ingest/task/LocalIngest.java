package org.chronopolis.ingest.task;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.repository.dao.BagFileDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.QDataFile;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.google.common.io.Files.asByteSource;
import static org.chronopolis.rest.models.enums.FixityAlgorithm.SHA_256;

/**
 * Ingestion steps for Bags registered by an ad-hoc process
 * <p>
 * Currently this will:
 * - scan for a bag using ${depositor.namespace}, ${bag.name}
 * - scan for files in a bag using ${path}/manifest-sha256.txt and ${path}/tagmanifest-sha256.txt
 * - register a {@link StagingStorage} entity if all files have been registered for a Bag and the
 * Bag status is {@code BagStatus.DEPOSITED}
 * - set a {@link Bag} status to {@code BagStatus.INITIALIZED} if it has not been set yet
 *
 * @author shake
 */
@Component
@Transactional
@EnableScheduling
@ConditionalOnProperty(prefix = "ingest", name = "scan.enabled", havingValue = "true")
public class LocalIngest {

    private final Logger log = LoggerFactory.getLogger(LocalIngest.class);

    private final BagFileDao dao;
    private final IngestProperties properties;

    private static final String MANIFEST_NAME = "manifest-";
    private static final String TAGMANIFEST_NAME = "tagmanifest-";

    public LocalIngest(BagFileDao dao, IngestProperties properties) {
        this.dao = dao;
        this.properties = properties;
    }

    /**
     * Entry point for operations involved with query for {@link Bag}s which need tasks run in
     * order to be distributed throughout Chronopolis.
     * <p>
     * Preconditions for the {@link Bag}s are that they have
     * {@code Bag.getStatus() == BagStatus.DEPOSITED}.
     * <p>
     * If a {@link Bag} has fewer than its  {@code Bag.getTotalFiles()} registered, it will be
     * scanned for on disk and have its manifests read in order to register files. Otherwise, it
     * will have a {@link StagingStorage} created when it has all {@link BagFile}s registered.
     */
    @Scheduled(cron = "0 0/1 * * * *")
    public void scan() {
        IngestProperties.Scan scanProperties = properties.getScan();
        if (!scanProperties.getEnabled()) {
            return;
        }

        Posix staging = scanProperties.getStaging();
        Long regionId = staging.getId();
        String username = scanProperties.getUsername();

        StorageRegion region = dao.findOne(QStorageRegion.storageRegion,
                QStorageRegion.storageRegion.id.eq(regionId));

        if (region == null) {
            log.error("Unable to run local ingestion, region (id={}) does not exist", regionId);
            return;
        }

        JPAQueryFactory query = dao.getJPAQueryFactory();
        List<Long> bags = query.select(QBag.bag.id)
                .from(QBag.bag)
                .where(QBag.bag.status.eq(BagStatus.DEPOSITED).and(QBag.bag.creator.eq(username)))
                .fetch();

        for (Long id : bags) {
            Bag bag = query.selectFrom(QBag.bag)
                    .leftJoin(QBag.bag.files, QDataFile.dataFile)
                    .where(QBag.bag.id.eq(id))
                    .fetchOne();

            if (bag == null) {
                log.warn("Unable to fetch Bag(id={}) from the database!", id);
                continue;
            }

            log.info("[{}] Ingestion tasks ready to be run", bag.getName());
            long numFiles = query.selectFrom(QBagFile.bagFile)
                    .where(QBagFile.bagFile.bag.eq(bag))
                    .fetchCount();

            if (numFiles < bag.getTotalFiles()) {
                scanForBag(bag, staging, numFiles);
            } else if (numFiles == bag.getTotalFiles()) {
                registerStaging(bag, region, staging);
                checkInitialized(bag, region);
            } else {
                log.error("[{}] Bag has more files than expected", bag.getName());
            }
        }
    }

    /**
     * Step which checks if a {@link Bag} needs to be updated to {@code BagStatus.INITIALIZED}
     * <p>
     * Preconditions on the {@link Bag} are that {@code Bag.getStatus() == BagStatus.DEPOSITED} and
     * that a {@link StagingStorage} exists for the {@link Bag}'s storage (not TOKEN storage).
     *
     * @param bag    the {@link Bag} to check
     * @param region the {@link StorageRegion} the {@link Bag} is staged in
     */
    private void checkInitialized(Bag bag, StorageRegion region) {
        if (bag.getStatus() == BagStatus.DEPOSITED) {
            JPAQueryFactory query = dao.getJPAQueryFactory();
            // could make a func for this but meh for now
            StagingStorage storage = query.selectFrom(QStagingStorage.stagingStorage)
                    .where(QStagingStorage.stagingStorage.bag.eq(bag).and(
                            QStagingStorage.stagingStorage.region.eq(region)))
                    .fetchOne();
            if (storage != null) {
                bag.setStatus(BagStatus.INITIALIZED);
                dao.save(bag);
            }
        }
    }

    /**
     * Step which checks to see if a {@link StagingStorage} db entity needs to be created for a
     * {@link Bag}.
     * <p>
     * Precondition on the {@link Bag} that it has ALL {@link BagFile}s registered and
     * at least one of the corresponds to the file /tagmanifest-sha256.txt.
     *
     * @param bag     the {@link Bag} to operate on
     * @param region  the {@link StorageRegion} the {@link Bag} is staged in
     * @param staging local {@link Posix} storage information about where the {@link Bag} is
     */
    private void registerStaging(Bag bag, StorageRegion region, Posix staging) {
        Path stage = Paths.get(staging.getPath());
        Path root = stage.resolve(bag.getDepositor().getNamespace()).resolve(bag.getName());
        String tagmanifest = "/" + TAGMANIFEST_NAME + SHA_256.bagitPrefix();

        JPAQueryFactory query = dao.getJPAQueryFactory();
        StagingStorage storage = query.selectFrom(QStagingStorage.stagingStorage)
                .where(QStagingStorage.stagingStorage.bag.eq(bag).and(
                        QStagingStorage.stagingStorage.region.eq(region)))
                .fetchOne();

        // need highest priority hash (only sha256 for now so whatever)
        BagFile file = query.selectFrom(QBagFile.bagFile)
                .where(QBagFile.bagFile.filename.eq(tagmanifest))
                .fetchFirst();

        // make sure everything is still there
        if (storage == null && root.toFile().exists() && file != null) {
            log.info("[{}] Creating bag staging", bag.getName());

            long sum = query.select(QBagFile.bagFile.size.sum())
                    .from(QBagFile.bagFile)
                    .where(QBagFile.bagFile.bag.eq(bag))
                    .fetchFirst();

            long totalFiles = bag.getTotalFiles();
            String relative = stage.relativize(root).toString();

            storage = new StagingStorage(region,
                    bag,
                    sum,
                    totalFiles,
                    relative,
                    true); // active = true
            storage.setFile(file);
            dao.save(storage);
        } else if (storage != null) {
            log.warn("[{}] Unable to create replication staging for bag, conditions not met: " +
                            "bag exists on disk ? {}; tagmanifest exists ? {}",
                    bag.getName(), root.toFile().exists(), file != null);
        }
    }

    /**
     * Step which looks for a {@link Bag} on local {@link Posix} storage
     *
     * @param bag      the {@link Bag} to look for
     * @param staging  local {@link Posix} storage information about where the {@link Bag} is
     * @param numFiles the number of files which have already been registered for a {@link Bag}
     */
    private void scanForBag(Bag bag, Posix staging, long numFiles) {
        Path stagingRoot = Paths.get(staging.getPath());
        Path bagRoot = stagingRoot
                .resolve(bag.getDepositor().getNamespace())
                .resolve(bag.getName());
        boolean exists = bagRoot.toFile().isDirectory();


        if (!exists) {
            log.warn("[{}] Unable to locate bag at {}!", bag.getName(), bagRoot);
        } else {
            scanForManifests(bag, bagRoot, numFiles);
        }
    }

    /**
     * Step which scans for manifests by the name of {@code MANIFEST_NAME} and
     * {@code TAGMANIFEST_NAME} in the local storage of a {@link Bag}.
     *
     * @param bag      the {@link Bag} being operated on
     * @param root     the {@link Path} to the {@link Bag} on local storage
     * @param numFiles the number of files which have already been registered for a {@link Bag}
     */
    private void scanForManifests(Bag bag, Path root, long numFiles) {
        // ideally we would scan on tagmanifest-\(*\).txt and extract the prefix
        // from there we can get the algorithm with the highest priority
        // but since we only support sha256... well... we can just look for that instead
        FixityAlgorithm sha256 = SHA_256;

        Path manifest = root.resolve(MANIFEST_NAME + sha256.bagitPrefix());
        Path tagmanifest = root.resolve(TAGMANIFEST_NAME + sha256.bagitPrefix());

        boolean manifestExists = manifest.toFile().exists();
        boolean tagmanifestExists = tagmanifest.toFile().exists();

        if (!manifestExists || !tagmanifestExists) {
            log.warn("[{}] Manifest exists: {} or Tagmanifest exists: {}",
                    bag.getName(), manifestExists, tagmanifestExists);
        } else {
            registerManifest(bag, root, manifest, numFiles);
            registerManifest(bag, root, tagmanifest, numFiles);
            registerTagmanifest(bag, tagmanifest);
        }
    }

    /**
     * Step which registers a {@link Bag}'s {@code /tagmanifest-sha256.txt} file
     *
     * @param bag         the {@link Bag} begin operated on
     * @param tagmanifest the {@link Path} to the tagmanifest
     */
    private void registerTagmanifest(Bag bag, Path tagmanifest) {
        log.debug("[{}] Adding tagmanifest", bag.getName());
        File tmFile = tagmanifest.toFile();
        try {
            String filename = tagmanifest.getFileName().toString();
            // something about hasher matching the prefix
            HashCode hash = asByteSource(tmFile).hash(Hashing.sha256());
            BagFile file = dao.findOne(QBagFile.bagFile,
                    QBagFile.bagFile.filename.eq("/" + filename).and(QBagFile.bagFile.bag.eq(bag)));
            if (file == null) {
                file = new BagFile();
                file.setBag(bag);
                file.setSize(tmFile.length());
                file.setFilename(filename);
                file.addFixity(new Fixity(
                        ZonedDateTime.now(), file, hash.toString(), SHA_256.getCanonical()));
                bag.addFile(file);

                dao.save(file);
            }
        } catch (IOException e) {
            log.error("[{}] Unable to calculate hash for tagmanifest!", bag.getName());
        }
    }

    /**
     * Register files found within a manifest (manifest or tag) to a {@link Bag}.
     * <p>
     *
     * @param bag      the {@link Bag} being operated on
     * @param root     the {@link Path} to the root of the {@link Bag} in local storage
     * @param manifest the {@link Path} of the manifest to read from the {@link Bag}
     * @param numFiles the number of files which have already been registered for a {@link Bag}
     */
    private void registerManifest(Bag bag, Path root, Path manifest, long numFiles) {
        final int hash_idx = 0;
        final int file_idx = 1;

        log.info("[{}] Reading {} for file ingestion", bag.getName(), manifest.getFileName());
        Set<BagFile> files = new TreeSet<>(Comparator.comparing(BagFile::getFilename));
        try (Stream<String> lines = Files.lines(manifest)) {
            lines.map(s -> s.split("\\s", 2))
                    // should map to manifest entry
                    .filter(strings -> strings[hash_idx] != null && !strings[hash_idx].isEmpty() &&
                            strings[file_idx] != null && !strings[file_idx].isEmpty())
                    .filter(strings -> root.resolve(strings[file_idx].trim()).toFile().exists())
                    .map(strings -> new ManifestEntry(bag,
                            strings[file_idx].trim(),
                            strings[hash_idx].trim()))
                    .filter(entry -> fileNotInDb(entry, numFiles))
                    .map(entry -> createBagFile(entry, root))
                    .forEach(bagFile -> loadToBatch(bagFile, bag, files));
        } catch (IOException e) {
            log.error("[{}] Error reading manifest {}!", bag.getName(), manifest, e);
        }

        if (!files.isEmpty()) {
            bag.getFiles().addAll(files);
            dao.save(bag);
        }
    }

    /**
     * Check that a file does not exist in the database. If no files are registered for a collection
     * yet, skip this query.
     *
     * @param entry    the {@link ManifestEntry} containing the Bag and Filename to query on
     * @param numFiles if numFiles == 0, skip the query
     * @return true if the file does not exist, false otherwise
     */
    private boolean fileNotInDb(ManifestEntry entry, long numFiles) {
        JPAQueryFactory query = dao.getJPAQueryFactory();
        return numFiles == 0 || query.selectFrom(QBagFile.bagFile)
                .where(QBagFile.bagFile.filename.eq(entry.coercedPath())
                        .and(QBagFile.bagFile.bag.eq(entry.bag)))
                .fetchCount() == 0;
    }

    /**
     * Load a {@link BagFile}s to a {@link Set} and save if the batch size has been met.
     *
     * @param bagFile the {@link BagFile} to append
     * @param bag     the {@link Bag} to save the {@link BagFile} to
     * @param files   the {@link Set} of {@link BagFile}s to save
     */
    private void loadToBatch(BagFile bagFile, Bag bag, Set<BagFile> files) {
        log.trace("[{}] Adding {} to batch", bag.getName(), bagFile.getFilename());
        files.add(bagFile);

        if (files.size() == properties.getFileIngestBatchSize()) {
            bag.getFiles().addAll(files);
            dao.save(bag);
            files.clear();
        }
    }

    /**
     * Create a {@link BagFile} from an entry in a manifest
     * <p>
     *
     * @param entry a {@link ManifestEntry} containing the Bag, Filename, and digested hash
     * @param root  the {@link Path} to the file on disk
     * @return the created {@link BagFile}
     */
    private BagFile createBagFile(ManifestEntry entry, Path root) {
        Path file = root.resolve(entry.path);

        BagFile bf = new BagFile();
        bf.setBag(entry.bag);
        bf.setFilename(entry.path);
        bf.setSize(file.toFile().length());

        if (bf.getFixities().isEmpty()) {
            bf.addFixity(new Fixity(ZonedDateTime.now(), bf, entry.digest, SHA_256.getCanonical()));
        }

        return bf;
    }

    /**
     * Encapsulate some information about a manifest entry so we don't continually pass around
     * string[]
     */
    private class ManifestEntry {
        private final Bag bag;
        private final String path;
        private final String digest;

        private ManifestEntry(Bag bag, String path, String digest) {
            this.bag = bag;
            this.path = path;
            this.digest = digest;
        }

        private String coercedPath() {
            return path.startsWith("/") ? path : "/" + path;
        }
    }

}
