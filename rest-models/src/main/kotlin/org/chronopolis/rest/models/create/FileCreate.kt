package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.models.Fixity
import org.chronopolis.rest.models.enums.FixityAlgorithm
import org.hibernate.validator.constraints.NotEmpty
import javax.validation.constraints.Min

/**
 * A request to create a File in Chronopolis
 *
 * @property filename the name of the file
 * @property size the size of the file, greater than 0
 * @property fixity the value used for the [Fixity]
 * @property fixityAlgorithm the algorithm used for the [Fixity]
 * @property bag The id of the [Bag] which the File belongs to, optional
 *
 * @author shake
 */
data class FileCreate(@get:NotEmpty var filename: String = "",
                      @get:Min(1) var size: Long = 0,
                      @get:NotEmpty var fixity: String = "",
                      var fixityAlgorithm: FixityAlgorithm = FixityAlgorithm.SHA_256,
                      var bag: Long? = null)
