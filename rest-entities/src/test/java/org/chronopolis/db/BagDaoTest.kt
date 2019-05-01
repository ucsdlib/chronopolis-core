package org.chronopolis.db

import org.chronopolis.db.binding.BagPageable
import org.chronopolis.db.result.CreateStatus
import org.chronopolis.rest.entities.JPAContext
import org.chronopolis.rest.models.create.BagCreate
import org.jooq.DSLContext
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [JPAContext::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BagDaoTest {

    val creator = "bag-dao-test"
    val depositor = "test-depositor"
    val name = "test-process-request"
    val size = 1024L
    val files = 5L
    val region = 1L

    @Autowired
    lateinit var dsl: DSLContext

    @Test
    fun testFindOneNotExists() {
        val dao = BagDao(dsl)
        val bag = dao.findOne(99L)
        Assert.assertFalse(bag.isPresent)
    }

    @Test
    fun testFindListIsEmpty() {
        val dao = BagDao(dsl)
        val bags = dao.findAll(emptyList(), emptyList(), Limit(0, 5))
        Assert.assertTrue(bags.isEmpty())
    }

    @Test
    fun testFindListByPageableIsEmpty() {
        val dao = BagDao(dsl)
        val pageable = BagPageable()
        pageable.setDepositor("test-depositor")
        val bags = dao.findAll(pageable)
        Assert.assertTrue(bags.isEmpty())
    }

    @Test
    fun testProcessRequest() {
        val location = "$depositor/$name"
        val dao = BagDao(dsl)
        val request = BagCreate(name, size, files, region, location, depositor)

        val result = dao.processRequest(creator, request)
        Assert.assertNotNull(result)
        Assert.assertNotNull(result.bag)
        Assert.assertEquals("Status should be created", CreateStatus.CREATED, result.status)
    }

    @Test
    fun testProcessRequestFailDepositor() {
        val depositor = "test-depositor-dne"
        val location = "$depositor/$name"
        val dao = BagDao(dsl)
        val request = BagCreate(name, size, files, region, location, depositor)

        val result = dao.processRequest(creator, request)
        Assert.assertNotNull(result)
        Assert.assertNull(result.bag)
        Assert.assertEquals("Status should be BAD_REQUEST", CreateStatus.BAD_REQUEST, result.status)
    }

    @Test
    @Ignore
    fun testProcessRequestFailConflict() {
    }
}