-- just doing a naive update on the ace_token tables take a tremendous amount of time, likely
-- because of the indexes which need updated. to mitigate this we take a few extra steps:
-- 1: create a new table for ace_tokens (ace_token_migration)
-- 2: insert into the new table from joining the ace_token table with the file table
--    this takes roughly as long as the select
-- 3: drop the old ace_tokens table
-- 4: rename the ace_token_migration table
-- 5: add constraints and indexes

-- 1
CREATE TABLE ace_token_migration(
    id bigint default nextval('ace_token_id_seq'),
    bag_id bigint, -- note the change from bag -> bag_id
    file_id bigint,
    round bigint,
    ims_host varchar(255),
    ims_service varchar(255),
    algorithm varchar(255),
    proof text,
    create_date timestamp
);

-- 2
INSERT INTO ace_token_migration(id, bag_id, file_id, round, ims_host, ims_service, algorithm, proof, create_Date)
    SELECT a.id, a.bag, f.id, a.round, a.ims_host, a.ims_service, a.algorithm, a.proof, a.create_date
    FROM ace_token a INNER JOIN file f ON a.filename = f.filename AND a.bag = f.bag_id;

-- 3/4
DROP TABLE ace_token;
ALTER TABLE ace_token_migration RENAME TO ace_token;
