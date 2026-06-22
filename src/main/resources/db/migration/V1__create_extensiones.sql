-- Extension PostGIS necesaria para columnas geography(POINT) y geography(POLYGON)
CREATE EXTENSION IF NOT EXISTS postgis;

-- Extension para generacion de UUIDs (gen_random_uuid)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
