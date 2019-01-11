package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.entities.DataFile

/**
 * Serializer for [DataFile] to [org.chronopolis.rest.models.File]
 *
 * @author shake
 */
class DataFileSerializer : JsonSerializer<DataFile>() {

    override fun serialize(dataFile: DataFile, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeObject(dataFile.model())
    }

}