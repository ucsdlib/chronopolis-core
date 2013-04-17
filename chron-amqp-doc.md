Chronopolis Message Documentation
===========================

In order to get automation within the Chronopolis Architecture we 
have a variety of messages sent with RabbitMQ for communication
between nodes. Messages are comprised of two parts, the header and
the body, each of which is mostly just key/value pairs.

### Message Prototype:  
Prototype for each message. The header will always be the same for messages
so only the body will be included in examples below.

As the ack looks to be part of the standard body, it is TBD whether it
isneeded in the header or not. Actually it's part of AMQP so let's take it 
out for now. 

__Message Header__  

| Key           | Value        | Type    | 
| ----------    | ------------ | ------  |
| origin        | node name    | String  |
| timestamp     | datetime     | String  |
| correlationId | Message ID   | String  | 
| returnKey     | ???          | String  | 

__Message body__  

| Key    | Value    | Type   | 
| ------ | -------- | ------ |
| Key 1  | Value 1  | String |
| ...    | ...      | ...    |
| Key n  | Value n  | String |


Exchanges :beer:
----------------
The current exchange that Chronopolis will use is chronopolis-control.
It is a topic exchange from which each consumer will bind queues to. Producers
do not need queues and will only write to the exchange with a routing key. 

### chronopolis-control

This is a topic exchange which all sites will bind to and have queues
listen on different topics. 

#### Topics 

Within our exchanges we have a variety of topics for the organization 
of data and how it flows.

Upon receiving and validating a new collection, the Ingestion Node will send
out messages on two routing keys.

#### Intake <--> Ingest

##### chron.collection.ingest

Upon recieving a ticket for a creating a collection, the intake will move the
information to the Ingest node which 

#### Ingest <--> Distribute

##### chron.distribute.register

This topic is for initializing collections in ACE. It does not handle the
replication of files. We send only the necessary components to build a
collection. The depoistor and collection names correlate with the group and
collection names in ACE. Settings such as the directory, e-mail, and storage 
type are handled internally in each distributer. The token store will be
imported after the creation of the collection and used to validate files.

###### Message body  

| Key         | Value          | Type    | 
| ----------  | -------------- | ------- |
| depositor   | depositor name | String  |
| collection  | coll name      | String  | 
| token store | url            | String  | 
| audit.period| 182            | Integer |

##### chron.distribute.transfer

For the passing of files and to the depositor nodes. Every file gets its own
message, and the actual file transfers happen through HTTP. The filename sent 
will be the whole path on disk so that the subdirectories may be created. The
digest and digest type are used to validate the file when downloading.

###### Message body  

| Key         | Value          | Type   | 
| ----------  | ------------   | ------ |
| depositor   | depositor name | String |
| filename    | path/on/disk   | String | 
| digest-type | digest-type    | String | 
| digest      | digest of file | String | 

### Distribute <--> Distribute

##### chron.distribute.available

When dealing with healing, we want to ask the other nodes if they have files
available to be replicated.

| Key         | Value          | Type   |
| ----------- | -------------- | ------ |
| filename    | coll/path/file | String |
| digest      | sha256 digest  | String |
| digest-type | SHA-256        | String |

This will have a response which looks like

| Key         | Value          | Type    |
| ----------- | -------------- | ------- |
| url         | http://file    | String  |
| valid       | True/False     | Boolean |


Current open questions:  

1. Do we want to ask a node if it is ready to receive files?
    * This will cause the ingestion node to wait before sending off the 
    messages for each file in a new collection.

2. For topic names and exchange names, do we want to have a set [naming scheme][1]?
    * Would help to organize the flow of messages into a more logical pattern.

3. When replicating files would we want a topic chron.replicate.file.host?
    * Would allow each host to listen for files independent of others. On a
    successful collection init, the producer can easily route to the host w/ 
    the new routingKey.

Closed questions (can be reopened if needed): 

1. Should message headers be the same for all messages?
   * Yes

[1]: http://thoai-nguyen.blogspot.com/2012/05/rabbitmq-exchange-queue-name-convention.html
