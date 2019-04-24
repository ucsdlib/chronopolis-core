package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.rest.entities.JPAContext
import org.jooq.DSLContext
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime

@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [JPAContext::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BagQueriesTest {

    @Autowired
    lateinit var dsl: DSLContext

    @Test
    fun testFilenamesInBag() {
        val bag = Tables.BAG
        val depositor = Tables.DEPOSITOR

        val depositorRecord = dsl.fetchOne(depositor)
        val bagRecord = dsl.insertInto(bag)
                .set(bag.CREATED_AT, LocalDateTime.now())
                .set(bag.UPDATED_AT, LocalDateTime.now())
                .set(bag.NAME, "test-filenames-in-bag")
                .set(bag.CREATOR, "bag-functions-test")
                .set(bag.SIZE, 100L)
                .set(bag.TOTAL_FILES, 4L)
                .set(bag.DEPOSITOR_ID, depositor.ID)
                .returning().fetchOne()

        val strings = BagQueries.filenamesInBag(dsl, bagRecord)
        Assert.assertTrue("Files exist for bag", strings.isEmpty())
    }

}