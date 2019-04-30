package org.chronopolis.db.result

/**
 * todo: okhttp bodybuilder responses attached to these
 */
enum class CreateStatus {
    CREATED, // (ResponseEntity.status(HttpStatus.CREATED)),
    CONFLICT,
    BAD_REQUEST
}