-- =====================================================================
-- V2 - Massa de demonstracao.
-- Carga inicial de catalogo para o MVP ter dados no Swagger e na banca.
-- Alguns itens ja nascem abaixo do nivel minimo para exercitar o RF09.
-- A partir da sprint 2, saldo so muda por movimentacao (RF07).
-- =====================================================================
INSERT INTO pastilha (codigo, descricao, categoria, unidade_medida,
                      nivel_minimo, saldo_atual, custo_unitario,
                      lead_time_dias, estoque_seguranca) VALUES
 ('CNMG120408', 'Pastilha de torneamento CNMG 120408 metal duro',   'Torneamento', 'UN', 50,  120, 38.90, 10, 20),
 ('WNMG080408', 'Pastilha de torneamento WNMG 080408 revestida',    'Torneamento', 'UN', 40,   18, 42.50, 10, 15),
 ('DNMG150608', 'Pastilha de torneamento DNMG 150608 acabamento',   'Torneamento', 'UN', 30,   95, 51.00, 14, 12),
 ('APKT1604',   'Pastilha de fresamento APKT 1604 PDER',            'Fresamento',  'UN', 60,   45, 29.75, 12, 25),
 ('SEKT1204',   'Pastilha de fresamento SEKT 1204 AFN',             'Fresamento',  'UN', 60,  210, 31.20, 12, 25),
 ('TPKN2204',   'Pastilha de fresamento TPKN 2204 PDTR',            'Fresamento',  'UN', 25,   25, 34.10, 12, 10),
 ('TCMT110204', 'Pastilha de furacao TCMT 110204 interna',          'Furacao',     'UN', 20,    8, 46.80, 21,  8),
 ('SPMG070204', 'Pastilha de furacao SPMG 070204 broca U',          'Furacao',     'UN', 20,   64, 55.40, 21,  8),
 ('N123H2',     'Pastilha de sangramento N123H2 0400',              'Sangramento', 'UN', 15,   12, 78.90, 18,  6),
 ('MGMN300',    'Pastilha de sangramento MGMN 300-M largura 3mm',   'Sangramento', 'UN', 15,   40, 24.60, 18,  6),
 ('16ER-AG60',  'Pastilha de rosqueamento 16ER AG60 externa',       'Rosqueamento','UN', 10,    4, 62.30, 25,  4),
 ('16IR-AG60',  'Pastilha de rosqueamento 16IR AG60 interna',       'Rosqueamento','UN', 10,   22, 64.90, 25,  4);
