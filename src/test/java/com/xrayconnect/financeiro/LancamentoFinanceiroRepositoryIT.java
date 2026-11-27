package com.xrayconnect.financeiro;

import com.xrayconnect.financeiro.dto.MovimentacaoDiaria;
import com.xrayconnect.financeiro.dto.TotalPorTipoESituacao;
import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.entidade.LancamentoFinanceiro;
import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import com.xrayconnect.financeiro.repositorio.CategoriaRepository;
import com.xrayconnect.financeiro.repositorio.LancamentoFinanceiroRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional // Cada teste executa em sua própria transação, revertida ao terminar.
class LancamentoFinanceiroRepositoryIT {

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
    void dadoLancamentoValido_AoSalvar_DevePersistirRelacionamentoComCategoria() {
        // Arrange
        var categoria = salvarCategoria("Prestadores", TipoLancamento.DESPESA);
        var vencimento = LocalDate.of(2026, 12, 10);
        var lancamento = criarLancamento(categoria, "350.00", vencimento);

        // Act
        var salvo = lancamentoRepository.save(lancamento);
        entityManager.flush();
        entityManager.clear(); // Obriga a próxima leitura a consultar o banco.
        var encontrado = lancamentoRepository.findById(salvo.getId()).orElseThrow();

        // Assert
        assertAll(
                () -> assertNotNull(encontrado.getId()),
                () -> assertEquals(categoria.getId(), encontrado.getCategoria().getId()),
                () -> assertEquals("Prestadores", encontrado.getCategoria().getNome()),
                () -> assertEquals(SituacaoLancamento.PENDENTE, encontrado.getSituacaoLancamento()),
                () -> assertNull(encontrado.getDataLiquidacao()),
                () -> assertNull(encontrado.getValorLiquidado()),
                () -> assertEquals(vencimento, encontrado.getDataVencimento()),
                () -> assertValor("350.00", encontrado.getValor())
        );
    }

    @Test
    void dadoLancamentoPendente_AoLiquidar_DevePersistirValorEfetivoEManterValorOriginal() {
        // Arrange
        var categoria = salvarCategoria("Servicos", TipoLancamento.DESPESA);
        var lancamento = salvarLancamento(categoria, "1000.00", LocalDate.of(2026, 11, 5));
        var dataLiquidacao = LocalDate.of(2026, 11, 10);

        // Act
        lancamento.liquidar(dataLiquidacao, new BigDecimal("1050.00"));
        entityManager.flush();
        var id = lancamento.getId();
        entityManager.clear();
        var encontrado = lancamentoRepository.findById(id).orElseThrow();

        // Assert
        assertAll(
                () -> assertEquals(SituacaoLancamento.LIQUIDADO, encontrado.getSituacaoLancamento()),
                () -> assertEquals(dataLiquidacao, encontrado.getDataLiquidacao()),
                () -> assertValor("1000.00", encontrado.getValor()),
                () -> assertValor("1050.00", encontrado.getValorLiquidado())
        );
    }

    @Test
    void dadosLancamentosPendentesELiquidados_AoAgruparTotais_DeveUsarValorCorretoPorSituacao() {
        // Arrange
        var receita = salvarCategoria("Vendas", TipoLancamento.RECEITA);
        var despesa = salvarCategoria("Compras", TipoLancamento.DESPESA);
        var dia = LocalDate.of(2026, 11, 10);

        salvarLancamento(receita, "300.00", dia);
        salvarLancamento(receita, "200.00", dia);
        salvarLancamento(despesa, "80.00", dia);

        liquidar(salvarLancamento(receita, "1000.00", dia), "950.00");
        liquidar(salvarLancamento(receita, "200.00", dia), "210.00");
        liquidar(salvarLancamento(despesa, "500.00", dia), "540.00");
        entityManager.flush();

        // Act
        var totais = lancamentoRepository.agruparTotaisPorTipoESituacao();

        // Assert
        assertAll(
                () -> assertEquals(4, totais.size()),
                () -> assertValor("500.00", total(totais, TipoLancamento.RECEITA, SituacaoLancamento.PENDENTE)),
                () -> assertValor("80.00", total(totais, TipoLancamento.DESPESA, SituacaoLancamento.PENDENTE)),
                () -> assertValor("1160.00", total(totais, TipoLancamento.RECEITA, SituacaoLancamento.LIQUIDADO)),
                () -> assertValor("540.00", total(totais, TipoLancamento.DESPESA, SituacaoLancamento.LIQUIDADO))
        );
    }

