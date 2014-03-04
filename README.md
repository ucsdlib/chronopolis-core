Chronopolis-Core
================

Hello. If you're just looking for documentation check out [chronopolis-doc][1].

The basic layout of the project is as follows:

    Core Chronopolis /
         |    amqp-core             // Basic amqp producer/consumers. May be able to move to
         |    common                // Common code to be shared between services
         |    ingest-shell          // Ingest service for message flows
         |    intake-shell          // Sample Intake service for making mesasge flows
         |    logger                // Full logging of messages
         |    messaging             // AMQP Infrastructure. Centralized so that no service
                                    // needs to define it's own messages
         |    replication-frontend  // Web frontend for replication service 
         |    replication-shell     // Distribution Service


Development:
Please make changes to the develop branch first for testing :)
git fetch origin
git checkout --track origin/develop

Todo: 

Lots of stuff


[1]: https://chron-git.umiacs.umd.edu/chron-core/chronopolis-doc