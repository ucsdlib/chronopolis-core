-- create a unique index on (ace_token.bag_id, ace_token.file_id) to prevent duplicate tokens
-- this isn't really ideal since we _could_ have multiple tokens for files but that isn't
-- supported at the moment so let's worry about it when we get there and protect our api for now

CREATE UNIQUE INDEX CONCURRENTLY idx_ace_token_to_file_id ON ace_token (bag_id, file_id);