    @Test
    void dadosLancamentosLiquidados_AoConsultarFluxoRealizado_DeveAgruparPorDataETipo() {
        // Arrange
        var receita = salvarCategoria("Receitas", TipoLancamento.RECEITA);
        var despesa = salvarCategoria("Despesas", TipoLancamento.DESPESA);
        var dia = LocalDate.of(2026, 11, 10);

        liquidar(salvarLancamento(receita, "1000.00", dia), "950.00");
        liquidar(salvarLancamento(receita, "100.00", dia), "110.00");
        liquidar(salvarLancamento(despesa, "200.00", dia), "220.00");
        salvarLancamento(receita, "900.00", dia); // Pendente: não entra no realizado.
        entityManager.flush();

        // Act
        var movimentos = lancamentoRepository.somarMovimentacoesPorDia(
                SituacaoLancamento.LIQUIDADO, dia, dia
        );

        // Assert
        assertAll(
                () -> assertEquals(2, movimentos.size()),
                () -> assertValor("1060.00", total(movimentos, dia, TipoLancamento.RECEITA)),
                () -> assertValor("220.00", total(movimentos, dia, TipoLancamento.DESPESA))
        );
    }

    @Test
    void dadosLancamentosPendentes_AoConsultarFluxoProjetado_DeveAgruparPorVencimentoETipo() {
        // Arrange
        var receita = salvarCategoria("Mensalidades", TipoLancamento.RECEITA);
        var despesa = salvarCategoria("Custos", TipoLancamento.DESPESA);
        var dia = LocalDate.of(2026, 12, 10);

        salvarLancamento(receita, "200.00", dia);
        salvarLancamento(receita, "400.00", dia);
        salvarLancamento(despesa, "150.00", dia);
        liquidar(salvarLancamento(receita, "3000.00", dia), "2800.00"); // Não entra na projeção.
        salvarLancamento(receita, "500.00", dia.plusDays(1)); // Fora do período.
        entityManager.flush();

        // Act
        var movimentos = lancamentoRepository.somarMovimentacoesProjetadasPorDia(
                SituacaoLancamento.PENDENTE, dia, dia
        );

        // Assert
        assertAll(
                () -> assertEquals(2, movimentos.size()),
                () -> assertValor("600.00", total(movimentos, dia, TipoLancamento.RECEITA)),
                () -> assertValor("150.00", total(movimentos, dia, TipoLancamento.DESPESA))
        );
    }

    @Test
    void dadosVencimentosDiferentes_AoContarVencidos_DeveContarSomentePendentesAnterioresAReferencia() {
        // Arrange
        var categoria = salvarCategoria("Contratos", TipoLancamento.RECEITA);
        var referencia = LocalDate.of(2026, 11, 20);

        salvarLancamento(categoria, "100.00", referencia.minusDays(1));
        salvarLancamento(categoria, "100.00", referencia); // Vence hoje: não está atrasado.
        salvarLancamento(categoria, "100.00", referencia.plusDays(1));
        liquidar(salvarLancamento(categoria, "100.00", referencia.minusDays(2)), "100.00");
        entityManager.flush();

        // Act
        long quantidade = lancamentoRepository.contarVencidos(SituacaoLancamento.PENDENTE, referencia);

        // Assert
        assertEquals(1L, quantidade);
    }

