-- =====================================================================
-- V1 - Schema inicial do controle de estoque de pastilhas industriais
-- Projeto Aplicado III | ADS - UniSENAI | DDA Metalurgica
-- =====================================================================

-- ---------------------------------------------------------------------
-- usuario (RF12, RF13) - as regras de acesso entram na sprint 4 (09/10).
-- A tabela ja nasce aqui porque movimentacao referencia o responsavel.
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id         BIGSERIAL    PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(160) NOT NULL UNIQUE,
    senha_hash VARCHAR(100) NOT NULL,          -- BCrypt (RNF05), nunca texto puro
    perfil     VARCHAR(20)  NOT NULL,
    ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'OPERADOR'))
);

-- ---------------------------------------------------------------------
-- pastilha (RF01 a RF04, RF08)
-- lead_time_dias e estoque_seguranca alimentam o Ponto de Pedido
-- PP = (d x t) + Qs, conforme material da disciplina (sprint 3).
-- ---------------------------------------------------------------------
CREATE TABLE pastilha (
    id                BIGSERIAL     PRIMARY KEY,
    codigo            VARCHAR(40)   NOT NULL UNIQUE,
    descricao         VARCHAR(200)  NOT NULL,
    categoria         VARCHAR(80)   NOT NULL,
    unidade_medida    VARCHAR(10)   NOT NULL,
    nivel_minimo      NUMERIC(14,3) NOT NULL DEFAULT 0,
    saldo_atual       NUMERIC(14,3) NOT NULL DEFAULT 0,
    custo_unitario    NUMERIC(14,2) NOT NULL DEFAULT 0,
    lead_time_dias    INTEGER       NOT NULL DEFAULT 0,
    estoque_seguranca NUMERIC(14,3) NOT NULL DEFAULT 0,
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em         TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em     TIMESTAMP,

    -- RNF14: o banco e a ultima linha de defesa contra saldo negativo.
    CONSTRAINT ck_pastilha_saldo_nao_negativo CHECK (saldo_atual >= 0),
    CONSTRAINT ck_pastilha_nivel_minimo       CHECK (nivel_minimo >= 0),
    CONSTRAINT ck_pastilha_estoque_seguranca  CHECK (estoque_seguranca >= 0),
    CONSTRAINT ck_pastilha_custo_unitario     CHECK (custo_unitario >= 0),
    CONSTRAINT ck_pastilha_lead_time          CHECK (lead_time_dias >= 0),
    CONSTRAINT ck_pastilha_unidade_medida     CHECK (unidade_medida IN ('UN','PC','CX','KG','M'))
);

CREATE INDEX idx_pastilha_categoria ON pastilha (categoria);
CREATE INDEX idx_pastilha_ativo     ON pastilha (ativo);

-- ---------------------------------------------------------------------
-- movimentacao (RF05, RF06, RF07, RF10) - sprint 2 (23/09)
-- saldo_apos guarda o saldo resultante de cada lancamento: e o que permite
-- auditar/reconciliar o saldo_atual desnormalizado da pastilha.
-- ---------------------------------------------------------------------
CREATE TABLE movimentacao (
    id             BIGSERIAL     PRIMARY KEY,
    pastilha_id    BIGINT        NOT NULL REFERENCES pastilha (id),
    tipo           VARCHAR(10)   NOT NULL,
    quantidade     NUMERIC(14,3) NOT NULL,
    saldo_apos     NUMERIC(14,3) NOT NULL,
    data_hora      TIMESTAMP     NOT NULL DEFAULT NOW(),
    usuario_id     BIGINT        REFERENCES usuario (id),
    origem_destino VARCHAR(160),
    documento      VARCHAR(60),
    observacao     VARCHAR(400),

    CONSTRAINT ck_movimentacao_tipo       CHECK (tipo IN ('ENTRADA', 'SAIDA', 'AJUSTE')),
    CONSTRAINT ck_movimentacao_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_movimentacao_saldo_apos CHECK (saldo_apos >= 0)
);

CREATE INDEX idx_movimentacao_pastilha_data ON movimentacao (pastilha_id, data_hora DESC);
CREATE INDEX idx_movimentacao_data          ON movimentacao (data_hora DESC);
