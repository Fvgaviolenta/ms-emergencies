-- Centros de acopio vinculados a una emergencia (dominio ms-emergencies).
-- No reemplaza la tabla canonica `centros` de ms-resources: es modelo de coordinacion / demo de integracion.
CREATE TABLE centros_acopio_emergencia (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    emergencia_id       uuid         NOT NULL,
    nombre              varchar(200) NOT NULL,
    ubicacion           geography(Point, 4326) NOT NULL,
    capacidad_estimada  integer,
    creado_en           timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT fk_centros_acopio_emergencia_emergencia
        FOREIGN KEY (emergencia_id) REFERENCES emergencias (id) ON DELETE CASCADE,

    CONSTRAINT chk_centros_acopio_emergencia_capacidad
        CHECK (capacidad_estimada IS NULL OR capacidad_estimada > 0)
);

CREATE INDEX idx_centros_acopio_emergencia_emergencia
    ON centros_acopio_emergencia (emergencia_id);

CREATE INDEX idx_centros_acopio_emergencia_ubicacion_gist
    ON centros_acopio_emergencia USING GIST (ubicacion);

-- Registro idempotente de que el consumidor local proceso el evento emergency.created
CREATE TABLE registro_procesamiento_emergencia_creada (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    evento_id       uuid         NOT NULL,
    emergencia_id   uuid         NOT NULL,
    procesado_en    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_registro_proc_emergencia_creada_evento
        UNIQUE (evento_id),

    CONSTRAINT fk_registro_proc_emergencia
        FOREIGN KEY (emergencia_id) REFERENCES emergencias (id) ON DELETE CASCADE
);

CREATE INDEX idx_registro_proc_emergencia_emergencia
    ON registro_procesamiento_emergencia_creada (emergencia_id);
