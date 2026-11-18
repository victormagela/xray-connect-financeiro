package com.xrayconnect.financeiro.entidade;

import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "lancamentos_financeiros")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class LancamentoFinanceiro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "nome_contraparte", length = 120)
    private String nomeContraparte;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoLancamento tipoLancamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false, length = 10)
    private SituacaoLancamento situacaoLancamento;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_liquidacao")
    private LocalDate dataLiquidacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public LancamentoFinanceiro(
        Categoria categoria,
        String descricao,
        String nomeContraparte,
        BigDecimal valor,
        TipoLancamento tipoLancamento,
        LocalDate dataVencimento
    ) {
        if (categoria == null) {
            throw new IllegalArgumentException("Categoria não informada!");
        }

        if (!StringUtils.hasText(descricao)) {
            throw new IllegalArgumentException("Descrição não informada!");
        }

        if (valor == null) {
            throw new IllegalArgumentException("Valor não informado!");
        }
        else if (valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor não pode ser negativo ou zero!");
        }

        if (tipoLancamento == null) {
            throw new IllegalArgumentException("Tipo do lançamento não informado!");
        }

        if (categoria.getTipo() != tipoLancamento) {
            throw new IllegalArgumentException("Tipo do lançamento informado não bate com o tipo da categoria informada!");
        }

        if (dataVencimento == null) {
            throw new IllegalArgumentException("Data do vencimento não informada!");
        }

        this.categoria = categoria;
        this.descricao = descricao;
        if (StringUtils.hasText(nomeContraparte)) {
            this.nomeContraparte = nomeContraparte;
        }
        else {
            this.nomeContraparte = null;
        }
        this.valor = valor;
        this.tipoLancamento = tipoLancamento;
        this.situacaoLancamento = SituacaoLancamento.PENDENTE;
        this.dataVencimento = dataVencimento;
    }

    @PrePersist
    private void aoCriar() {
        LocalDateTime agora = LocalDateTime.now(ZoneOffset.UTC);

        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    @PreUpdate
    private void aoAtualizar() {this.atualizadoEm = LocalDateTime.now(ZoneOffset.UTC);}

    public void liquidar(LocalDate dataLiquidacao) {
        if (dataLiquidacao == null) {
            throw new IllegalArgumentException("Data de liquidação não informada!");
        }

        if (this.situacaoLancamento != SituacaoLancamento.PENDENTE) {
            throw new IllegalStateException(
                "Lançamento não está pendente. Apenas lançamentos pendentes podem ser liquidados"
            );
        }

        if (dataLiquidacao.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de liquidação não pode ser futura");
        }

        this.situacaoLancamento = SituacaoLancamento.LIQUIDADO;
        this.dataLiquidacao = dataLiquidacao;
    }

    public boolean estaVencida(LocalDate dataReferencia) {
        return this.situacaoLancamento == SituacaoLancamento.PENDENTE && dataReferencia.isAfter(this.dataVencimento);
    }
}
