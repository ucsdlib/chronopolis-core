Chronopolis-Core
================

Hello. If you're just looking for documentation check out [chronopolis-doc][1].

The basic layout of the project is as follows:

    Core Chronopolis /
         |    amqp-core             // Basic amqp producer/consumers | @Deprecated
         |    chron-db              // DB Entities/Reposotories 
         |    common                // Common code to be shared between services
         |    duracloud-backend     // Backend code for duracloud intake service (bagging/etc)
         |    duracloud-frontend    // API and webapp for duracloud intake service
         |    ingest-rest           // RESTful implementation for ingest service
         |    ingest-shell          // Ingest service for message flows | @Deprecated
         |    intake-shell          // Sample Intake service for making mesasge flows
         |    logger                // Full logging of messages
         |    messaging             // AMQP Infrastructure. Centralized so that no service
                                    // needs to define it's own messages | @Deprecated
         |    replication-frontend  // Web frontend for replication service 
         |    replication-shell     // Distribution Service
         |    rest-common           // Common models/interfaces for RESTful services


Development
===========
Please make changes to the develop branch first for testing :)
```
git fetch origin
git checkout --track origin/develop
```
Todo: 

* Add property for setting the from field for smtp (may want to copy ace and do from@localhost)


[1]: https://gitlab.umiacs.umd.edu/chronopolis/chronopolis-core/wikis/home
