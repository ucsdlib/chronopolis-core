insert into users (username, password, enabled) values ('admin', '$2a$04$PLODVoI9PXF4cu9uk.Zng.vFQ8883O5FnF/GvlBGChVZh7Ne/3hem', true);
insert into users (username, password, enabled) values ('umiacs', '$2a$04$76rT9ZdZ0x4i..B.XDJDXeFrWjXIMBtQYTfTRA2pMhar7yhywJBAW', true);
insert into users (username, password, enabled) values ('ncar', '$2a$04$TXo2NmbY8WcAOAcIU6NZUuTBysa6/cZS.GU4capVVY/jC2EFjdQBu', true);
insert into users (username, password, enabled) values ('sdsc', '$2a$04$F19KKkMERGHCarAsNSxrU.RCrRjz9VyeeHth.rwr8.qffsboKwn5K', true);
insert into users (username, password, enabled) values ('ucsd', '$2a$04$K6i6BBtNX.cSHekw5.rG/.KbFhok8Qen4ExYYduTfKZWsyeDAvvRG', true);

insert into node (id, enabled, password, username) VALUES (DEFAULT, true, 'umiacs', 'umiacs');
insert into node (id, enabled, password, username) VALUES (DEFAULT, true, 'ncar', 'ncar');
insert into node (id, enabled, password, username) VALUES (DEFAULT, true, 'sdsc', 'sdsc');
insert into node (id, enabled, password, username) VALUES (DEFAULT, true, 'ucsd', 'ucsd');

insert into authorities (username, authority) values ('admin', 'ROLE_ADMIN');
insert into authorities (username, authority) values ('umiacs', 'ROLE_USER');
insert into authorities (username, authority) values ('ncar', 'ROLE_USER');
insert into authorities (username, authority) values ('sdsc', 'ROLE_USER');
insert into authorities (username, authority) values ('ucsd', 'ROLE_USER');

INSERT INTO storage_region VALUES(DEFAULT, 1, 'BAG', 'LOCAL', 1000000, '', CURRENT_DATE, CURRENT_DATE);
INSERT INTO storage_region VALUES(DEFAULT, 1, 'TOKEN', 'LOCAL', 1000000, '', CURRENT_DATE, CURRENT_DATE);