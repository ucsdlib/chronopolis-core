Duracloud Intake
================

This module represents the frontend for which the Duracloud Bridge Application
communicates with Chronopolis.

It consists of the RESTful API defined [on the Duraspace wiki](https://wiki.duraspace.org/display/CHRONO/Chron+Bag+Server+REST+API?src=contextnavpagetreemode)
and of the necessary message handlers for communication within Chronopolis.

TODO:
* Pull RESTful API out of the bag-rest-impl module and move it here
* Pull the message listeners from the bag-rest-impl and move them here
* mnah mnah