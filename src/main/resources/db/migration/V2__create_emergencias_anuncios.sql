-- =====================================================
-- Tabla: emergencias
-- Dominio: Coordinacion de Emergencias (ms-emergencies)
-- =====================================================
CREATE TABLE emergencias (
    id                          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo                        varchar(50)  NOT NULL,
    severidad                   varchar(20)  NOT NULL,
    region                      varchar(100) NOT NULL,
    estado                      varchar(20)  NOT NULL DEFAULT 'ACTIVA',
    coordenadas_epicentro       geography(POINT, 4326),
    zona_impacto                geography(POLYGON, 4326),
    declarada_por_usuario_id    uuid         NOT NULL,
    declarada_en                timestamptz  NOT NULL DEFAULT now(),
    actualizada_en              timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT chk_emergencias_tipo CHECK (
        tipo IN ('TERREMOTO','TSUNAMI','INCENDIO','INUNDACION','ERUPCION','ALUVION')
    ),
    CONSTRAINT chk_emergencias_severidad CHECK (
        severidad IN ('BAJA','MEDIA','ALTA','CATASTROFICA')
    ),
    CONSTRAINT chk_emergencias_estado CHECK (
        estado IN ('ACTIVA','CONTROLADA','FINALIZADA')
    )
);

-- Indices GIST obligatorios para consultas geoespaciales
CREATE INDEX idx_emergencias_epicentro_gist
    ON emergencias USING GIST (coordenadas_epicentro);

CREATE INDEX idx_emergencias_zona_impacto_gist
    ON emergencias USING GIST (zona_impacto);

-- Indice para filtrar rapido las activas ordenadas por fecha
CREATE INDEX idx_emergencias_estado_declarada_en
    ON emergencias (estado, declarada_en DESC);


-- =====================================================
-- Tabla: anuncios
-- =====================================================
CREATE TABLE anuncios (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    emergencia_id   uuid         NOT NULL,
    autor_usuario_id uuid        NOT NULL,
    titulo          varchar(200) NOT NULL,
    contenido       text         NOT NULL,
    severidad       varchar(20)  NOT NULL,
    alcance         varchar(20)  NOT NULL,
    region          varchar(100),
    vigente_desde   timestamptz  NOT NULL,
    vigente_hasta   timestamptz,
    creado_en       timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT fk_anuncios_emergencia
        FOREIGN KEY (emergencia_id) REFERENCES emergencias (id),

    CONSTRAINT chk_anuncios_severidad CHECK (
        severidad IN ('INFORMATIVO','IMPORTANTE','URGENTE','EMERGENCIA')
    ),
    CONSTRAINT chk_anuncios_alcance CHECK (
        alcance IN ('NACIONAL','REGIONAL','COMUNAL')
    ),
    CONSTRAINT chk_anuncios_vigencia CHECK (
        vigente_hasta IS NULL OR vigente_hasta > vigente_desde
    )
);

-- Indice para listar anuncios activos por severidad y fecha de vigencia
CREATE INDEX idx_anuncios_severidad_vigencia
    ON anuncios (severidad, vigente_desde DESC);

-- Indice para buscar anuncios asociados a una emergencia
CREATE INDEX idx_anuncios_emergencia_id
    ON anuncios (emergencia_id);
