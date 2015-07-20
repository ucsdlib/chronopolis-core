CREATE SEQUENCE bag_distribution_id_seq;
CREATE TABLE bag_distribution (
    id bigint PRIMARY KEY DEFAULT nextval('bag_distribution_id_seq'),
    bag_id bigint,
    node_id bigint,
    status varchar(255) -- DEFAULT 'DISTRIBUTE'
);

ALTER TABLE bag_distribution
    ADD CONSTRAINT FK_bd_bag FOREIGN KEY (bag_id) REFERENCES bag;

ALTER TABLE bag_distribution
    ADD CONSTRAINT FK_bd_node FOREIGN KEY (node_id) REFERENCES node;

-- migrate the old data
INSERT INTO bag_distribution (bag_id, node_id)
SELECT * FROM bag_replications;

-- we assume all previous entries are replications
UPDATE bag_distribution SET status = 'REPLICATE';

-- drop the constraints/old table
-- ALTER TABLE bag_replications
--     DROP CONSTRAINT FK_br_bag;

-- ALTER TABLE bag_replications
--     DROP CONSTRAINT FK_br_node;

DELETE FROM bag_replications;
DROP TABLE bag_replications;
