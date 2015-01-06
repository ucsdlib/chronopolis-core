insert into users (username, password, enabled) values ('admin', 'admin', true);
insert into users (username, password, enabled) values ('umiacs', 'umiacs', true);
insert into users (username, password, enabled) values ('ncar', 'ncar', true);
insert into users (username, password, enabled) values ('sdsc', 'sdsc', true);
insert into users (username, password, enabled) values ('ucsd', 'ucsd', true);

insert into authorities (username, authority) values ('admin', 'ROLE_ADMIN');
insert into authorities (username, authority) values ('umiacs', 'ROLE_USER');
insert into authorities (username, authority) values ('ncar', 'ROLE_USER');
insert into authorities (username, authority) values ('sdsc', 'ROLE_USER');
insert into authorities (username, authority) values ('ucsd', 'ROLE_USER');
