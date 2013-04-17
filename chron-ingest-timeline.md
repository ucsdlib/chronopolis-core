Chronopolis DPN Timeline
========================

### April - May

Develop messages needed for Ingestion

Determine if we should use the chron-notify service to send messages as it 
already is an ingestion point for Duracloud 

Early possibilities for message body:

|     K      |       V       | 
| ---------- | ------------- |
| accountId  | myAccount     |
| status     | 0             |
| submittor  | john-connor   |
| identifier | 1351538671433 |
| spaceId    | cloud-space   |
| itemId     | item.txt      |


### May - July

Start work on chron-ingest service, producing nightly or weekly builds with
new updates to the services messages and production

Specific tasks which need to be implemented:
* Message consumer for recieving new collections
* Internal Queue which holds locations of things to download
* Actual thing to download files (and check digests)
* Database (or config file) to hold staging area and other settings
* Ability to create Chronopolis style bags
* Ability to ingest DPN style bags

### July - August

Begin testing ingests from DPN and possibly other sources (Duracloud).
Refactoring of messages will happen if necessary, we want to send the 
minimum amount of data necessary to keep things simple. 

Move on to larger tests and bring in real data from DPN.