#!/bin/bash
set -e
export PGPASSWORD=$POSTGRES_PASSWORD;
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  CREATE USER $APP_DB_USER WITH PASSWORD '$APP_DB_PASS';
  CREATE DATABASE $APP_DB_NAME;
  GRANT CONNECT ON DATABASE $APP_DB_NAME TO $APP_DB_USER;
  
  \connect $APP_DB_NAME $POSTGRES_USER
  BEGIN;
    CREATE SCHEMA $APP_DB_SCHEMA;
    GRANT USAGE ON SCHEMA $APP_DB_SCHEMA TO $APP_DB_USER;

    CREATE TABLE IF NOT EXISTS $APP_DB_SCHEMA.users (
      username VARCHAR(15) PRIMARY KEY NOT NULL,
      email VARCHAR(128) NOT NULL,
      personal_name VARCHAR(64) NOT NULL,
      user_password VARCHAR(256) NOT NULL,
      date_created TIMESTAMP NOT NULL,
      public_key VARCHAR(2048) NOT NULL,
      private_key VARCHAR(2048) NOT NULL
    );

    CREATE TABLE IF NOT EXISTS $APP_DB_SCHEMA.files (
	    id VARCHAR(36) PRIMARY KEY NOT NULL,
      directory BOOLEAN NOT NULL,
      file_name VARCHAR(64) NOT NULL,
      active BOOLEAN NOT NULL,
      parent_id VARCHAR(36) NOT NULL,
      date_modified TIMESTAMP NOT NULL,
      owner_id VARCHAR(15) NOT NULL,

      CONSTRAINT fk_user
        FOREIGN KEY(owner_id)
        REFERENCES $APP_DB_SCHEMA.users (username)
    );

    ALTER TABLE $APP_DB_SCHEMA.files 
      ADD CONSTRAINT fk_parent
        FOREIGN KEY(parent_id)
        REFERENCES $APP_DB_SCHEMA.files (id);

    CREATE INDEX idx_file_owner_id ON $APP_DB_SCHEMA.files (owner_id); 
    CREATE INDEX idx_file_parent_id ON $APP_DB_SCHEMA.files (parent_id); 
    CREATE INDEX idx_user_email ON $APP_DB_SCHEMA.users (email);
    

    GRANT SELECT , INSERT , UPDATE , DELETE ON TABLE $APP_DB_SCHEMA.files TO $APP_DB_USER;
    GRANT SELECT , INSERT , UPDATE , DELETE ON TABLE $APP_DB_SCHEMA.users TO $APP_DB_USER;

  COMMIT;
EOSQL