    @Test
    void dadoFiltroReceita_AoListar_DeveRetornarSomenteReceitas() {
        // Arrange
        var receita = salvarCategoria("Vendas", TipoLancamento.RECEITA);
        var despesa = salvarCategoria("Taxas", TipoLancamento.DESPESA);
        salvarLancamento(receita, "800.00", LocalDate.of(2026, 12, 1));
        salvarLancamento(despesa, "400.00", LocalDate.of(2026, 12, 1));
        entityManager.flush();

        // Act
        var pagina = lancamentoRepository.findByTipoLancamento(
                TipoLancamento.RECEITA, PageRequest.of(0, 10)
        );

        // Assert
        assertAll(
                () -> assertEquals(1L, pagina.getTotalElements()),
                () -> assertEquals(1, pagina.getContent().size()),
                () -> assertEquals(TipoLancamento.RECEITA, pagina.getContent().getFirst().getTipoLancamento())
        );
    }

    @Test
    void dadoLancamentoComCategoria_AoVerificarDependencia_DeveRetornarTrue() {
        // Arrange
        var utilizada = salvarCategoria("Utilizada", TipoLancamento.DESPESA);
        var livre = salvarCategoria("Livre", TipoLancamento.DESPESA);
        salvarLancamento(utilizada, "250.00", LocalDate.of(2026, 12, 1));
        entityManager.flush();

        // Act
        boolean possuiLancamentos = lancamentoRepository.existsByCategoria_Id(utilizada.getId());
        boolean categoriaLivre = lancamentoRepository.existsByCategoria_Id(livre.getId());

        // Assert
        assertAll(
                () -> assertTrue(possuiLancamentos),
                () -> assertFalse(categoriaLivre)
        );
    }

    @Test
    void dadoPeriodoSemMovimentacoes_AoConsultarFluxos_DeveRetornarListasVazias() {
        // Arrange
        var inicio = LocalDate.of(2030, 1, 1);
        var fim = LocalDate.of(2030, 1, 31);

        // Act
        var realizado = lancamentoRepository.somarMovimentacoesPorDia(
                SituacaoLancamento.LIQUIDADO, inicio, fim
        );
        var projetado = lancamentoRepository.somarMovimentacoesProjetadasPorDia(
                SituacaoLancamento.PENDENTE, inicio, fim
        );

        // Assert
        assertAll(
                () -> assertTrue(realizado.isEmpty()),
                () -> assertTrue(projetado.isEmpty())
        );
    }

    // Métodos auxiliares: evitam repetir a criação de dados em cada teste.

    private Categoria salvarCategoria(String nome, TipoLancamento tipo) {
        return categoriaRepository.save(new Categoria(nome, null, tipo));
    }

    private LancamentoFinanceiro criarLancamento(
            Categoria categoria, String valor, LocalDate vencimento
    ) {
        return new LancamentoFinanceiro(
                categoria, "Lançamento de integração", null,
                new BigDecimal(valor), categoria.getTipo(), vencimento
        );
    }

    private LancamentoFinanceiro salvarLancamento(
            Categoria categoria, String valor, LocalDate vencimento
    ) {
        return lancamentoRepository.save(criarLancamento(categoria, valor, vencimento));
    }

    private void liquidar(LancamentoFinanceiro lancamento, String valorEfetivo) {
        lancamento.liquidar(LocalDate.of(2026, 11, 10), new BigDecimal(valorEfetivo));
    }

    private static BigDecimal total(
            List<TotalPorTipoESituacao> totais,
            TipoLancamento tipo,
            SituacaoLancamento situacao
    ) {
        return totais.stream()
                .filter(t -> t.tipo() == tipo && t.situacao() == situacao)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Grupo financeiro não retornado: " + tipo + "/" + situacao))
                .total();
    }

    private static BigDecimal total(
            List<MovimentacaoDiaria> movimentos,
            LocalDate data,
            TipoLancamento tipo
    ) {
        return movimentos.stream()
                .filter(m -> m.data().equals(data) && m.tipo() == tipo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Movimentação não retornada: " + data + "/" + tipo))
                .total();
    }

    private static void assertValor(String esperado, BigDecimal atual) {
        assertNotNull(atual);
        assertEquals(0, new BigDecimal(esperado).compareTo(atual));
    }
}
