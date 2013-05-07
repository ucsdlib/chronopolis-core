Chronopolis-Core
================

Hello. If you're just looking for documentation check out [chronopolis-doc][1].

The basic layout of the project is as follows:

    Core Chronopolis /
         |    chron-common     // Common code to be shared between services
         |    chron-repl       // Distribution Service ( needs to be renamed)
         |    chron-intake     // Doesn't exist yet
         |    duracloud-intake // REST Service for duracloud intake
         |    chron-ingest     // Ingest Service, doesn't exist. will make soon
         |    chron-messaging  // AMQP Infrastructure. Centralized so that no service
                               // needs to define it's own messages
         |    chron-amqp-core  // Can probably be removed
         |    chron-notifier   // REST Service for chron stuff (intake I think)
         |    chron-bagit      // BagIt stuff, should probably be moved into a separate repo


Todo: 

Lots of stuff


[1]: https://chron-git.umiacs.umd.edu/chron-core/chronopolis-doc
