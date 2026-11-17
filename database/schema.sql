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
