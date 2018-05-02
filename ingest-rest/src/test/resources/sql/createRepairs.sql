-- create a bag
INSERT INTO bag VALUES(1, CURRENT_DATE, CURRENT_DATE, 'bag-0', 'admin', 1, 'bags/test-bag-0', 'tokens/test-bag-0', 'token-fixity', 'tag-fixity', 'DEPOSITED', 'SHA-256', 1500, 5, 3);

-- create a fulfillment strategy for one of our fulfillments
--                          id, api-key,        url,                        link, type
INSERT INTO strategy VALUES(1, 'mock-api-key', 'http://some-ace-url/ace-am', NULL, 'ACE');

-- create repair requests
--                        id created_at    updated_at  audit     status       requester  to_node from_node bag_id cleaned replaced validated type strategy_id
INSERT INTO repair VALUES(1, CURRENT_DATE, CURRENT_DATE, 'PRE', 'STAGING',    'ucsd',    4,      1,        1,     FALSE,  FALSE,   FALSE,    NULL, NULL);
INSERT INTO repair VALUES(2, CURRENT_DATE, CURRENT_DATE, 'PRE', 'REQUESTED',  'ncar',    2,      NULL,     1,     FALSE,  FALSE,   FALSE,    NULL, NULL);
INSERT INTO repair VALUES(3, CURRENT_DATE, CURRENT_DATE, 'PRE', 'READY',      'umiacs',  1,      2,        1,     FALSE,  FALSE,   FALSE,    'ACE', 1);
