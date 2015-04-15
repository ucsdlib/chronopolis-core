-- POSTGRESQL schema

-- tables used by spring security
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    username varchar(256),
    password varchar(256),
    enabled boolean
);

DROP TABLE IF EXISTS authorities;
create table authorities (
    username varchar(256),
    authority varchar(256)
);

-- our entities
DROP TABLE IF EXISTS bag CASCADE;
DROP SEQUENCE IF EXISTS bag_id_seq;
CREATE SEQUENCE bag_id_seq;
CREATE TABLE bag (
    id bigint PRIMARY KEY DEFAULT nextval('bag_id_seq'),
    name varchar(255),
    depositor varchar(255),
    location varchar(255),
    token_location varchar(255),
    token_digest varchar(255),
    tag_manifest_digest varchar(255),
    status varchar(255),
    fixity_algorithm varchar(255),
    size bigint NOT NULL,
    total_files bigint NOT NULL,
    required_replications int
);

DROP TABLE IF EXISTS node CASCADE;
DROP SEQUENCE IF EXISTS node_id_seq;
CREATE SEQUENCE node_id_seq;
CREATE TABLE node (
    id bigint PRIMARY KEY DEFAULT nextval('node_id_seq'),
    enabled boolean,
    username varchar(255) UNIQUE,
    password varchar(255)
);

DROP TABLE IF EXISTS replication;
DROP SEQUENCE IF EXISTS replication_replicationid_seq;
CREATE SEQUENCE replication_replicationid_seq;
CREATE TABLE replication (
    id bigint PRIMARY KEY DEFAULT nextval('replication_replicationid_seq'),
    status varchar(255),
    bag_link varchar(255),
    token_link varchar(255),
    protocol varchar(255),
    received_tag_fixity varchar(255),
    received_token_fixity varchar(255),
    bag_id bigint,
    node_id bigint
);

DROP TABLE IF EXISTS restoration;
DROP SEQUENCE IF EXISTS restoration_restoration_id_seq;
CREATE SEQUENCE restoration_restoration_id_seq;
CREATE TABLE restoration (
    restoration_id bigint PRIMARY KEY DEFAULT nextval('restoration_restoration_id_seq'),
    depositor varchar(255),
    link varchar(255),
    name varchar(255),
    protocol varchar(255),
    status varchar(255),
    node_id bigint
);

DROP TABLE IF EXISTS ace_token;
DROP SEQUENCE IF EXISTS ace_token_id_seq;
CREATE SEQUENCE ace_token_id_seq;
CREATE TABLE ace_token (
    id bigint PRIMARY KEY DEFAULT nextval('ace_token_id_seq'),
    create_date timestamp,
    filename text,
    proof text,
    ims_service varchar(255),
    algorithm varchar(255),
    round bigint,
    bag bigint
);

DROP TABLE IF EXISTS bag_replications;
CREATE TABLE bag_replications (
    bag_id bigint,
    node_id bigint
);

ALTER TABLE replication
    ADD CONSTRAINT FK_repl_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE replication
    ADD CONSTRAINT FK_repl_node FOREIGN KEY (node_id) REFERENCES node;

ALTER TABLE restoration
    ADD CONSTRAINT FL_rest_node FOREIGN KEY (node_id) REFERENCES node;

ALTER TABLE ace_token
    ADD CONSTRAINT FK_token_bag FOREIGN KEY (bag) REFERENCES bag;

ALTER TABLE bag_replications
    ADD CONSTRAINT FK_br_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE bag_replications
    ADD CONSTRAINT FK_br_node FOREIGN KEY (node_id) REFERENCES node;
