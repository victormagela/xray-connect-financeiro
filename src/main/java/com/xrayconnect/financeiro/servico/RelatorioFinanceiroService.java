package com.xrayconnect.financeiro.servico;

import com.xrayconnect.financeiro.dto.*;
import com.xrayconnect.financeiro.enumeracao.*;
import com.xrayconnect.financeiro.repositorio.LancamentoFinanceiroRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RelatorioFinanceiroService {

    private final LancamentoFinanceiroRepository repositorio;

    @Transactional(readOnly = true)
    public DashboardResponse obterDashboard() {

        BigDecimal totalAReceber = BigDecimal.ZERO;
        BigDecimal totalAPagar = BigDecimal.ZERO;
        BigDecimal totalRecebido = BigDecimal.ZERO;
        BigDecimal totalPago = BigDecimal.ZERO;

        var totais = repositorio.agruparTotaisPorTipoESituacao();

        for (var item : totais) {

            if (item.tipo() == TipoLancamento.RECEITA) {
                if (item.situacao() == SituacaoLancamento.PENDENTE) {
                    totalAReceber = item.total();
                } else if (item.situacao() == SituacaoLancamento.LIQUIDADO) {
                    totalRecebido = item.total();
                }
            }

            if (item.tipo() == TipoLancamento.DESPESA) {
                if (item.situacao() == SituacaoLancamento.PENDENTE) {
                    totalAPagar = item.total();
                } else if (item.situacao() == SituacaoLancamento.LIQUIDADO) {
                    totalPago = item.total();
                }
            }
        }

        BigDecimal saldoRealizado = totalRecebido.subtract(totalPago);

        long quantidadeVencidos = repositorio.contarVencidos(
                SituacaoLancamento.PENDENTE,
                LocalDate.now()
        );

        return new DashboardResponse(
                totalAReceber,
                totalAPagar,
                totalRecebido,
                totalPago,
                saldoRealizado,
                quantidadeVencidos
        );
    }

    @Transactional(readOnly = true)
    public FluxoCaixaResponse obterFluxoCaixa(
            LocalDate inicio,
            LocalDate fim
    ) {
        validarPeriodo(inicio, fim);

        var movimentacoes = repositorio.somarMovimentacoesPorDia(
                SituacaoLancamento.LIQUIDADO,
                inicio,
                fim
        );

        return montarFluxoCaixa(inicio, fim, movimentacoes);
    }

    @Transactional(readOnly = true)
    public FluxoCaixaResponse obterFluxoCaixaProjetado(
            LocalDate inicio,
            LocalDate fim
    ) {
        validarPeriodo(inicio, fim);

        var movimentacoes = repositorio.somarMovimentacoesProjetadasPorDia(
                SituacaoLancamento.PENDENTE,
                inicio,
                fim
        );

        return montarFluxoCaixa(inicio, fim, movimentacoes);
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {

        if (inicio == null || fim == null) {
            throw new IllegalArgumentException(
                    "O período inicial e final deve ser informado."
            );
        }

        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException(
                    "A data inicial não pode ser posterior à data final."
            );
        }
    }

    private FluxoCaixaResponse montarFluxoCaixa(
            LocalDate inicio,
            LocalDate fim,
            List<MovimentacaoDiaria> movimentacoes
    ) {

        Map<YearMonth, BigDecimal> receitasPorMes = new HashMap<>();
        Map<YearMonth, BigDecimal> despesasPorMes = new HashMap<>();

        for (var movimentacao : movimentacoes) {

            YearMonth mes = YearMonth.from(movimentacao.data());

            if (movimentacao.tipo() == TipoLancamento.RECEITA) {

                receitasPorMes.merge(
                        mes,
                        movimentacao.total(),
                        BigDecimal::add
                );

            } else {

                despesasPorMes.merge(
                        mes,
                        movimentacao.total(),
                        BigDecimal::add
                );
            }
        }

        List<ResumoMensalResponse> meses = new ArrayList<>();

        BigDecimal totalReceitas = BigDecimal.ZERO;
        BigDecimal totalDespesas = BigDecimal.ZERO;

        YearMonth mesAtual = YearMonth.from(inicio);
        YearMonth ultimoMes = YearMonth.from(fim);

        while (!mesAtual.isAfter(ultimoMes)) {

            BigDecimal receitas = receitasPorMes.getOrDefault(
                    mesAtual,
                    BigDecimal.ZERO
            );

            BigDecimal despesas = despesasPorMes.getOrDefault(
                    mesAtual,
                    BigDecimal.ZERO
            );

            BigDecimal saldo = receitas.subtract(despesas);

            meses.add(new ResumoMensalResponse(
                    mesAtual.toString(),
                    receitas,
                    despesas,
                    saldo
            ));

            totalReceitas = totalReceitas.add(receitas);
            totalDespesas = totalDespesas.add(despesas);

            mesAtual = mesAtual.plusMonths(1);
        }

        BigDecimal saldoPeriodo = totalReceitas.subtract(totalDespesas);

        return new FluxoCaixaResponse(
                inicio,
                fim,
                totalReceitas,
                totalDespesas,
                saldoPeriodo,
                meses
        );
    }
}