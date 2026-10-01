#!/bin/sh
# Executado uma única vez pelo container do PostgreSQL, na criação do volume de dados.
# Separa privilégios (ESPECIFICACAO §49/§77):
#   - DB_OWNER_USER: dono do schema, usado somente pelo Flyway;
#   - DB_APP_USER:   usado pela aplicação, apenas DML nas tabelas do schema.
set -eu

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v owner_user="$DB_OWNER_USER" \
  -v owner_password="$DB_OWNER_PASSWORD" \
  -v app_user="$DB_APP_USER" \
  -v app_password="$DB_APP_PASSWORD" \
  -v db_name="$POSTGRES_DB" <<'SQL'
CREATE ROLE :"owner_user" LOGIN PASSWORD :'owner_password';
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password';

REVOKE ALL ON DATABASE :"db_name" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db_name" TO :"owner_user", :"app_user";

REVOKE ALL ON SCHEMA public FROM PUBLIC;

CREATE SCHEMA app AUTHORIZATION :"owner_user";
GRANT USAGE ON SCHEMA app TO :"app_user";

ALTER DEFAULT PRIVILEGES FOR ROLE :"owner_user" IN SCHEMA app
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO :"app_user";
ALTER DEFAULT PRIVILEGES FOR ROLE :"owner_user" IN SCHEMA app
  GRANT USAGE, SELECT ON SEQUENCES TO :"app_user";

ALTER ROLE :"owner_user" SET search_path = app;
ALTER ROLE :"app_user" SET search_path = app;
SQL
