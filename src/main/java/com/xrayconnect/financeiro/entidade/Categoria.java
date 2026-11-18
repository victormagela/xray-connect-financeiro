package com.xrayconnect.financeiro.entidade;

import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "categorias")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoLancamento tipo;

    public Categoria(
            String nome,
            String descricao,
            TipoLancamento tipo
    ) {
        validarDados(nome, tipo);

        this.nome = nome;
        if (StringUtils.hasText(descricao)) {
            this.descricao = descricao;
        } else {
            this.descricao = null;
        }
        this.tipo = tipo;
    }

    public void atualizar(String nome, String descricao, TipoLancamento tipo) {
        validarDados(nome, tipo);

        this.nome = nome;
        if (StringUtils.hasText(descricao)) {
            this.descricao = descricao;
        } else {
            this.descricao = null;
        }
        this.tipo = tipo;
    }

    private void validarDados(String nome, TipoLancamento tipo) {
        if (!StringUtils.hasText(nome)) {
            throw new IllegalArgumentException("Nome da categoria não informado");
        }

        if (tipo == null) {
            throw new IllegalArgumentException("Tipo não informado!");
        }
    }
}
