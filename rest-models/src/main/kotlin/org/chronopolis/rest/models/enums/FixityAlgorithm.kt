package org.chronopolis.rest.models.enums

/**
 * Supported message digest functions in Chronopolis
 *
 * todo: canonical name?
 *
 * @author shake
 */
enum class FixityAlgorithm {
    SHA_256, UNSUPPORTED;

    companion object {
        fun fromString(algorithm: String): FixityAlgorithm {
            return when (algorithm.toLowerCase()) {
                "sha256", "sha-256", "sha_256" -> SHA_256
                else -> UNSUPPORTED
            }
        }
    }
}