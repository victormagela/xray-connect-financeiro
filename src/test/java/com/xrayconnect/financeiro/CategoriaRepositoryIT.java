package com.xrayconnect.financeiro;

import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.entidade.LancamentoFinanceiro;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import com.xrayconnect.financeiro.repositorio.CategoriaRepository;
import com.xrayconnect.financeiro.repositorio.LancamentoFinanceiroRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional // Cada teste roda em uma transação revertida ao terminar.
class CategoriaRepositoryIT {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4")
            .withDatabaseName("financeiro_test");

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private LancamentoFinanceiroRepository lancamentoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void dadaCategoriaValida_AoSalvar_DevePersistirEReceberId() {
        // Arrange
        var categoria = new Categoria(
                "Manutencao preventiva",
                "Custos de manutencao dos equipamentos",
                TipoLancamento.DESPESA
        );

        // Act
        var salva = categoriaRepository.saveAndFlush(categoria);
        Long id = salva.getId();
        entityManager.clear(); // Evita obter a mesma instância do cache de primeiro nível.
        var encontrada = categoriaRepository.findById(id).orElseThrow();

        // Assert
        assertAll(
                () -> assertNotNull(id),
                () -> assertEquals("Manutencao preventiva", encontrada.getNome()),
                () -> assertEquals("Custos de manutencao dos equipamentos", encontrada.getDescricao()),
                () -> assertEquals(TipoLancamento.DESPESA, encontrada.getTipo())
        );
    }

    @Test
    void dadaCategoriaExistente_AoBuscarPorId_DeveRetornarRegistroCorreto() {
        // Arrange
        var primeira = salvarCategoria("Vendas de aparelhos", TipoLancamento.RECEITA);
        var segunda = salvarCategoria("Venda de pecas", TipoLancamento.RECEITA);
        entityManager.clear();

        // Act
        var encontrada = categoriaRepository.findById(segunda.getId());
        var inexistente = categoriaRepository.findById(-1L);

        // Assert
        assertAll(
                () -> assertTrue(encontrada.isPresent()),
                () -> assertEquals(segunda.getId(), encontrada.orElseThrow().getId()),
                () -> assertEquals("Venda de pecas", encontrada.orElseThrow().getNome()),
                () -> assertNotEquals(primeira.getId(), encontrada.orElseThrow().getId()),
                () -> assertTrue(inexistente.isEmpty())
        );
    }

    @Test
    void dadasCategoriasComMesmoNomeETipo_AoSalvarSegunda_DeveRejeitarDuplicidade() {
        // Arrange
        salvarCategoria("Frete", TipoLancamento.DESPESA);
        var duplicada = new Categoria("Frete", null, TipoLancamento.DESPESA);

        // Act & Assert
        // A inserção força o MySQL a validar a UNIQUE (tipo, nome).
        // Não executar novas consultas após a exceção: a transação será revertida.
        assertThrows(DataIntegrityViolationException.class, () ->
                categoriaRepository.saveAndFlush(duplicada)
        );
    }

    @Test
    void dadasCategoriasComMesmoNomeETiposDiferentes_AoSalvar_DevePermitirAmbas() {
        // Arrange
        var receita = new Categoria("Manutencao", null, TipoLancamento.RECEITA);
        var despesa = new Categoria("Manutencao", null, TipoLancamento.DESPESA);

        // Act
        var receitaSalva = categoriaRepository.saveAndFlush(receita);
        var despesaSalva = categoriaRepository.saveAndFlush(despesa);
        entityManager.clear();

        // Assert
        assertAll(
                () -> assertNotEquals(receitaSalva.getId(), despesaSalva.getId()),
                () -> assertEquals(TipoLancamento.RECEITA,
                        categoriaRepository.findById(receitaSalva.getId()).orElseThrow().getTipo()),
                () -> assertEquals(TipoLancamento.DESPESA,
                        categoriaRepository.findById(despesaSalva.getId()).orElseThrow().getTipo()),
                () -> assertTrue(categoriaRepository.existsByNomeAndTipo(
                        "Manutencao", TipoLancamento.RECEITA)),
                () -> assertTrue(categoriaRepository.existsByNomeAndTipo(
                        "Manutencao", TipoLancamento.DESPESA))
        );
    }

    @Test
    void dadaCategoriaExistente_AoAtualizar_DevePersistirNovosDados() {
        // Arrange
        var categoria = salvarCategoria("Suprimentos", TipoLancamento.DESPESA);
        Long id = categoria.getId();

        // Act
        categoria.atualizar(
                "Materiais de escritorio",
                "Papel, cartuchos e outros materiais",
                TipoLancamento.DESPESA
        );
        categoriaRepository.flush();
        entityManager.clear();
        var atualizada = categoriaRepository.findById(id).orElseThrow();

        // Assert
        assertAll(
                () -> assertEquals(id, atualizada.getId()),
                () -> assertEquals("Materiais de escritorio", atualizada.getNome()),
                () -> assertEquals("Papel, cartuchos e outros materiais", atualizada.getDescricao()),
                () -> assertEquals(TipoLancamento.DESPESA, atualizada.getTipo()),
                () -> assertFalse(categoriaRepository.existsByNomeAndTipo(
                        "Suprimentos", TipoLancamento.DESPESA))
        );
    }

    @Test
    void dadasDuasCategorias_AoVerificarNomeETipoExcluindoId_DeveIdentificarOutraCategoria() {
        // Arrange
        var utilizada = salvarCategoria("Logistica", TipoLancamento.DESPESA);
        var outra = salvarCategoria("Transporte", TipoLancamento.DESPESA);

        // Act
        boolean duplicadaEmOutraCategoria = categoriaRepository.existsByNomeAndTipoAndIdNot(
                "Logistica", TipoLancamento.DESPESA, outra.getId()
        );
        boolean mesmaCategoriaNaoContaComoDuplicada = categoriaRepository.existsByNomeAndTipoAndIdNot(
                "Logistica", TipoLancamento.DESPESA, utilizada.getId()
        );
        boolean tipoDiferenteNaoContaComoDuplicada = categoriaRepository.existsByNomeAndTipoAndIdNot(
                "Logistica", TipoLancamento.RECEITA, outra.getId()
        );

        // Assert
        assertAll(
                () -> assertTrue(duplicadaEmOutraCategoria),
                () -> assertFalse(mesmaCategoriaNaoContaComoDuplicada),
                () -> assertFalse(tipoDiferenteNaoContaComoDuplicada)
        );
    }

    @Test
    void dadaCategoriaSemLancamentos_AoExcluir_DeveRemoverDoBanco() {
        // Arrange
        var categoria = salvarCategoria("Despesas eventuais", TipoLancamento.DESPESA);
        Long id = categoria.getId();

        // Act
        categoriaRepository.delete(categoria);
        categoriaRepository.flush();
        entityManager.clear();

        // Assert
        assertTrue(categoriaRepository.findById(id).isEmpty());
    }

    private Categoria salvarCategoria(String nome, TipoLancamento tipo) {
        return categoriaRepository.saveAndFlush(new Categoria(nome, null, tipo));
    }
}
