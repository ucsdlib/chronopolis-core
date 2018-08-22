package org.chronopolis.rest.entities;

import com.google.common.collect.ImmutableSet;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.storage.Fixity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Set;

import static org.chronopolis.rest.entities.JPAContext.FIXITY_ALGORITHM;
import static org.chronopolis.rest.entities.JPAContext.FIXITY_VALUE;
import static org.chronopolis.rest.entities.JPAContext.IMS_HOST;
import static org.chronopolis.rest.entities.JPAContext.IMS_SERVICE;
import static org.chronopolis.rest.entities.JPAContext.LONG_VALUE;
import static org.chronopolis.rest.entities.JPAContext.PROOF;

/**
 * Test for persistence of BagFiles with AceTokens
 *
 * @author shake
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JPAContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DataFileTest {

    private final Logger log = LoggerFactory.getLogger(DataFileTest.class);

    private final String CREATOR = "data-file-test";
    private final String TEST_PATH = "test-path";

    private Depositor depositor;

    @Autowired
    private EntityManager entityManager;

    @Before
    public void initFromDb() {
        depositor = entityManager.find(Depositor.class, 1L);
    }

    public void runChecks(String name, Bag bag, AceToken token, Fixity fixity) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QBag qBag = QBag.bag;
        Bag fetch = queryFactory.selectFrom(qBag)
                .where(QBag.bag.name.eq(name))
                .fetchOne();

        Assert.assertNotNull(fetch);
        Assert.assertNotNull(fetch.getFiles());
        Assert.assertEquals(1, fetch.getFiles().size());

        fetch.getFiles().forEach(df -> {
            Assert.assertEquals(1L, df.getSize());
            Assert.assertEquals(TEST_PATH, df.getFilename());
            Assert.assertEquals(1, df.getFixities().size());
            df.getFixities().forEach(registeredFixity -> {
                Assert.assertEquals(fixity.getValue(), registeredFixity.getValue());
                Assert.assertEquals(fixity.getAlgorithm(), registeredFixity.getAlgorithm());
            });

            Assert.assertNotNull(df.getToken());
            Assert.assertEquals(token, df.getToken());
            Assert.assertEquals(bag, df.getBag());
            Assert.assertEquals(bag, df.getToken().getBag());
        });
    }

    @Test
    public void persistWithFileAndToken() {
        final String name = "persist-file-token";

        Date date = new Date();
        Bag bag = JPAContext.createBag(name, CREATOR, depositor);
        Fixity fixity = new Fixity(ZonedDateTime.now(), FIXITY_VALUE, FIXITY_ALGORITHM);
        BagFile file = new BagFile();
        file.setBag(bag);
        file.setSize(LONG_VALUE);
        file.setFilename(TEST_PATH);
        file.setFixities(ImmutableSet.of(fixity));

        AceToken token = new AceToken(PROOF, LONG_VALUE,
                IMS_SERVICE, FIXITY_ALGORITHM, IMS_HOST, date, file);
        token.setBag(bag);
        file.setToken(token);
        bag.addFile(file);

        entityManager.persist(bag);

        runChecks(name, bag, token, fixity);
    }

    @Test
    public void mergeToken() {
        final String name = "merge-token";
        Date date = new Date();
        Bag bag = JPAContext.createBag(name, CREATOR, depositor);
        BagFile file = new BagFile();
        file.setBag(bag);
        file.setSize(LONG_VALUE);
        file.setFilename(TEST_PATH);
        bag.addFile(file);

        entityManager.persist(bag);

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        Bag fetch = queryFactory.selectFrom(QBag.bag)
                .where(QBag.bag.name.eq(name))
                .fetchOne();

        Assert.assertNotNull(fetch);
        Assert.assertNotNull(fetch.getFiles());
        Set<DataFile> files = fetch.getFiles();
        Assert.assertEquals(1L, files.size());

        // For fun, query the BagFile
        BagFile bagFile = queryFactory.selectFrom(QBagFile.bagFile)
                .where(QBagFile.bagFile.bag.eq(fetch))
                .fetchFirst();
        Fixity fixity;
        AceToken token;
        fixity = new Fixity(ZonedDateTime.now(), FIXITY_VALUE, FIXITY_ALGORITHM);
        bagFile.getFixities().add(fixity);

        token = new AceToken(PROOF, LONG_VALUE,
                IMS_SERVICE, FIXITY_ALGORITHM, IMS_HOST, date, bagFile);
        token.setBag(fetch);
        bagFile.setToken(token);

        entityManager.merge(fetch);
        runChecks(name, fetch, token, fixity);
    }
}