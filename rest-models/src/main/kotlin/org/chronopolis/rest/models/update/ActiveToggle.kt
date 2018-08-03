package org.chronopolis.rest.models.update

/**
 * Toggle for StagingStorage to show it is no longer in use
 *
 * Note: it would be nice to have the val be 'isActive', but this causes deserialization issues. We
 * could write a deserializer for it - simple since it is only a single value, or just provide
 * 'isActive' as a method here since it is easy to impl. Not a big deal either way, for now just
 * going to add the method as it is less work (don't need to worry about registration, less classes)
 *
 * @author shake
 */
data class ActiveToggle(val active: Boolean) {
    fun isActive() = active
}
