package org.chronopolis.rest.entities.storage

import org.chronopolis.rest.entities.DataFile
import org.chronopolis.rest.entities.PersistableEntity
import org.chronopolis.rest.entities.converters.ZonedDateTimeConverter
import java.time.ZonedDateTime
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.ManyToOne

/**
 * Basic description of a Fixity entity
 *
 * todo: supported algorithms should be defined in the database
 * todo: does this need a custom equals?
 *
 * @property value the value generated by the [algorithm]
 * @property algorithm the algorithm used
 * @property createdAt when the Fixity was created
 *
 * @author shake
 */
@Entity
class Fixity(
        @Convert(converter = ZonedDateTimeConverter::class)
        var createdAt: ZonedDateTime = ZonedDateTime.now(),

        @ManyToOne
        var file: DataFile? = null,

        var value: String = "",
        var algorithm: String = ""
) : PersistableEntity()