package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.AceToken
import org.chronopolis.rest.models.Bag
import java.time.ZonedDateTime
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

/**
 * Request to create an [AceToken]
 *
 * @property bagId The id of the [Bag] which this [AceToken] belongs to
 * @property round The corresponding round from the ACE TokenResponse
 * @property createDate The corresponding createDate from the ACE TokenResponse
 * @property proof The corresponding proof from the ACE TokenResponse
 * @property imsHost The IMS used to create this [AceToken]
 * @property filename The filename identifying this [AceToken]
 * @property algorithm The corresponding algorithm from the Ace TokenResponse
 * @property imsService The corresponding digest service from the Ace TokenResponse
 * @author shake
 */
data class AceTokenCreate(@get:Min(0) var bagId: Long = -1,
                          @get:Min(0) var round: Long = -1,
                          var createDate: ZonedDateTime = ZonedDateTime.now(),
                          @get:NotBlank var proof: String = "",
                          @get:NotBlank var imsHost: String = "",
                          @get:NotBlank var filename: String = "",
                          @get:NotBlank var algorithm: String = "",
                          @get:NotBlank var imsService: String = "")
