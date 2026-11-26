import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.entidade.LancamentoFinanceiro;
import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class LancamentoFinanceiroTest {

    @Test
    public void deveCriarLancamentoValido() {

        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(50000);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        //Act
        var lancamento = new LancamentoFinanceiro(
                categoria,
                descricaoLancamento,
                nomeContraparte,
                valor,
                tipo,
                dataVencimento
                );

        //Assert
        assertEquals(valor, lancamento.getValor());
        assertEquals(tipo, lancamento.getTipoLancamento());
        assertEquals(SituacaoLancamento.PENDENTE, lancamento.getSituacaoLancamento());
        assertNull(lancamento.getDataLiquidacao());
        assertNull(lancamento.getNomeContraparte());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    public void dadoValorMenorQueZeroOuIgualZero_AoContruirNovoLancamento_DeveLancarExcecao(int candidate) {

        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(candidate);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        //Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
            new LancamentoFinanceiro(
                    categoria,
                    descricaoLancamento,
                    nomeContraparte,
                    valor,
                    tipo,
                    dataVencimento
            );
        });
    }

    @Test
    public void dadoTipoLancamentoDiferenteTipoCategoria_AoConstruirNovoLancamento_DeveLancaExcecao() {
        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(50000);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        //Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new LancamentoFinanceiro(
                    categoria,
                    descricaoLancamento,
                    nomeContraparte,
                    valor,
                    TipoLancamento.RECEITA,
                    dataVencimento
            );
        });
    }

    @Test
    public void dadoDataLiquidacaoValida_AoLiquidar_DeveMudarEstado() {
        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(50000);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        var lancamento = new LancamentoFinanceiro(
                categoria,
                descricaoLancamento,
                nomeContraparte,
                valor,
                tipo,
                dataVencimento
        );

        var dataLiquidacao = LocalDate.now();

        //Act
        lancamento.liquidar(dataLiquidacao, new BigDecimal("1000.00"));

        //Assert
        assertEquals(SituacaoLancamento.LIQUIDADO, lancamento.getSituacaoLancamento());
        assertEquals(dataLiquidacao, lancamento.getDataLiquidacao());
    }

    @Test
    public void dadoLancamentoJaLiquidado_AoLiquidar_DeveLancarExcecao() {
        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(50000);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        var lancamento = new LancamentoFinanceiro(
                categoria,
                descricaoLancamento,
                nomeContraparte,
                valor,
                tipo,
                dataVencimento
        );

        var dataLiquidacao = LocalDate.now();

        lancamento.liquidar(dataLiquidacao, new BigDecimal("1000.00"));

        //Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            lancamento.liquidar(dataLiquidacao, new BigDecimal("1000.00"));
        });
        assertEquals(dataLiquidacao, lancamento.getDataLiquidacao());
        assertEquals(SituacaoLancamento.LIQUIDADO, lancamento.getSituacaoLancamento());
    }

    @Test
    public void dadoDataFutura_AoLiquidar_DeveLancarExcecao() {
        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(50000);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        var lancamento = new LancamentoFinanceiro(
                categoria,
                descricaoLancamento,
                nomeContraparte,
                valor,
                tipo,
                dataVencimento
        );

        var dataLiquidacao = LocalDate.now().plusDays(1);

        //Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
           lancamento.liquidar(dataLiquidacao, new BigDecimal("1000.00"));
        });
        assertNull(lancamento.getDataLiquidacao());
        assertEquals(SituacaoLancamento.PENDENTE, lancamento.getSituacaoLancamento());
    }

    @Test
    public void dadoDataReferenciaPosterior_AoVerificarVencimento_DeveRetornarTrue() {
        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(50000);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        var lancamento = new LancamentoFinanceiro(
                categoria,
                descricaoLancamento,
                nomeContraparte,
                valor,
                tipo,
                dataVencimento
        );

        var dataReferencia = LocalDate.of(2026, 11, 30);

        //Act
        boolean estaVencida = lancamento.estaVencida(dataReferencia);

        //Assert
        assertTrue(estaVencida);
    }

    @Test
    public void dadoLancamentoLiquidado_AoVerificarVencimento_DeveRetornarFalse() {
        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(50000);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        var lancamento = new LancamentoFinanceiro(
                categoria,
                descricaoLancamento,
                nomeContraparte,
                valor,
                tipo,
                dataVencimento
        );
        lancamento.liquidar(LocalDate.now(), new BigDecimal("1000.00"));

        var dataReferencia = LocalDate.of(2026, 11, 30);

        //Act
        boolean estaVencida = lancamento.estaVencida(dataReferencia);

        //Assert
        assertFalse(estaVencida);
    }

    @Test
    public void dadoDataReferenciaAnterior_AoVerificarVencimento_DeveRetornarFalse() {
        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(50000);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        var lancamento = new LancamentoFinanceiro(
                categoria,
                descricaoLancamento,
                nomeContraparte,
                valor,
                tipo,
                dataVencimento
        );

        var dataReferencia = LocalDate.of(2026, 11, 24);

        //Act
        boolean estaVencida = lancamento.estaVencida(dataReferencia);

        //Assert
        assertFalse(estaVencida);
    }

    @Test
    public void dadoDataReferenciaIgual_AoVerificarVencimento_DeveRetornarFalse() {
        //Arrange
        TipoLancamento tipo = TipoLancamento.DESPESA;
        String descricao = "";
        String nome = "Salários";
        var categoria = new Categoria(nome, descricao, tipo);

        String descricaoLancamento = "Pagamento da folha salarial do mês X";
        String nomeContraparte = "";
        var valor = new BigDecimal(50000);
        var dataVencimento = LocalDate.of(2026, 11, 25);

        var lancamento = new LancamentoFinanceiro(
                categoria,
                descricaoLancamento,
                nomeContraparte,
                valor,
                tipo,
                dataVencimento
        );

        var dataReferencia = dataVencimento;

        //Act
        boolean estaVencida = lancamento.estaVencida(dataReferencia);

        //Assert
        assertFalse(estaVencida);
    }
}
