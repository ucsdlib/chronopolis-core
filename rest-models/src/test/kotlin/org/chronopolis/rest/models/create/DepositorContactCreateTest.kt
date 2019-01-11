package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Phone
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

class DepositorContactCreateTest {

    private val name = "test-name"
    private val email = "fake-account@umiacs.umd.edu"
    private val invalidEmail = "notafulladdress"
    private val validPhone = Phone("446681800", "CH")
    private val invalidPhone = Phone("0", "US")

    private lateinit var factory: ValidatorFactory
    private lateinit var validator: Validator

    @Before
    fun init() {
        factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Test
    fun testValidDepositorContact() {
        val contact = DepositorContactCreate(name, email, validPhone)
        val violations = validator.validate(contact)
        Assert.assertTrue(violations.isEmpty())
    }

    @Test
    fun testInvalidName() {
        val contact = DepositorContactCreate("", email, validPhone)
        val violations = validator.validate(contact)
        Assert.assertEquals(1, violations.size)
    }

    @Test
    fun testInvalidEmail() {
        val contact = DepositorContactCreate(name, invalidEmail, validPhone)
        val violations = validator.validate(contact)
        Assert.assertEquals(1, violations.size)
    }

    @Test
    fun testInvalidPhoneNumber() {
        val contact = DepositorContactCreate(name, email, invalidPhone)
        val violations = validator.validate(contact)
        Assert.assertEquals(1, violations.size)
    }

}