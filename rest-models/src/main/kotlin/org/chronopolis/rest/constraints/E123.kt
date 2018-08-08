package org.chronopolis.rest.constraints

import javax.validation.Constraint
import kotlin.reflect.KClass

/**
 * JSR-303 annotation for validation on phone numbers (with the help of libphonenumber)
 *
 * @author shake
 */
@MustBeDocumented
// @Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PhoneNumberValidator::class])
annotation class E123(
        val message: String = "Invalid PhoneNumber",
        val groups: Array<KClass<out Any>> = [],
        val payload: Array<KClass<out Any>> = []
)