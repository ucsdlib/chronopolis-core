Chronopolis-Core
================

Hello. If you're just looking for documentation check out [chronopolis-doc][1].

The basic layout of the project is as follows:

    Core Chronopolis /
         |    chron-common       // Common code to be shared between services
         |    chron-repl         // Distribution Service ( needs to be renamed)
         |    chron-intake       // Doesn't exist yet
         |    duracloud-intake   // REST Service for duracloud intake
         |    chron-ingest-shell // Temp Ingest service for message flows
         |    chron-intake-shell // Temp Intake service for making mesasge flows
         |    chron-messaging    // AMQP Infrastructure. Centralized so that no service
                                 // needs to define it's own messages
         |    chron-amqp-core    // Basic amqp producer/consumers. May be able to move to
                                 // chron-common
         |    chron-notifier     // REST Service for chron stuff (intake I think)
         |    chron-bagit        // BagIt stuff, should probably be moved into a separate repo


Todo: 

Lots of stuff


[1]: https://chron-git.umiacs.umd.edu/chron-core/chronopolis-doc
