package com.xrayconnect.financeiro.repositorio;

import com.xrayconnect.financeiro.dto.MovimentacaoDiaria;
import com.xrayconnect.financeiro.dto.TotalPorTipoESituacao;
import com.xrayconnect.financeiro.entidade.LancamentoFinanceiro;
import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, Long> {

    boolean existsByCategoria_Id(Long categoriaId);

    @Query("""
            SELECT new com.xrayconnect.financeiro.dto.TotalPorTipoESituacao(
                l.tipoLancamento,
                l.situacaoLancamento,
                SUM(l.valor)
            )
            FROM LancamentoFinanceiro l
            GROUP BY l.tipoLancamento, l.situacaoLancamento
            """)
    List<TotalPorTipoESituacao> agruparTotaisPorTipoESituacao();

    @Query("""
            SELECT COUNT(l)
            FROM LancamentoFinanceiro l
            WHERE l.situacaoLancamento = :situacao
              AND l.dataVencimento < :hoje
            """)
    long contarVencidos(
            @Param("situacao") SituacaoLancamento situacao,
            @Param("hoje") LocalDate hoje
    );

    @Query("""
            SELECT new com.xrayconnect.financeiro.dto.MovimentacaoDiaria(
                l.dataLiquidacao,
                l.tipoLancamento,
                SUM(l.valor)
            )
            FROM LancamentoFinanceiro l
            WHERE l.situacaoLancamento = :situacao
              AND l.dataLiquidacao BETWEEN :inicio AND :fim
            GROUP BY l.dataLiquidacao, l.tipoLancamento
            ORDER BY l.dataLiquidacao
            """)
    List<MovimentacaoDiaria> somarMovimentacoesPorDia(
            @Param("situacao") SituacaoLancamento situacao,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim
    );
}
