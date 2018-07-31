package org.chronopolis.rest.kot.entities.serializers

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class SerializerContext

fun main(args: Array<String>) {
    SpringApplication.run(SerializerContext::class.java)
}