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
import java.math.BigDecimal

@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [JPAContext::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StorageRegionQueriesTest {

    @Autowired
    lateinit var context: DSLContext

    @Test
    fun testUsedSpaceZero() {
        val storage = context.selectFrom(Tables.STORAGE_REGION).fetchAny()
        val used = StorageRegionQueries.usedSpace(context, storage)

        Assert.assertEquals("Used space is nonzero", used, BigDecimal(0))
    }

}