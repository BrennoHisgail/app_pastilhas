-- =====================================================================
-- V4 - Prepara a tabela usuario para autenticacao de verdade (RF12, RF13)
--
-- atualizado_em entra para ficar simetrico com pastilha, que ja tem
-- esse campo. A estrutura de login/perfil em si (perfil, senha_hash,
-- ativo) ja veio pronta desde a V1.
-- =====================================================================

ALTER TABLE usuario ADD COLUMN atualizado_em TIMESTAMP;
