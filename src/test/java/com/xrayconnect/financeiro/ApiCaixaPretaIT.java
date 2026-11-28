package com.xrayconnect.financeiro;

import com.jayway.jsonpath.JsonPath;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FinanceiroApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class ApiCaixaPretaIT {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4")
            .withDatabaseName("financeiro_test");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dadoCadastroValido_AoCriarCategoria_DeveRetornar201() throws Exception {
        // Arrange
        String json = """
                {"nome":"Servicos", "descricao":"Receitas de servicos", "tipo":"RECEITA"}
                """;

        // Act & Assert
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Servicos"))
                .andExpect(jsonPath("$.tipo").value("RECEITA"));
    }

    @Test
    void dadoNomeEmBranco_AoCriarCategoria_DeveRetornar400() throws Exception {
        // Arrange
        String json = """
                {"nome":"   ", "tipo":"DESPESA"}
                """;

        // Act & Assert
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.nome").isArray());
    }

    @Test
    void dadoTipoAusente_AoCriarCategoria_DeveRetornar400() throws Exception {
        // Arrange
        String json = """
                {"nome":"Sem tipo"}
                """;

        // Act & Assert
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.tipo").isArray());
    }

    @Test
    void dadaCategoriaDuplicada_AoCadastrar_DeveRetornar409() throws Exception {
        // Arrange
        cadastrarCategoria("Duplicada", TipoLancamento.DESPESA);
        String json = """
                {"nome":"Duplicada", "tipo":"DESPESA"}
                """;

        // Act & Assert
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void dadaCategoriaExistente_AoConsultarPorId_DeveRetornar200() throws Exception {
        // Arrange
        long id = cadastrarCategoria("Consulta", TipoLancamento.RECEITA);

        // Act & Assert
        mockMvc.perform(get("/api/categorias/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Math.toIntExact(id)))
                .andExpect(jsonPath("$.nome").value("Consulta"));
    }

    @Test
    void dadoIdInexistente_AoConsultarCategoria_DeveRetornar404() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/categorias/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void dadaCategoriaExistente_AoAtualizar_DeveRetornar200() throws Exception {
        // Arrange
        long id = cadastrarCategoria("Nome original", TipoLancamento.DESPESA);
        String json = """
                {"nome":"Nome atualizado", "descricao":"Atualizada", "tipo":"DESPESA"}
                """;

        // Act & Assert
        mockMvc.perform(put("/api/categorias/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome atualizado"))
                .andExpect(jsonPath("$.descricao").value("Atualizada"));
    }

    @Test
    void dadaCategoriaSemLancamentos_AoExcluir_DeveRetornar204() throws Exception {
        // Arrange
        long id = cadastrarCategoria("Excluivel", TipoLancamento.DESPESA);

        // Act & Assert
        mockMvc.perform(delete("/api/categorias/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categorias/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void dadaCategoriaComLancamentos_AoExcluir_DeveRetornar409() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Com lancamento", TipoLancamento.DESPESA);
        cadastrarLancamento(categoriaId, "Conta de agua", TipoLancamento.DESPESA,
                new BigDecimal("120.00"), LocalDate.now().plusDays(5));

        // Act & Assert
        mockMvc.perform(delete("/api/categorias/{id}", categoriaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void dadaCategoriaComLancamentos_AoMudarTipo_DeveRetornar409() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Tipo protegido", TipoLancamento.DESPESA);
        cadastrarLancamento(categoriaId, "Conta de luz", TipoLancamento.DESPESA,
                new BigDecimal("200.00"), LocalDate.now().plusDays(5));
        String json = """
                {"nome":"Tipo protegido", "tipo":"RECEITA"}
                """;

        // Act & Assert
        mockMvc.perform(put("/api/categorias/{id}", categoriaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void dadoCadastroValido_AoCriarLancamento_DeveRetornar201() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Vendas", TipoLancamento.RECEITA);
        String json = jsonLancamento(categoriaId, "Venda de equipamento",
                TipoLancamento.RECEITA, new BigDecimal("1500.00"), LocalDate.now().plusDays(5));

        // Act & Assert
        mockMvc.perform(post("/api/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.valor").value(1500.0))
                .andExpect(jsonPath("$.situacaoLancamento").value("PENDENTE"))
                .andExpect(jsonPath("$.valorLiquidado").value(nullValue()));
    }

    @Test
    void dadoValorZero_AoCriarLancamento_DeveRetornar400() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Custos", TipoLancamento.DESPESA);
        String json = jsonLancamento(categoriaId, "Valor invalido",
                TipoLancamento.DESPESA, BigDecimal.ZERO, LocalDate.now().plusDays(5));

        // Act & Assert
        mockMvc.perform(post("/api/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valor").isArray());
    }

    @Test
    void dadaCategoriaInexistente_AoCriarLancamento_DeveRetornar404() throws Exception {
        // Arrange
        String json = jsonLancamento(999999L, "Categoria inexistente",
                TipoLancamento.RECEITA, new BigDecimal("200.00"), LocalDate.now().plusDays(5));

        // Act & Assert
        mockMvc.perform(post("/api/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void dadoTipoIncompativel_AoCriarLancamento_DeveRetornar400() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Despesa somente", TipoLancamento.DESPESA);
        String json = jsonLancamento(categoriaId, "Tipo incompativel",
                TipoLancamento.RECEITA, new BigDecimal("200.00"), LocalDate.now().plusDays(5));

        // Act & Assert
        mockMvc.perform(post("/api/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dadoFiltroPorTipo_AoListarLancamentos_DeveRetornarSomenteReceitas() throws Exception {
        // Arrange
        long idReceita = cadastrarCategoria("Receita filtro", TipoLancamento.RECEITA);
        long idDespesa = cadastrarCategoria("Despesa filtro", TipoLancamento.DESPESA);
        cadastrarLancamento(idReceita, "Receita A", TipoLancamento.RECEITA,
                new BigDecimal("300.00"), LocalDate.now().plusDays(5));
        cadastrarLancamento(idDespesa, "Despesa B", TipoLancamento.DESPESA,
                new BigDecimal("200.00"), LocalDate.now().plusDays(5));

        // Act & Assert
        mockMvc.perform(get("/api/lancamentos").param("tipo", "RECEITA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].tipoLancamento").value("RECEITA"));
    }

    @Test
    void dadoLancamentoPendente_AoLiquidarComJuros_DeveRetornar200() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Pagamento", TipoLancamento.DESPESA);
        long id = cadastrarLancamento(categoriaId, "Conta com juros",
                TipoLancamento.DESPESA, new BigDecimal("1000.00"), LocalDate.now().minusDays(2));
        String json = jsonLiquidacao(LocalDate.now(), new BigDecimal("1050.00"));

        // Act & Assert
        mockMvc.perform(patch("/api/lancamentos/{id}/liquidar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(1000.0))
                .andExpect(jsonPath("$.valorLiquidado").value(1050.0))
                .andExpect(jsonPath("$.situacaoLancamento").value("LIQUIDADO"))
                .andExpect(jsonPath("$.dataLiquidacao").value(LocalDate.now().toString()));
    }

    @Test
    void dadoValorLiquidadoAusente_AoLiquidar_DeveRetornar400() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Liquidacao invalida", TipoLancamento.RECEITA);
        long id = cadastrarLancamento(categoriaId, "Sem valor efetivo",
                TipoLancamento.RECEITA, new BigDecimal("1000.00"), LocalDate.now());
        String json = """
                {"dataLiquidacao":"%s"}
                """.formatted(LocalDate.now());

        // Act & Assert
        mockMvc.perform(patch("/api/lancamentos/{id}/liquidar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valorLiquidado").isArray());
    }

    @Test
    void dadaDataFutura_AoLiquidar_DeveRetornar400() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Data invalida", TipoLancamento.RECEITA);
        long id = cadastrarLancamento(categoriaId, "Pagamento futuro",
                TipoLancamento.RECEITA, new BigDecimal("100.00"), LocalDate.now());

        // Act & Assert
        mockMvc.perform(patch("/api/lancamentos/{id}/liquidar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLiquidacao(LocalDate.now().plusDays(1), new BigDecimal("100.00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.dataLiquidacao").isArray());
    }

    @Test
    void dadoLancamentoJaLiquidado_AoLiquidarNovamente_DeveRetornar409() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Liquidado uma vez", TipoLancamento.RECEITA);
        long id = cadastrarLancamento(categoriaId, "Recebimento unico",
                TipoLancamento.RECEITA, new BigDecimal("100.00"), LocalDate.now());
        liquidarLancamento(id, LocalDate.now(), new BigDecimal("98.00"));

        // Act & Assert
        mockMvc.perform(patch("/api/lancamentos/{id}/liquidar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLiquidacao(LocalDate.now(), new BigDecimal("100.00"))))
                .andExpect(status().isConflict());
    }

    @Test
    void dadoLancamentoJaLiquidado_AoAtualizar_DeveRetornar409() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Categoria protegida", TipoLancamento.DESPESA);
        long id = cadastrarLancamento(categoriaId, "Despesa original",
                TipoLancamento.DESPESA, new BigDecimal("500.00"), LocalDate.now());
        liquidarLancamento(id, LocalDate.now(), new BigDecimal("520.00"));
        String novoLancamento = jsonLancamento(categoriaId, "Despesa alterada",
                TipoLancamento.DESPESA, new BigDecimal("700.00"), LocalDate.now());

        // Act & Assert
        mockMvc.perform(put("/api/lancamentos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novoLancamento))
                .andExpect(status().isConflict());
    }

    @Test
    void dadoLancamentoJaLiquidado_AoExcluir_DeveRetornar409() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Despesa a manter", TipoLancamento.DESPESA);
        long id = cadastrarLancamento(categoriaId, "Despesa liquidada",
                TipoLancamento.DESPESA, new BigDecimal("500.00"), LocalDate.now());
        liquidarLancamento(id, LocalDate.now(), new BigDecimal("500.00"));

        // Act & Assert
        mockMvc.perform(delete("/api/lancamentos/{id}", id))
                .andExpect(status().isConflict());
    }

    @Test
    void dadoLancamentoPendente_AoExcluir_DeveRetornar204() throws Exception {
        // Arrange
        long categoriaId = cadastrarCategoria("Exclusao pendente", TipoLancamento.DESPESA);
        long id = cadastrarLancamento(categoriaId, "Conta cancelada",
                TipoLancamento.DESPESA, new BigDecimal("70.00"), LocalDate.now().plusDays(5));

        // Act & Assert
        mockMvc.perform(delete("/api/lancamentos/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lancamentos/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ==================== RELATORIOS ====================

    @Test
    void dadosLancamentosDiversos_AoConsultarDashboard_DeveRetornarTotaisEfetivos() throws Exception {
        // Arrange
        long receita = cadastrarCategoria("Receitas dashboard", TipoLancamento.RECEITA);
        long despesa = cadastrarCategoria("Despesas dashboard", TipoLancamento.DESPESA);
        long recebimento = cadastrarLancamento(receita, "Receita liquidada",
                TipoLancamento.RECEITA, new BigDecimal("1000.00"), LocalDate.now());
        long pagamento = cadastrarLancamento(despesa, "Despesa liquidada",
                TipoLancamento.DESPESA, new BigDecimal("600.00"), LocalDate.now());
        cadastrarLancamento(receita, "Receita pendente",
                TipoLancamento.RECEITA, new BigDecimal("300.00"), LocalDate.now().plusDays(5));
        cadastrarLancamento(despesa, "Despesa pendente",
                TipoLancamento.DESPESA, new BigDecimal("150.00"), LocalDate.now().plusDays(5));
        liquidarLancamento(recebimento, LocalDate.now(), new BigDecimal("950.00"));
        liquidarLancamento(pagamento, LocalDate.now(), new BigDecimal("650.00"));

        // Act & Assert
        mockMvc.perform(get("/api/relatorios/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAReceber").value(300.0))
                .andExpect(jsonPath("$.totalAPagar").value(150.0))
                .andExpect(jsonPath("$.totalRecebido").value(950.0))
                .andExpect(jsonPath("$.totalPago").value(650.0))
                .andExpect(jsonPath("$.saldoRealizado").value(300.0));
    }

    @Test
    void dadosLancamentosLiquidados_AoConsultarFluxoRealizado_DeveUsarValoresEfetivos() throws Exception {
        // Arrange
        LocalDate dia = LocalDate.now();
        long categoriaReceita = cadastrarCategoria("Realizado receita", TipoLancamento.RECEITA);
        long categoriaDespesa = cadastrarCategoria("Realizado despesa", TipoLancamento.DESPESA);
        long idReceita = cadastrarLancamento(categoriaReceita, "Receita",
                TipoLancamento.RECEITA, new BigDecimal("1000.00"), dia);
        long idDespesa = cadastrarLancamento(categoriaDespesa, "Despesa",
                TipoLancamento.DESPESA, new BigDecimal("500.00"), dia);
        cadastrarLancamento(categoriaReceita, "Pendente nao entra",
                TipoLancamento.RECEITA, new BigDecimal("200.00"), dia);
        liquidarLancamento(idReceita, dia, new BigDecimal("950.00"));
        liquidarLancamento(idDespesa, dia, new BigDecimal("540.00"));

        // Act & Assert
        mockMvc.perform(get("/api/relatorios/fluxo-caixa")
                        .param("inicio", dia.withDayOfMonth(1).toString())
                        .param("fim", dia.withDayOfMonth(dia.lengthOfMonth()).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas").value(950.0))
                .andExpect(jsonPath("$.totalDespesas").value(540.0))
                .andExpect(jsonPath("$.saldoPeriodo").value(410.0))
                .andExpect(jsonPath("$.meses[0].mes").value(YearMonth.from(dia).toString()));
    }

    @Test
    void dadosLancamentosPendentes_AoConsultarFluxoProjetado_DeveUsarValoresPrevistos() throws Exception {
        // Arrange
        LocalDate vencimento = LocalDate.now().plusMonths(1).withDayOfMonth(15);
        long categoriaReceita = cadastrarCategoria("Projetado receita", TipoLancamento.RECEITA);
        long categoriaDespesa = cadastrarCategoria("Projetado despesa", TipoLancamento.DESPESA);
        cadastrarLancamento(categoriaReceita, "Receita pendente",
                TipoLancamento.RECEITA, new BigDecimal("800.00"), vencimento);
        cadastrarLancamento(categoriaDespesa, "Despesa pendente",
                TipoLancamento.DESPESA, new BigDecimal("400.00"), vencimento);
        long idLiquidado = cadastrarLancamento(categoriaDespesa, "Nao entra no projetado",
                TipoLancamento.DESPESA, new BigDecimal("1000.00"), vencimento);
        liquidarLancamento(idLiquidado, LocalDate.now(), new BigDecimal("1000.00"));

        // Act & Assert
        mockMvc.perform(get("/api/relatorios/fluxo-caixa/projetado")
                        .param("inicio", vencimento.withDayOfMonth(1).toString())
                        .param("fim", vencimento.withDayOfMonth(vencimento.lengthOfMonth()).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas").value(800.0))
                .andExpect(jsonPath("$.totalDespesas").value(400.0))
                .andExpect(jsonPath("$.saldoPeriodo").value(400.0))
                .andExpect(jsonPath("$.meses[0].mes").value(YearMonth.from(vencimento).toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/relatorios/fluxo-caixa",
            "/api/relatorios/fluxo-caixa/projetado"
    })
    void dadoPeriodoInvertido_AoConsultarFluxo_DeveRetornar400(String endpoint) throws Exception {
        // Act & Assert
        mockMvc.perform(get(endpoint)
                        .param("inicio", "2026-12-31")
                        .param("fim", "2026-12-01"))
                .andExpect(status().isBadRequest());
    }

    private long cadastrarCategoria(String nome, TipoLancamento tipo) throws Exception {
        String json = """
                {"nome":"%s", "descricao":"Categoria de teste", "tipo":"%s"}
                """.formatted(nome, tipo.name());

        var resultado = mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private long cadastrarLancamento(long categoriaId, String descricao,
                                     TipoLancamento tipo, BigDecimal valor,
                                     LocalDate vencimento) throws Exception {
        var resultado = mockMvc.perform(post("/api/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLancamento(categoriaId, descricao, tipo, valor, vencimento)))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private void liquidarLancamento(long id, LocalDate data, BigDecimal valor) throws Exception {
        mockMvc.perform(patch("/api/lancamentos/{id}/liquidar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLiquidacao(data, valor)))
                .andExpect(status().isOk());
    }

    private String jsonLancamento(long categoriaId, String descricao,
                                  TipoLancamento tipo, BigDecimal valor,
                                  LocalDate vencimento) {
        return """
                {
                  "categoriaId": %d,
                  "descricao": "%s",
                  "nomeContraparte": "Contraparte teste",
                  "valor": %s,
                  "tipoLancamento": "%s",
                  "dataVencimento": "%s"
                }
                """.formatted(categoriaId, descricao, valor.toPlainString(), tipo.name(), vencimento);
    }

    private String jsonLiquidacao(LocalDate data, BigDecimal valor) {
        return """
                {"dataLiquidacao":"%s", "valorLiquidado":%s}
                """.formatted(data, valor.toPlainString());
    }
}
