-- =====================================================================
-- V3 - Controle de concorrencia otimista na pastilha
--
-- Sem isso, um UPDATE feito fora do fluxo de movimentacao (inativar,
-- reativar, editar cadastro, mudar nivel minimo) sobrescreve TODAS as
-- colunas da linha com os valores que tinha em memoria no momento da
-- leitura - inclusive o saldo_atual, que pode ter mudado por uma
-- entrada/saida concorrente protegida pelo lock da movimentacao.
-- A coluna versao da ao Hibernate como detectar e rejeitar esse
-- UPDATE desatualizado (optimistic locking) em vez de deixar passar.
-- =====================================================================

ALTER TABLE pastilha ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
