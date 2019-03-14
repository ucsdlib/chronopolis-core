package org.chronopolis.ingest.task;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.dao.BagFileDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.QDepositor;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.enums.BagStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.chronopolis.ingest.JpaContext.DELETE_SCRIPT;
import static org.chronopolis.rest.models.enums.FixityAlgorithm.SHA_256;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = AFTER_TEST_METHOD, scripts = DELETE_SCRIPT)
})
public class LocalIngestTest extends IngestTest {

    private static final Long ID = 1L;
    private static final String BAG_NAME = "new-bag-1";
    private static final String USERNAME = "test-admin";
    private static final String DEPOSITOR = "test-depositor";

    private static final Long HW_SIZE = 12L;
    private static final String HW_FILE = "/data/hello_world";
    private static final String HW_HASH = "a948904f2f0f479b8f8197694b30184b0d2ed1c1cd2a1ec0fb85d299a192a447";

    private static final Long MANIFEST_SIZE = 83L;
    private static final String MANIFEST_FILE = "/manifest-sha256.txt";
    private static final String MANIFEST_HASH = "5f9e5736168cec558b642bc10cdedb9cadfb87ca2fe0d3957dbd0d0ee6bc2ac4";

    private static final Long TAGMANIFEST_SIZE = 86L;
    private static final String TAGMANIFEST_FILE = "/tagmanifest-sha256.txt";
    private static final String TAGMANIFEST_HASH = "2ee2ba4bcc37fd36d82d66caac5788336443c745f410a671960c95fd8f2fb74f";

    private Depositor depositor;
    private LocalIngest localIngest;
    @Autowired private EntityManager entityManager;

    @Before
    public void setup() throws URISyntaxException {
        final URL bagRoot = ClassLoader.getSystemClassLoader().getResource("bags");

        MockitoAnnotations.initMocks(this);
        BagFileDao dao = new BagFileDao(entityManager);
        IngestProperties properties = new IngestProperties();
        properties.getScan().setEnabled(true);
        properties.getScan().setUsername(USERNAME);
        properties.getScan().setStaging(new Posix().setId(ID).setPath(bagRoot.getFile()));

        depositor = dao.findOne(QDepositor.depositor, QDepositor.depositor.namespace.eq(DEPOSITOR));
        localIngest = new LocalIngest(dao, properties);
    }

    @Test
    public void scanRegisterFiles() {
        // setup
        BagFileDao dao = new BagFileDao(entityManager);
        Bag toScan = new Bag(BAG_NAME, USERNAME, depositor, 3, 189, BagStatus.DEPOSITED);
        dao.save(toScan);

        localIngest.scan();

        List<BagFile> all = dao.findAll(QBagFile.bagFile, QBagFile.bagFile.bag.eq(toScan));
        Assert.assertEquals(3, all.size());

        Map<String, String> fixities = ImmutableMap.of(HW_FILE, HW_HASH,
                MANIFEST_FILE, MANIFEST_HASH,
                TAGMANIFEST_FILE, TAGMANIFEST_HASH);
        Map<String, Long> sizes = ImmutableMap.of(HW_FILE, HW_SIZE,
                MANIFEST_FILE, MANIFEST_SIZE,
                TAGMANIFEST_FILE, TAGMANIFEST_SIZE);

        all.forEach(bf -> {
            Assert.assertTrue(bf.getFilename().startsWith("/"));
            long size = sizes.getOrDefault(bf.getFilename(), -1L);
            Assert.assertEquals(size, bf.getSize());
            Assert.assertTrue(!bf.getFixities().isEmpty());
            bf.getFixities().forEach(fixity ->
                    Assert.assertEquals(fixities.get(bf.getFilename()), fixity.getValue())
            );
        });
    }

    @Test
    public void scanRegisterStagingStorage() {
        final String path = DEPOSITOR + "/" + BAG_NAME;

        // setup
        String algorithm = SHA_256.getCanonical();
        BagFileDao dao = new BagFileDao(entityManager);
        Bag toScan = new Bag(BAG_NAME, USERNAME, depositor, 189, 3, BagStatus.DEPOSITED);
        BagFile hw = new BagFile();
        hw.setBag(toScan);
        hw.setSize(HW_SIZE);
        hw.setFilename(HW_FILE);
        hw.addFixity(new Fixity(ZonedDateTime.now(), hw, HW_HASH, algorithm));
        BagFile manifest = new BagFile();
        manifest.setBag(toScan);
        manifest.setSize(MANIFEST_SIZE);
        manifest.setFilename(MANIFEST_FILE);
        manifest.addFixity(new Fixity(ZonedDateTime.now(), manifest, MANIFEST_HASH, algorithm));
        BagFile tagmanifest = new BagFile();
        tagmanifest.setBag(toScan);
        tagmanifest.setSize(TAGMANIFEST_SIZE);
        tagmanifest.setFilename(TAGMANIFEST_FILE);
        tagmanifest.addFixity(new Fixity(ZonedDateTime.now(), tagmanifest, TAGMANIFEST_HASH, algorithm));
        toScan.addFile(hw);
        toScan.addFile(manifest);
        toScan.addFile(tagmanifest);
        dao.save(toScan);

        localIngest.scan();

        StagingStorage staging = dao.findOne(QStagingStorage.stagingStorage,
                QStagingStorage.stagingStorage.bag.eq(toScan));


        Assert.assertNotNull(staging);
        Assert.assertTrue(staging.isActive());
        Assert.assertEquals(HW_SIZE + MANIFEST_SIZE + TAGMANIFEST_SIZE, staging.getSize());
        Assert.assertEquals(3, staging.getTotalFiles());
        Assert.assertEquals(tagmanifest, staging.getFile());
        Assert.assertEquals(path, staging.getPath());
    }
}