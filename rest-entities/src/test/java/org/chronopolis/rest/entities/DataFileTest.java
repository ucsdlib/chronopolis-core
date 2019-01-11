package org.chronopolis.rest.entities;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.QFixity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private final String CREATOR = "data-file-test";
    private final String TEST_PATH = "/test-path";

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

            Assert.assertTrue(df instanceof BagFile);
            BagFile bf = (BagFile) df;
            Assert.assertNotNull(bf.getToken());
            Assert.assertEquals(token, bf.getToken());
            Assert.assertEquals(bag, bf.getToken().getBag());
            Assert.assertEquals(bag, df.getBag());
        });
    }

    @Test
    public void persistWithFileAndToken() {
        final String name = "persist-file-token";

        Date date = new Date();
        Bag bag = JPAContext.createBag(name, CREATOR, depositor);
        BagFile file = new BagFile();
        Fixity fixity = new Fixity(ZonedDateTime.now(), file, FIXITY_VALUE, FIXITY_ALGORITHM);
        file.setBag(bag);
        file.setSize(LONG_VALUE);
        file.setFilename(TEST_PATH);
        file.addFixity(fixity);

        AceToken token = new AceToken(PROOF, LONG_VALUE,
                IMS_SERVICE, FIXITY_ALGORITHM, IMS_HOST, date, bag, file);
        file.setToken(token);
        bag.addFile(file);

        entityManager.persist(bag);

        runChecks(name, bag, token, fixity);

        // checks against querying fixity
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QFixity qFixity = QFixity.fixity;
        Fixity fetchFixity = queryFactory.select(qFixity)
                .from(QBagFile.bagFile)
                .join(QBagFile.bagFile.fixities, qFixity)
                .where(QBagFile.bagFile.id.eq(file.getId())
                        .and(qFixity.algorithm.eq(FIXITY_ALGORITHM)))
                .fetchOne();

        Assert.assertNotNull(fetchFixity);
        
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
        fixity = new Fixity(ZonedDateTime.now(), bagFile, FIXITY_VALUE, FIXITY_ALGORITHM);
        bagFile.addFixity(fixity);

        token = new AceToken(PROOF, LONG_VALUE,
                IMS_SERVICE, FIXITY_ALGORITHM, IMS_HOST, date, bag, bagFile);
        bagFile.setToken(token);

        entityManager.merge(fetch);
        runChecks(name, fetch, token, fixity);
    }
}
