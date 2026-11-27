import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class CategoriaTest {

    // Testes de construcao

    @Test
    public void deveConstruirCategoriaValida() {
        // Arrange
        var tipo = TipoLancamento.RECEITA;
        String nome = "Venda de Equipamento";
        String descricao = "";

        // Act
        var categoria = new Categoria(nome, descricao, tipo);

        // Assert
        assertAll(
                () -> assertEquals(nome, categoria.getNome()),
                () -> assertEquals(tipo, categoria.getTipo()),
                () -> assertNull(categoria.getDescricao())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    public void dadoNomeNuloVazioOuEmBranco_AoCriarCategoria_DeveLancarExcecao(String nome) {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new Categoria(nome, "Descricao", TipoLancamento.RECEITA)
        );
    }

    @Test
    public void dadoTipoNulo_AoCriarCategoria_DeveLancarExcecao() {
        assertThrows(IllegalArgumentException.class, () ->
                new Categoria("Vendas", "Descricao", null)
        );
    }

    // Testes de atualizacao

    @Test
    public void dadoDadosValidos_AoAtualizarCategoria_DeveAtualizarAtributos() {
        // Arrange
        var categoria = new Categoria("Vendas", null, TipoLancamento.RECEITA);

        // Act
        categoria.atualizar("Materiais", "Compra de materiais", TipoLancamento.DESPESA);

        // Assert
        assertAll(
                () -> assertEquals("Materiais", categoria.getNome()),
                () -> assertEquals("Compra de materiais", categoria.getDescricao()),
                () -> assertEquals(TipoLancamento.DESPESA, categoria.getTipo())
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    public void dadoNomeInvalido_AoAtualizarCategoria_DeveLancarExcecaoESemAlterarEstado(String nome) {
        // Arrange
        var categoria = new Categoria("Vendas", "Receitas de vendas", TipoLancamento.RECEITA);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                categoria.atualizar(nome, "Nova descricao", TipoLancamento.DESPESA)
        );

        assertAll(
                () -> assertEquals("Vendas", categoria.getNome()),
                () -> assertEquals("Receitas de vendas", categoria.getDescricao()),
                () -> assertEquals(TipoLancamento.RECEITA, categoria.getTipo())
        );
    }

    @Test
    public void dadoTipoNulo_AoAtualizarCategoria_DeveLancarExcecaoESemAlterarEstado() {
        // Arrange
        var categoria = new Categoria("Vendas", "Receitas de vendas", TipoLancamento.RECEITA);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                categoria.atualizar("Outro nome", "Nova descricao", null)
        );

        assertAll(
                () -> assertEquals("Vendas", categoria.getNome()),
                () -> assertEquals("Receitas de vendas", categoria.getDescricao()),
                () -> assertEquals(TipoLancamento.RECEITA, categoria.getTipo())
        );
    }

    @Test
    public void dadoDescricaoEmBranco_AoAtualizarCategoria_DeveArmazenarNull() {
        // Arrange
        var categoria = new Categoria("Vendas", "Descricao anterior", TipoLancamento.RECEITA);

        // Act
        categoria.atualizar("Vendas", "   ", TipoLancamento.RECEITA);

        // Assert
        assertNull(categoria.getDescricao());
    }
}
