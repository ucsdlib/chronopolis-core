package org.chronopolis.db.queries;

import org.chronopolis.db.generated.Tables;
import org.chronopolis.db.generated.tables.Bag;
import org.chronopolis.db.generated.tables.Depositor;
import org.chronopolis.db.generated.tables.records.DepositorRecord;
import org.chronopolis.rest.entities.JPAContext;
import org.jooq.DSLContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Super crusty test, need to prepopulate things and get everything going... oof
 *
 * @author shake
 */
@DataJpaTest(excludeAutoConfiguration = FlywayAutoConfiguration.class)
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JPAContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BagFunctionsTest {

    @Autowired
    private DSLContext dsl;

    @Test
    public void testFilenamesInBag() {
        Bag bag = Tables.BAG;
        Depositor depositor = Tables.DEPOSITOR;
        Timestamp now = Timestamp.from(ZonedDateTime.now().toInstant());

        DepositorRecord depositorRecord = dsl.fetchOne(depositor);

        dsl.insertInto(bag)
                .set(bag.CREATED_AT, now)
                .set(bag.UPDATED_AT, now)
                .set(bag.NAME, "test-filenames-in-bag")
                .set(bag.CREATOR, "bag-functions-test")
                .set(bag.SIZE, 100L)
                .set(bag.TOTAL_FILES, 4L)
                .set(bag.DEPOSITOR_ID, depositorRecord.getId())
                .execute();

        List<String> strings = BagFunctionsKt.filenamesInBag(dsl, dsl.fetchOne(bag));
        Assert.assertTrue("Files exist for bag", strings.isEmpty());
    }

    @Test
    public void testTokenCountForBag() {
        Bag bag = Tables.BAG;
        Depositor depositor = Tables.DEPOSITOR;
        Timestamp now = Timestamp.from(ZonedDateTime.now().toInstant());

        DepositorRecord depositorRecord = dsl.fetchOne(depositor);

        dsl.insertInto(bag)
                .set(bag.CREATED_AT, now)
                .set(bag.UPDATED_AT, now)
                .set(bag.NAME, "test-token-count-for-bag")
                .set(bag.CREATOR, "bag-functions-test")
                .set(bag.SIZE, 100L)
                .set(bag.TOTAL_FILES, 4L)
                .set(bag.DEPOSITOR_ID, depositorRecord.getId())
                .execute();

        int count = BagFunctionsKt.tokenCountForBag(dsl, dsl.selectFrom(bag).where(bag.NAME.eq("test-token-count-for-bag")).fetchOne());
        Assert.assertEquals("Tokens exist for bag!", 0, count);
    }

}
