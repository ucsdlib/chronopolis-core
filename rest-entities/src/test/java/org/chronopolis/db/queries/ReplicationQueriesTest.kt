package org.chronopolis.db.queries

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

@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [JPAContext::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReplicationQueriesTest {

    @Autowired
    lateinit var context: DSLContext

    @Test
    fun testReplicationSummaryEmpty() {
        val summary = ReplicationQueries.replicationSummary(context)

        Assert.assertEquals("Replications exists", summary.total, 0)
        Assert.assertEquals("Replications are stuck (1wk)", summary.stuckOneWeek, 0)
        Assert.assertEquals("Replications are stuck (2wk)", summary.stuckTwoWeeks, 0)
    }

}