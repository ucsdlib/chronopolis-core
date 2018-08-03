package org.chronopolis.rest.kot.api

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Properties for connecting to a Chronopolis Ingest Server
 *
 * @property endpoint The endpoint of the Ingest Server
 * @property username The username to connect to the Ingest Server as
 * @property password The password to use
 *
 * @author shake
 */
@ConfigurationProperties(prefix = "ingest.api")
data class IngestApiProperties(val endpoint: String = "http://localhost:8080/",
                               val username: String = "ingest-user",
                               val password: String = "change-me")
