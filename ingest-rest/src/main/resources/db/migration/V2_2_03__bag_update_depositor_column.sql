ALTER TABLE bag ADD COLUMN depositor_id BIGINT;

UPDATE bag SET depositor_id = d.id FROM depositor d WHERE bag.depositor = d.namespace;

ALTER TABLE bag DROP COLUMN depositor;

ALTER TABLE bag
    ADD CONSTRAINT FK_bag_depositor FOREIGN KEY (depositor_id) REFERENCES depositor;