import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CategoriaTest {

    @Test
    public void deveConstruirCategoriaValida() {
        // Arrange
        var tipo = TipoLancamento.RECEITA;
        String nome = "Venda de Equipamento";
        String descricao = "";

        // Act
        var categoria = new Categoria(nome, descricao, tipo);

        // Assert
        assertEquals(nome, categoria.getNome());
        assertEquals(tipo, categoria.getTipo());
        assertNull(categoria.getDescricao());
    }
}
