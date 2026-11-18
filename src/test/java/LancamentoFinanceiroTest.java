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
}
