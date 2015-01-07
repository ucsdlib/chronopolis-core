create table users (
   username varchar(256),
   password varchar(256),
   enabled boolean
);

create table authorities (
  username varchar(256),
  authority varchar(256)
);

-- TODO: Add all our entities and disable ddl generation in our app.properties