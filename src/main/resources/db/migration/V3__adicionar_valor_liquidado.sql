ALTER TABLE lancamentos_financeiros
    ADD COLUMN valor_liquidado DECIMAL(15, 2) NULL;

UPDATE lancamentos_financeiros
    SET valor_liquidado = valor
    WHERE situacao = 'LIQUIDADO';

ALTER TABLE lancamentos_financeiros
    DROP CHECK ck_lancamentos_liquidacao_coerente;

