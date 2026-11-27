import com.xrayconnect.financeiro.dto.MovimentacaoDiaria;
import com.xrayconnect.financeiro.dto.TotalPorTipoESituacao;
import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import com.xrayconnect.financeiro.repositorio.LancamentoFinanceiroRepository;
import com.xrayconnect.financeiro.servico.RelatorioFinanceiroService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RelatorioFinanceiroServiceTest {

    @Mock
    private LancamentoFinanceiroRepository repositorio;

    @InjectMocks
    private RelatorioFinanceiroService servico;

    private static BigDecimal valor(String numero) {
        return new BigDecimal(numero);
    }

    // Testes do Dashboard

    @Test
    public void dadoTotaisPorTipoESituacao_AoObterDashboard_DeveCalcularIndicadores() {
        // Arrange
        var totais = List.of(
                new TotalPorTipoESituacao(TipoLancamento.RECEITA, SituacaoLancamento.PENDENTE, valor("800.00")),
                new TotalPorTipoESituacao(TipoLancamento.DESPESA, SituacaoLancamento.PENDENTE, valor("400.00")),
                new TotalPorTipoESituacao(TipoLancamento.RECEITA, SituacaoLancamento.LIQUIDADO, valor("950.00")),
                new TotalPorTipoESituacao(TipoLancamento.DESPESA, SituacaoLancamento.LIQUIDADO, valor("540.00"))
        );

        when(repositorio.agruparTotaisPorTipoESituacao()).thenReturn(totais);
        when(repositorio.contarVencidos(eq(SituacaoLancamento.PENDENTE), any(LocalDate.class)))
                .thenReturn(2L);

        // Act
        var resultado = servico.obterDashboard();

        // Assert
        assertAll(
                () -> assertEquals(valor("800.00"), resultado.totalAReceber()),
                () -> assertEquals(valor("400.00"), resultado.totalAPagar()),
                () -> assertEquals(valor("950.00"), resultado.totalRecebido()),
                () -> assertEquals(valor("540.00"), resultado.totalPago()),
                () -> assertEquals(valor("410.00"), resultado.saldoRealizado()),
                () -> assertEquals(2L, resultado.quantidadeVencidos())
        );

        verify(repositorio).agruparTotaisPorTipoESituacao();
        verify(repositorio).contarVencidos(eq(SituacaoLancamento.PENDENTE), any(LocalDate.class));
    }

    @Test
    public void dadoNenhumLancamento_AoObterDashboard_DeveRetornarTotaisZerados() {
        // Arrange
        when(repositorio.agruparTotaisPorTipoESituacao()).thenReturn(List.of());

        // Act
        var resultado = servico.obterDashboard();

        // Assert
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, resultado.totalAReceber()),
                () -> assertEquals(BigDecimal.ZERO, resultado.totalAPagar()),
                () -> assertEquals(BigDecimal.ZERO, resultado.totalRecebido()),
                () -> assertEquals(BigDecimal.ZERO, resultado.totalPago()),
                () -> assertEquals(BigDecimal.ZERO, resultado.saldoRealizado()),
                () -> assertEquals(0L, resultado.quantidadeVencidos())
        );
    }

    @Test
    public void dadoDespesasLiquidadasMaioresQueReceitas_AoObterDashboard_DeveRetornarSaldoNegativo() {
        // Arrange
        var totais = List.of(
                new TotalPorTipoESituacao(TipoLancamento.RECEITA, SituacaoLancamento.LIQUIDADO, valor("400.00")),
                new TotalPorTipoESituacao(TipoLancamento.DESPESA, SituacaoLancamento.LIQUIDADO, valor("900.00"))
        );
        when(repositorio.agruparTotaisPorTipoESituacao()).thenReturn(totais);

        // Act
        var resultado = servico.obterDashboard();

        // Assert
        assertAll(
                () -> assertEquals(valor("-500.00"), resultado.saldoRealizado()),
                () -> assertEquals(BigDecimal.ZERO, resultado.totalAReceber()),
                () -> assertEquals(BigDecimal.ZERO, resultado.totalAPagar())
        );
    }

    // Testes do Fluxo de Caixa realizado

    @Test
    public void dadoMovimentacoesEmMesesDistintos_AoObterFluxoRealizado_DeveAgruparECalcularTotais() {
        // Arrange: dezembro nao possui movimentacoes; o periodo atravessa a virada de ano.
        var inicio = LocalDate.of(2026, 11, 1);
        var fim = LocalDate.of(2027, 1, 31);
        var movimentacoes = List.of(
                new MovimentacaoDiaria(LocalDate.of(2026, 11, 3), TipoLancamento.RECEITA, valor("900.00")),
                new MovimentacaoDiaria(LocalDate.of(2026, 11, 25), TipoLancamento.RECEITA, valor("100.00")),
                new MovimentacaoDiaria(LocalDate.of(2026, 11, 25), TipoLancamento.DESPESA, valor("200.00")),
                new MovimentacaoDiaria(LocalDate.of(2027, 1, 10), TipoLancamento.RECEITA, valor("300.00")),
                new MovimentacaoDiaria(LocalDate.of(2027, 1, 20), TipoLancamento.DESPESA, valor("500.00"))
        );

        when(repositorio.somarMovimentacoesPorDia(SituacaoLancamento.LIQUIDADO, inicio, fim))
                .thenReturn(movimentacoes);

        // Act
        var resultado = servico.obterFluxoCaixa(inicio, fim);

        // Assert
        assertAll(
                () -> assertEquals(inicio, resultado.inicio()),
                () -> assertEquals(fim, resultado.fim()),
                () -> assertEquals(valor("1300.00"), resultado.totalReceitas()),
                () -> assertEquals(valor("700.00"), resultado.totalDespesas()),
                () -> assertEquals(valor("600.00"), resultado.saldoPeriodo()),
                () -> assertEquals(3, resultado.meses().size())
        );

        var novembro = resultado.meses().get(0);
        var dezembro = resultado.meses().get(1);
        var janeiro = resultado.meses().get(2);

        assertAll(
                () -> assertEquals("2026-11", novembro.mes()),
                () -> assertEquals(valor("1000.00"), novembro.receitas()),
                () -> assertEquals(valor("200.00"), novembro.despesas()),
                () -> assertEquals(valor("800.00"), novembro.saldo()),

                () -> assertEquals("2026-12", dezembro.mes()),
                () -> assertEquals(BigDecimal.ZERO, dezembro.receitas()),
                () -> assertEquals(BigDecimal.ZERO, dezembro.despesas()),
                () -> assertEquals(BigDecimal.ZERO, dezembro.saldo()),

                () -> assertEquals("2027-01", janeiro.mes()),
                () -> assertEquals(valor("300.00"), janeiro.receitas()),
                () -> assertEquals(valor("500.00"), janeiro.despesas()),
                () -> assertEquals(valor("-200.00"), janeiro.saldo())
        );

        verify(repositorio).somarMovimentacoesPorDia(SituacaoLancamento.LIQUIDADO, inicio, fim);
        verifyNoMoreInteractions(repositorio);
    }

    @Test
    public void dadoApenasReceitas_AoObterFluxoRealizado_DeveManterDespesasZeradas() {
        // Arrange
        var inicio = LocalDate.of(2026, 11, 1);
        var fim = LocalDate.of(2026, 11, 30);

        when(repositorio.somarMovimentacoesPorDia(SituacaoLancamento.LIQUIDADO, inicio, fim))
                .thenReturn(List.of(
                        new MovimentacaoDiaria(LocalDate.of(2026, 11, 10), TipoLancamento.RECEITA, valor("350.00"))
                ));

        // Act
        var resultado = servico.obterFluxoCaixa(inicio, fim);

        // Assert
        assertAll(
                () -> assertEquals(valor("350.00"), resultado.totalReceitas()),
                () -> assertEquals(BigDecimal.ZERO, resultado.totalDespesas()),
                () -> assertEquals(valor("350.00"), resultado.saldoPeriodo()),
                () -> assertEquals(BigDecimal.ZERO, resultado.meses().get(0).despesas())
        );
    }

    @Test
    public void dadoNenhumaMovimentacao_AoObterFluxoRealizado_DeveRetornarMesesZerados() {
        // Arrange
        var inicio = LocalDate.of(2026, 11, 1);
        var fim = LocalDate.of(2026, 12, 31);

        when(repositorio.somarMovimentacoesPorDia(SituacaoLancamento.LIQUIDADO, inicio, fim))
                .thenReturn(List.of());

        // Act
        var resultado = servico.obterFluxoCaixa(inicio, fim);

        // Assert
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, resultado.totalReceitas()),
                () -> assertEquals(BigDecimal.ZERO, resultado.totalDespesas()),
                () -> assertEquals(BigDecimal.ZERO, resultado.saldoPeriodo()),
                () -> assertEquals(2, resultado.meses().size()),
                () -> assertEquals("2026-11", resultado.meses().get(0).mes()),
                () -> assertEquals("2026-12", resultado.meses().get(1).mes()),
                () -> assertTrue(resultado.meses().stream().allMatch(mes ->
                        mes.receitas().compareTo(BigDecimal.ZERO) == 0
                                && mes.despesas().compareTo(BigDecimal.ZERO) == 0
                                && mes.saldo().compareTo(BigDecimal.ZERO) == 0
                ))
        );
    }

    // Testes do Fluxo de Caixa projetado

    @Test
    public void dadoMovimentacoesPendentes_AoObterFluxoProjetado_DeveCalcularTotaisPorVencimento() {
        // Arrange
        var inicio = LocalDate.of(2026, 12, 1);
        var fim = LocalDate.of(2027, 2, 28);
        var movimentacoes = List.of(
                new MovimentacaoDiaria(LocalDate.of(2026, 12, 15), TipoLancamento.RECEITA, valor("800.00")),
                new MovimentacaoDiaria(LocalDate.of(2026, 12, 15), TipoLancamento.DESPESA, valor("300.00")),
                new MovimentacaoDiaria(LocalDate.of(2027, 1, 5), TipoLancamento.DESPESA, valor("100.00")),
                new MovimentacaoDiaria(LocalDate.of(2027, 2, 20), TipoLancamento.RECEITA, valor("1000.00"))
        );

        when(repositorio.somarMovimentacoesProjetadasPorDia(SituacaoLancamento.PENDENTE, inicio, fim))
                .thenReturn(movimentacoes);

        // Act
        var resultado = servico.obterFluxoCaixaProjetado(inicio, fim);

        // Assert
        assertAll(
                () -> assertEquals(valor("1800.00"), resultado.totalReceitas()),
                () -> assertEquals(valor("400.00"), resultado.totalDespesas()),
                () -> assertEquals(valor("1400.00"), resultado.saldoPeriodo()),
                () -> assertEquals(3, resultado.meses().size())
        );

        var dezembro = resultado.meses().get(0);
        var janeiro = resultado.meses().get(1);
        var fevereiro = resultado.meses().get(2);

        assertAll(
                () -> assertEquals("2026-12", dezembro.mes()),
                () -> assertEquals(valor("800.00"), dezembro.receitas()),
                () -> assertEquals(valor("300.00"), dezembro.despesas()),
                () -> assertEquals(valor("500.00"), dezembro.saldo()),

                () -> assertEquals("2027-01", janeiro.mes()),
                () -> assertEquals(BigDecimal.ZERO, janeiro.receitas()),
                () -> assertEquals(valor("100.00"), janeiro.despesas()),
                () -> assertEquals(valor("-100.00"), janeiro.saldo()),

                () -> assertEquals("2027-02", fevereiro.mes()),
                () -> assertEquals(valor("1000.00"), fevereiro.receitas()),
                () -> assertEquals(BigDecimal.ZERO, fevereiro.despesas()),
                () -> assertEquals(valor("1000.00"), fevereiro.saldo())
        );

        verify(repositorio).somarMovimentacoesProjetadasPorDia(SituacaoLancamento.PENDENTE, inicio, fim);
        verifyNoMoreInteractions(repositorio);
    }

    @Test
    public void dadoNenhumaMovimentacao_AoObterFluxoProjetado_DeveRetornarMesZerado() {
        // Arrange
        var inicio = LocalDate.of(2026, 12, 1);
        var fim = LocalDate.of(2026, 12, 31);

        when(repositorio.somarMovimentacoesProjetadasPorDia(SituacaoLancamento.PENDENTE, inicio, fim))
                .thenReturn(List.of());

        // Act
        var resultado = servico.obterFluxoCaixaProjetado(inicio, fim);

        // Assert
        assertAll(
                () -> assertEquals(BigDecimal.ZERO, resultado.totalReceitas()),
                () -> assertEquals(BigDecimal.ZERO, resultado.totalDespesas()),
                () -> assertEquals(BigDecimal.ZERO, resultado.saldoPeriodo()),
                () -> assertEquals(1, resultado.meses().size()),
                () -> assertEquals("2026-12", resultado.meses().get(0).mes())
        );
    }

    @Test
    public void dadoPeriodoDeUmDia_AoObterFluxoRealizado_DeveRetornarMesCorrespondente() {
        // Arrange
        var data = LocalDate.of(2026, 11, 25);

        when(repositorio.somarMovimentacoesPorDia(SituacaoLancamento.LIQUIDADO, data, data))
                .thenReturn(List.of(
                        new MovimentacaoDiaria(data, TipoLancamento.DESPESA, valor("75.00"))
                ));

        // Act
        var resultado = servico.obterFluxoCaixa(data, data);

        // Assert
        assertAll(
                () -> assertEquals(data, resultado.inicio()),
                () -> assertEquals(data, resultado.fim()),
                () -> assertEquals(valor("75.00"), resultado.totalDespesas()),
                () -> assertEquals(valor("-75.00"), resultado.saldoPeriodo()),
                () -> assertEquals(1, resultado.meses().size()),
                () -> assertEquals("2026-11", resultado.meses().get(0).mes())
        );
    }

    // Testes de validacao do periodo

    @Test
    public void dadoPeriodoInvertido_AoObterFluxoRealizado_DeveLancarExcecao() {
        // Arrange
        var inicio = LocalDate.of(2026, 12, 31);
        var fim = LocalDate.of(2026, 12, 1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> servico.obterFluxoCaixa(inicio, fim));
        verifyNoInteractions(repositorio);
    }

    @Test
    public void dadoPeriodoInvertido_AoObterFluxoProjetado_DeveLancarExcecao() {
        // Arrange
        var inicio = LocalDate.of(2026, 12, 31);
        var fim = LocalDate.of(2026, 12, 1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> servico.obterFluxoCaixaProjetado(inicio, fim));
        verifyNoInteractions(repositorio);
    }

    @Test
    public void dadoDataNula_AoObterFluxoRealizado_DeveLancarExcecao() {
        // Arrange
        var data = LocalDate.of(2026, 11, 1);

        // Act & Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servico.obterFluxoCaixa(null, data)),
                () -> assertThrows(IllegalArgumentException.class, () -> servico.obterFluxoCaixa(data, null))
        );
        verifyNoInteractions(repositorio);
    }

    @Test
    public void dadoDataNula_AoObterFluxoProjetado_DeveLancarExcecao() {
        // Arrange
        var data = LocalDate.of(2026, 12, 1);

        // Act & Assert
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> servico.obterFluxoCaixaProjetado(null, data)),
                () -> assertThrows(IllegalArgumentException.class, () -> servico.obterFluxoCaixaProjetado(data, null))
        );
        verifyNoInteractions(repositorio);
    }
}
