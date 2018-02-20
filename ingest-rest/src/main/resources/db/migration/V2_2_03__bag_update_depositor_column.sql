ALTER TABLE bag ADD COLUMN depositor_id BIGINT;

INSERT INTO bag (depositor_id) SELECT d.id FROM bag
    JOIN depositor d ON bag.depositor = d.namesapce;

ALTER TABLE bag DROP COLUMN depositor;

ALTER TABLE bag
    ADD CONSTRAINT FK_bag_depositor FOREIGN KEY (depositor_id) REFERENCES depositor;