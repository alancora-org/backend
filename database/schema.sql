CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(100) NOT NULL CHECK (char_length(trim(nome)) >= 2),
    email VARCHAR(254) NOT NULL UNIQUE CHECK (email = lower(trim(email))),
    senha_hash VARCHAR(255) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS moradia VARCHAR(120) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS localizacao_aproximada VARCHAR(120) NOT NULL DEFAULT '';

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS tipo_moradia VARCHAR(30) NOT NULL DEFAULT ''
        CHECK (tipo_moradia IN ('', 'CASA_TERREA', 'SOBRADO', 'APARTAMENTO', 'OUTRO'));

CREATE TABLE IF NOT EXISTS necessidade (
    codigo VARCHAR(50) PRIMARY KEY,
    nome VARCHAR(120) NOT NULL CHECK (char_length(trim(nome)) > 0)
);

CREATE TABLE IF NOT EXISTS usuario_necessidade (
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    necessidade_codigo VARCHAR(50) NOT NULL REFERENCES necessidade(codigo) ON DELETE RESTRICT,
    PRIMARY KEY (usuario_id, necessidade_codigo)
);

CREATE INDEX IF NOT EXISTS usuario_necessidade_codigo_idx ON usuario_necessidade (necessidade_codigo);

INSERT INTO necessidade (codigo, nome) VALUES
    ('MOBILIDADE_REDUZIDA', 'Mobilidade reduzida'),
    ('DEPENDENCIA_ENERGIA', 'Dependência de energia para equipamentos'),
    ('ASSISTENCIA_COMUNICACAO', 'Necessidade de apoio na comunicação'),
    ('DIFICULDADE_ESCADAS', 'Dificuldade para usar escadas'),
    ('APOIO_DESLOCAMENTO', 'Precisa de ajuda para se deslocar'),
    ('DIFICULDADE_CARREGAR_OBJETOS', 'Dificuldade para carregar objetos'),
    ('SEM_TRANSPORTE', 'Sem transporte disponível')
ON CONFLICT (codigo) DO NOTHING;

CREATE TABLE IF NOT EXISTS plano (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao_regras INTEGER NOT NULL CHECK (versao_regras > 0),
    emergencia VARCHAR(30) NOT NULL CHECK (emergencia IN ('ENCHENTE', 'INCENDIO', 'FALTA_ENERGIA')),
    nome VARCHAR(100) NOT NULL,
    moradia VARCHAR(120) NOT NULL,
    localizacao_aproximada VARCHAR(120) NOT NULL CHECK (char_length(trim(localizacao_aproximada)) > 0),
    necessidades TEXT[] NOT NULL
);

ALTER TABLE plano
    ADD COLUMN IF NOT EXISTS tipo_moradia VARCHAR(30) NOT NULL DEFAULT ''
        CHECK (tipo_moradia IN ('', 'CASA_TERREA', 'SOBRADO', 'APARTAMENTO', 'OUTRO'));

ALTER TABLE plano DROP CONSTRAINT IF EXISTS plano_moradia_check;

CREATE INDEX IF NOT EXISTS plano_usuario_id_idx ON plano (usuario_id, id DESC);

CREATE TABLE IF NOT EXISTS orientacao (
    plano_id BIGINT NOT NULL REFERENCES plano(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL CHECK (ordem > 0),
    regra VARCHAR(100) NOT NULL,
    motivo TEXT NOT NULL,
    texto TEXT NOT NULL,
    fonte TEXT NOT NULL,
    PRIMARY KEY (plano_id, ordem),
    UNIQUE (plano_id, regra)
);
