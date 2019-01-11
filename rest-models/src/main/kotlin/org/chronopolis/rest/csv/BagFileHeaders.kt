package org.chronopolis.rest.csv

import org.chronopolis.rest.models.create.FileCreate

/**
 * Definition of the headers for a csv of what is essentially a [FileCreate]
 *
 * @author shake
 */
enum class BagFileHeaders {
    FILENAME, SIZE, FIXITY_VALUE, FIXITY_ALGORITHM
}