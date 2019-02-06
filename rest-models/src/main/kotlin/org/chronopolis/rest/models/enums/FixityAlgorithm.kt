package org.chronopolis.rest.models.enums

/**
 * Supported message digest functions in Chronopolis
 *
 * @author shake
 */
enum class FixityAlgorithm(val canonical: String) {
    SHA_256("SHA-256"), UNSUPPORTED("unsupported");

    fun bagitPrefix(): String = when (this) {
        SHA_256 -> "sha256.txt"
        else -> ".txt"
    }

    companion object {
        fun fromString(algorithm: String): FixityAlgorithm {
            return when (algorithm.toLowerCase()) {
                "sha256", "sha-256", "sha_256" -> SHA_256
                else -> UNSUPPORTED
            }
        }
    }
}