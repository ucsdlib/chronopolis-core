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
import java.math.BigDecimal

@DataJpaTest(excludeAutoConfiguration = [FlywayAutoConfiguration::class])
@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [JPAContext::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DepositorQueriesTest {

    @Autowired
    lateinit var ctx: DSLContext

    @Test
    fun testDepositorSummaryEmpty() {
        val summary = DepositorQueries.depositorsSummary(ctx)

        Assert.assertEquals("Multiple depositors exist", 1, summary.total)
        Assert.assertEquals("Bags exist", BigDecimal(0), summary.avgSum)
        Assert.assertEquals("Bags exist", BigDecimal(0), summary.avgCount)
    }

    @Test
    fun testZeroTopDepositorsBySum() {
        val topBySum = DepositorQueries.topDepositorsBySum(ctx, 5)
        Assert.assertTrue("Results found", topBySum.isEmpty())
    }

    @Test
    fun testZeroTopDepositorsByCount() {
        val topBySum = DepositorQueries.topDepositorsByCount(ctx, 5)
        Assert.assertTrue("Results found", topBySum.isEmpty())
    }

}