package org.chronopolis.rest.kot.entities.serializers

import org.chronopolis.rest.kot.models.enums.BagStatus
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class SerializerContext

fun main(args: Array<String>) {
    val members = BagStatus::class.members
    members.forEach { println(it) }


    SpringApplication.run(SerializerContext::class.java)
}