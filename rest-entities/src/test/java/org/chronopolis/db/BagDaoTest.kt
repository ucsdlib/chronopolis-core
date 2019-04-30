package org.chronopolis.db

import org.chronopolis.db.binding.BagPageable
import org.chronopolis.db.result.CreateStatus
import org.chronopolis.rest.entities.JPAContext
import org.chronopolis.rest.models.create.BagCreate
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
class BagDaoTest {
    @Autowired
    lateinit var dsl: DSLContext

    @Test
    fun testFindOneNotExists() {
        val dao = BagDao(dsl)
        val bag = dao.findOne(1L)
        Assert.assertFalse(bag.isPresent)
    }

    @Test
    fun testFindListIsEmpty() {
        val dao = BagDao(dsl)
        val bags = dao.find(emptyList(), emptyList(), Limit(0, 5))
        Assert.assertTrue(bags.isEmpty())
    }

    @Test
    fun testFindListByPageableIsEmpty() {
        val dao = BagDao(dsl)
        val pageable = BagPageable()
        pageable.setDepositor("test-depositor")
        val bags = dao.find(pageable)
        Assert.assertTrue(bags.isEmpty())
    }

    @Test
    fun testProcessRequest() {
        val creator = "bag-dao-test"
        val depositor = "test-depositor"
        val name = "test-process-request"
        val size = 1024L
        val files = 5L
        val region = 1L
        val location = "$depositor/$name"

        val dao = BagDao(dsl)
        val request = BagCreate(name, size, files, region, location, depositor)

        val result = dao.processRequest(creator, request)
        Assert.assertNotNull(result)
        Assert.assertNotNull(result.bag)
        Assert.assertEquals("Status should be created", CreateStatus.CREATED, result.status)
    }
}