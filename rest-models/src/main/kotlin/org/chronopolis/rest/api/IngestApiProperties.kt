package org.chronopolis.rest.api

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
data class IngestApiProperties(var endpoint: String = "http://localhost:8080/",
                               var username: String = "ingest-user",
                               var password: String = "change-me")
