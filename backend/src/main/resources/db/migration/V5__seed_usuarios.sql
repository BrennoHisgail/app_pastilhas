-- =====================================================================
-- V5 - Usuarios de demonstracao
--
-- Hashes BCrypt (forca 10, igual ao BCryptPasswordEncoder() padrao) das
-- senhas abaixo. BCrypt nao e determinístico por causa do salt, mas uma
-- vez gerado o hash e fixo: ele confere certo contra a senha em texto
-- puro sempre que alguem fizer login, porque a verificacao reconstroi
-- o calculo usando o salt que ja esta embutido no proprio hash.
--
-- admin@dda.com.br    / admin123
-- operador@dda.com.br / operador123
-- =====================================================================

INSERT INTO usuario (nome, email, senha_hash, perfil, ativo) VALUES
    ('Administradora Demo', 'admin@dda.com.br',
     '$2a$10$KJ7HdZeIMRG8lCd5Af2p..2.aKNONV3CP/wVdlvSOpCXxMHZ8lFe2', 'ADMIN', TRUE),
    ('Operador Demo', 'operador@dda.com.br',
     '$2a$10$2JyxBojUqu2fJVIwIoDjKeTx6J3XUz2Lp2u/Qz/XivyEG1qSEJo.O', 'OPERADOR', TRUE);
