import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.entidade.LancamentoFinanceiro;
import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import com.xrayconnect.financeiro.exception.ConflitoNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class LancamentoFinanceiroTest {

    private static final LocalDate VENCIMENTO = LocalDate.of(2026, 11, 25);
    private static final BigDecimal VALOR_ORIGINAL = new BigDecimal("1000.00");

    private LancamentoFinanceiro criarLancamentoValido() {
        var categoria = new Categoria("Energia eletrica", null, TipoLancamento.DESPESA);

        return new LancamentoFinanceiro(
                categoria,
                "Pagamento de energia",
                null,
                VALOR_ORIGINAL,
                TipoLancamento.DESPESA,
                VENCIMENTO
        );
    }

    // Testes de construcao

    @Test
    public void deveCriarLancamentoValido() {
        // Act
        var lancamento = criarLancamentoValido();

        // Assert
        assertAll(
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValor()),
                () -> assertEquals(TipoLancamento.DESPESA, lancamento.getTipoLancamento()),
                () -> assertEquals(SituacaoLancamento.PENDENTE, lancamento.getSituacaoLancamento()),
                () -> assertNull(lancamento.getDataLiquidacao()),
                () -> assertNull(lancamento.getValorLiquidado()),
                () -> assertNull(lancamento.getNomeContraparte())
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    public void dadoValorMenorQueZeroOuIgualZero_AoConstruirNovoLancamento_DeveLancarExcecao(int valor) {
        var categoria = new Categoria("Energia eletrica", null, TipoLancamento.DESPESA);

        assertThrows(IllegalArgumentException.class, () ->
                new LancamentoFinanceiro(
                        categoria,
                        "Pagamento de energia",
                        null,
                        BigDecimal.valueOf(valor),
                        TipoLancamento.DESPESA,
                        VENCIMENTO
                )
        );
    }

    @Test
    public void dadoTipoLancamentoDiferenteTipoCategoria_AoConstruirNovoLancamento_DeveLancarExcecao() {
        var categoria = new Categoria("Energia eletrica", null, TipoLancamento.DESPESA);

        assertThrows(IllegalArgumentException.class, () ->
                new LancamentoFinanceiro(
                        categoria,
                        "Pagamento de energia",
                        null,
                        VALOR_ORIGINAL,
                        TipoLancamento.RECEITA,
                        VENCIMENTO
                )
        );
    }

    // Testes de liquidacao

    @Test
    public void dadoDataLiquidacaoValida_AoLiquidar_DeveMudarEstado() {
        // Arrange
        var lancamento = criarLancamentoValido();
        var dataLiquidacao = LocalDate.now();

        // Act
        lancamento.liquidar(dataLiquidacao, VALOR_ORIGINAL);

        // Assert
        assertAll(
                () -> assertEquals(SituacaoLancamento.LIQUIDADO, lancamento.getSituacaoLancamento()),
                () -> assertEquals(dataLiquidacao, lancamento.getDataLiquidacao()),
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValorLiquidado())
        );
    }

    @Test
    public void dadoLancamentoJaLiquidado_AoLiquidar_DeveLancarExcecao() {
        // Arrange
        var lancamento = criarLancamentoValido();
        var dataLiquidacao = LocalDate.now();
        lancamento.liquidar(dataLiquidacao, VALOR_ORIGINAL);

        // Act & Assert
        assertThrows(ConflitoNegocioException.class, () ->
                lancamento.liquidar(dataLiquidacao, new BigDecimal("2000.00"))
        );

        assertAll(
                () -> assertEquals(dataLiquidacao, lancamento.getDataLiquidacao()),
                () -> assertEquals(SituacaoLancamento.LIQUIDADO, lancamento.getSituacaoLancamento()),
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValorLiquidado())
        );
    }

    @Test
    public void dadoDataFutura_AoLiquidar_DeveLancarExcecao() {
        // Arrange
        var lancamento = criarLancamentoValido();
        var dataFutura = LocalDate.now().plusDays(1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                lancamento.liquidar(dataFutura, VALOR_ORIGINAL)
        );

        assertLancamentoPendenteSemLiquidacao(lancamento);
    }

    @Test
    public void dadoDataNula_AoLiquidar_DeveLancarExcecao() {
        var lancamento = criarLancamentoValido();

        assertThrows(IllegalArgumentException.class, () ->
                lancamento.liquidar(null, VALOR_ORIGINAL)
        );
        assertLancamentoPendenteSemLiquidacao(lancamento);
    }

    @Test
    public void dadoValorComJuros_AoLiquidar_DevePreservarValorOriginal() {
        // Arrange
        var lancamento = criarLancamentoValido();
        var valorComJuros = new BigDecimal("1050.00");

        // Act
        lancamento.liquidar(LocalDate.now(), valorComJuros);

        // Assert
        assertAll(
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValor()),
                () -> assertEquals(valorComJuros, lancamento.getValorLiquidado()),
                () -> assertEquals(SituacaoLancamento.LIQUIDADO, lancamento.getSituacaoLancamento())
        );
    }

    @Test
    public void dadoValorComDesconto_AoLiquidar_DevePreservarValorOriginal() {
        // Arrange
        var lancamento = criarLancamentoValido();
        var valorComDesconto = new BigDecimal("950.00");

        // Act
        lancamento.liquidar(LocalDate.now(), valorComDesconto);

        // Assert
        assertAll(
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValor()),
                () -> assertEquals(valorComDesconto, lancamento.getValorLiquidado()),
                () -> assertEquals(SituacaoLancamento.LIQUIDADO, lancamento.getSituacaoLancamento())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    public void dadoValorLiquidadoZeroOuNegativo_AoLiquidar_DeveLancarExcecao(String valor) {
        // Arrange
        var lancamento = criarLancamentoValido();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                lancamento.liquidar(LocalDate.now(), new BigDecimal(valor))
        );
        assertLancamentoPendenteSemLiquidacao(lancamento);
    }

    @Test
    public void dadoValorLiquidadoNulo_AoLiquidar_DeveLancarExcecao() {
        var lancamento = criarLancamentoValido();

        assertThrows(IllegalArgumentException.class, () ->
                lancamento.liquidar(LocalDate.now(), null)
        );
        assertLancamentoPendenteSemLiquidacao(lancamento);
    }

    @Test
    public void dadoValorLiquidadoComMaisDeDuasCasasDecimais_AoLiquidar_DeveLancarExcecao() {
        var lancamento = criarLancamentoValido();

        assertThrows(IllegalArgumentException.class, () ->
                lancamento.liquidar(LocalDate.now(), new BigDecimal("1000.001"))
        );
        assertLancamentoPendenteSemLiquidacao(lancamento);
    }

    @Test
    public void dadoValorLiquidadoMaiorQueDecimaisPermitidos_AoLiquidar_DeveLancarExcecao() {
        var lancamento = criarLancamentoValido();

        // DECIMAL(15,2) comporta no maximo 13 digitos inteiros.
        assertThrows(IllegalArgumentException.class, () ->
                lancamento.liquidar(LocalDate.now(), new BigDecimal("10000000000000.00"))
        );
        assertLancamentoPendenteSemLiquidacao(lancamento);
    }

    // Testes de vencimento

    @Test
    public void dadoDataReferenciaPosterior_AoVerificarVencimento_DeveRetornarTrue() {
        var lancamento = criarLancamentoValido();

        assertTrue(lancamento.estaVencida(VENCIMENTO.plusDays(5)));
    }

    @Test
    public void dadoLancamentoLiquidado_AoVerificarVencimento_DeveRetornarFalse() {
        var lancamento = criarLancamentoValido();
        lancamento.liquidar(LocalDate.now(), VALOR_ORIGINAL);

        assertFalse(lancamento.estaVencida(VENCIMENTO.plusDays(5)));
    }

    @Test
    public void dadoDataReferenciaAnterior_AoVerificarVencimento_DeveRetornarFalse() {
        var lancamento = criarLancamentoValido();

        assertFalse(lancamento.estaVencida(VENCIMENTO.minusDays(1)));
    }

    @Test
    public void dadoDataReferenciaIgual_AoVerificarVencimento_DeveRetornarFalse() {
        var lancamento = criarLancamentoValido();

        assertFalse(lancamento.estaVencida(VENCIMENTO));
    }

    // Testes de atualizacao

    @Test
    public void dadoLancamentoPendente_AoAtualizar_DeveAtualizarCampos() {
        // Arrange
        var lancamento = criarLancamentoValido();
        var novaCategoria = new Categoria("Manutencao", null, TipoLancamento.DESPESA);
        var novaData = VENCIMENTO.plusMonths(1);
        var novoValor = new BigDecimal("1250.00");

        // Act
        lancamento.atualizar(
                novaCategoria,
                "Manutencao de equipamento",
                "Fornecedor A",
                novoValor,
                TipoLancamento.DESPESA,
                novaData
        );

        // Assert
        assertAll(
                () -> assertSame(novaCategoria, lancamento.getCategoria()),
                () -> assertEquals("Manutencao de equipamento", lancamento.getDescricao()),
                () -> assertEquals("Fornecedor A", lancamento.getNomeContraparte()),
                () -> assertEquals(novoValor, lancamento.getValor()),
                () -> assertEquals(TipoLancamento.DESPESA, lancamento.getTipoLancamento()),
                () -> assertEquals(novaData, lancamento.getDataVencimento()),
                () -> assertEquals(SituacaoLancamento.PENDENTE, lancamento.getSituacaoLancamento()),
                () -> assertNull(lancamento.getValorLiquidado())
        );
    }

    @Test
    public void dadoLancamentoJaLiquidado_AoAtualizar_DeveLancarConflitoESemAlterarCampos() {
        // Arrange
        var lancamento = criarLancamentoValido();
        var dataLiquidacao = LocalDate.now();
        lancamento.liquidar(dataLiquidacao, VALOR_ORIGINAL);
        var novaCategoria = new Categoria("Manutencao", null, TipoLancamento.DESPESA);

        // Act & Assert
        assertThrows(ConflitoNegocioException.class, () ->
                lancamento.atualizar(
                        novaCategoria,
                        "Descricao indevida",
                        "Fornecedor A",
                        new BigDecimal("2000.00"),
                        TipoLancamento.DESPESA,
                        VENCIMENTO.plusDays(10)
                )
        );

        assertAll(
                () -> assertEquals("Pagamento de energia", lancamento.getDescricao()),
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValor()),
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValorLiquidado()),
                () -> assertEquals(dataLiquidacao, lancamento.getDataLiquidacao()),
                () -> assertEquals(SituacaoLancamento.LIQUIDADO, lancamento.getSituacaoLancamento())
        );
    }

    @Test
    public void dadoCategoriaDeTipoIncompativel_AoAtualizar_DeveLancarExcecao() {
        // Arrange
        var lancamento = criarLancamentoValido();
        var categoriaReceita = new Categoria("Servicos", null, TipoLancamento.RECEITA);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                lancamento.atualizar(
                        categoriaReceita,
                        "Descricao indevida",
                        null,
                        new BigDecimal("2000.00"),
                        TipoLancamento.DESPESA,
                        VENCIMENTO
                )
        );

        assertAll(
                () -> assertEquals("Pagamento de energia", lancamento.getDescricao()),
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValor()),
                () -> assertEquals(SituacaoLancamento.PENDENTE, lancamento.getSituacaoLancamento())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    public void dadoValorZeroOuNegativo_AoAtualizar_DeveLancarExcecao(String valor) {
        // Arrange
        var lancamento = criarLancamentoValido();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                lancamento.atualizar(
                        lancamento.getCategoria(),
                        "Descricao indevida",
                        null,
                        new BigDecimal(valor),
                        TipoLancamento.DESPESA,
                        VENCIMENTO
                )
        );

        assertAll(
                () -> assertEquals("Pagamento de energia", lancamento.getDescricao()),
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValor()),
                () -> assertEquals(SituacaoLancamento.PENDENTE, lancamento.getSituacaoLancamento())
        );
    }

    private void assertLancamentoPendenteSemLiquidacao(LancamentoFinanceiro lancamento) {
        assertAll(
                () -> assertEquals(SituacaoLancamento.PENDENTE, lancamento.getSituacaoLancamento()),
                () -> assertNull(lancamento.getDataLiquidacao()),
                () -> assertNull(lancamento.getValorLiquidado()),
                () -> assertEquals(VALOR_ORIGINAL, lancamento.getValor())
        );
    }
}
