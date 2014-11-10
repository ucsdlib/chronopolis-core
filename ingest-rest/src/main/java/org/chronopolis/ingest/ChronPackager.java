package org.chronopolis.ingest;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.chronopolis.common.ace.BagTokenizer;
import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.digest.DigestUtil;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.ingest.model.Bag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by shake on 11/6/14.
 */
public class ChronPackager {
    private final Logger log = LoggerFactory.getLogger(ChronPackager.class);
    private static final String TAR_TYPE = "application/x-tar";

    String name;
    String fileName;
    String depositor;
    String fixityAlgorithm;
    IngestSettings settings;

    public ChronPackager(String name, String fileName, String depositor, IngestSettings settings) {
        this.name = name;
        this.fileName = fileName;
        this.depositor = depositor;
        this.settings = settings;
    }

    public Bag packageForChronopolis() {
        BagTokenizer tokenizer;
        MailUtil mailUtil = new MailUtil();

        // Set up our paths
        Path toBag = Paths.get(settings.getBagStage(), fileName);
        try {
            String mimeType = Files.probeContentType(toBag);
            if (mimeType != null && mimeType.equals(TAR_TYPE)) {
                toBag = untar(toBag, depositor);
            }
        } catch (IOException e) {
            log.error("Error probing mime type for bag", e);
            throw new RuntimeException(e);
        }

        // TODO: Read from bag
        fixityAlgorithm = "SHA-256";
        Digest fixity = Digest.fromString(fixityAlgorithm);

        Path tokenStage = Paths.get(settings.getTokenStage());
        String tagManifestDigest; // = tokenizer.getTagManifestDigest();

        Bag bag = new Bag();
        bag.setName(name);
        bag.setDepositor(depositor);

        // And create our tokens
        Path manifest = null;
        if (bag.getTokenLocation() != null) {
            log.info("Tokens already created for {}, skipping", name);
            manifest = tokenStage.resolve(depositor)
                                 .resolve(name + "-tokens");
            tagManifestDigest = bag.getTagManifestDigest();
        } else {
            tokenizer = new BagTokenizer(toBag, tokenStage, fixityAlgorithm, depositor);
            try {
                manifest = tokenizer.getAceManifestWithValidation();
            } catch (Exception e) {
                log.error("Error creating ace manifest {}", e);
            }

            tagManifestDigest = tokenizer.getTagManifestDigest();
        }

        // Create digests for replicate nodes to validate from
        String tokenDigest = DigestUtil.digest(manifest, fixity.getName());

        bag.setTagManifestDigest(tagManifestDigest);
        bag.setTokenDigest(tokenDigest);

        String user = settings.getExternalUser();
        String server = settings.getStorageServer();

        // Should end up being the location for a download
        StringBuilder tokenStore = new StringBuilder(user);
        tokenStore.append("@").append(server);
        tokenStore.append(":").append(manifest.toString());
        StringBuilder bagLocation = new StringBuilder(user);
        bagLocation.append("@").append(server);
        bagLocation.append(":").append(toBag.toString());

        bag.setTokenLocation(tokenStore.toString());
        bag.setLocation(bagLocation.toString());
        bag.setFixityAlgorithm("SHA-256");
        bag.setProtocol("rsync");
        bag.setSize(1000);

        SimpleMailMessage message = mailUtil.createMessage(settings.getNode(),
           "Package Ready to Replicate",
           bag.toString());
        mailUtil.send(message);

        return bag;
    }


    private Path untar(final Path toBag, String depositor) throws IOException {
        // Set up our tar stream and channel
        TarArchiveInputStream tais = new TarArchiveInputStream(Files.newInputStream(toBag));
        TarArchiveEntry entry = tais.getNextTarEntry();
        ReadableByteChannel inChannel = Channels.newChannel(tais);

        // Get our root path (just the staging area), and create an updated bag path
        Path root = Paths.get(settings.getBagStage(), depositor);
        Path bag = root.resolve(entry.getName());

        while (entry != null) {
            Path entryPath = root.resolve(entry.getName());

            if (entry.isDirectory()) {
                log.trace("Creating directory {}", entry.getName());
                Files.createDirectories(entryPath);
            } else {
                log.trace("Creating file {}", entry.getName());

                entryPath.getParent().toFile().mkdirs();

                // In case files are greater than 2^32 bytes, we need to use a
                // RandomAccessFile and FileChannel to write them
                RandomAccessFile file = new RandomAccessFile(entryPath.toFile(), "rw");
                FileChannel out = file.getChannel();

                // The TarArchiveInputStream automatically updates its offset as
                // it is read, so we don't need to worry about it
                out.transferFrom(inChannel, 0, entry.getSize());
                out.close();
            }

            entry = tais.getNextTarEntry();
        }

        // Because we aren't always certain the first element of the tar file
        // is the root directory, we need to resolve it from the bag variable
        // TODO: There might be a cleaner way to do this
        return root.resolve(bag.getName(root.getNameCount()));
    }



}
