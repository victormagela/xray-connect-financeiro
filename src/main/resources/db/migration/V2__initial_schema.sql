CREATE TABLE categorias (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            nome VARCHAR(100) NOT NULL,
                            descricao VARCHAR(255) NULL,
                            tipo ENUM('RECEITA', 'DESPESA') NOT NULL,

                            CONSTRAINT pk_categorias PRIMARY KEY (id),
                            CONSTRAINT uk_categorias_tipo_nome UNIQUE (tipo, nome)
);

CREATE TABLE lancamentos_financeiros (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         id_categoria BIGINT NOT NULL,
                                         descricao VARCHAR(255) NOT NULL,
                                         nome_contraparte VARCHAR(120) NULL,
                                         valor DECIMAL(15, 2) NOT NULL,
                                         tipo ENUM('RECEITA', 'DESPESA') NOT NULL,
                                         situacao ENUM('PENDENTE', 'LIQUIDADO') NOT NULL,
                                         data_vencimento DATE NOT NULL,
                                         data_liquidacao DATE NULL,
                                         criado_em DATETIME(6) NOT NULL,
                                         atualizado_em DATETIME(6) NULL,

                                         CONSTRAINT pk_lancamentos_financeiros PRIMARY KEY (id),
                                         CONSTRAINT fk_lancamentos_categoria
                                             FOREIGN KEY (id_categoria) REFERENCES categorias(id)
                                                 ON UPDATE RESTRICT ON DELETE RESTRICT,
                                         CONSTRAINT ck_lancamentos_valor_positivo CHECK (valor > 0),
                                         CONSTRAINT ck_lancamentos_liquidacao_coerente CHECK (
                                             (situacao = 'PENDENTE' AND data_liquidacao IS NULL)
                                                 OR
                                             (situacao = 'LIQUIDADO' AND data_liquidacao IS NOT NULL)
                                             )
);
