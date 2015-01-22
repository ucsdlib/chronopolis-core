Ingest-REST
================

This module serves as an implementation for the API laid out in the wiki. There 
are a few resource files of importance which should be noted:

* data.sql - Initial data which gets loaded to the database
* schema.sql - The database schema

Testing
=======

## Unit Tests
I've noticed a few quirks for testing which we can smooth out, but for now we need
our application.properties to have a few specific settings:
```
spring.profiles.active=development
```

When running the development profile, we create mock data from which we can test
the API. 

## Other testing
[Postman][Postman] is a great tool for interacting with RESTful APIs, 
it allows you to easily set authentication information, and will do a pretty
print for all json which is returned. Useful for quickly testing new updates made
to the API.

[Postman]: https://chrome.google.com/webstore/detail/postman-rest-client/fdmmgilgnpjigdojojpjoooidkmcomcm?hl=en
