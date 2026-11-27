import com.xrayconnect.financeiro.dto.LancamentoFinanceiroRequest;
import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.entidade.LancamentoFinanceiro;
import com.xrayconnect.financeiro.enumeracao.SituacaoLancamento;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import com.xrayconnect.financeiro.exception.ConflitoNegocioException;
import com.xrayconnect.financeiro.exception.RecursoNaoEncontradoException;
import com.xrayconnect.financeiro.repositorio.CategoriaRepository;
import com.xrayconnect.financeiro.repositorio.LancamentoFinanceiroRepository;
import com.xrayconnect.financeiro.servico.LancamentoFinanceiroService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LancamentoFinanceiroServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    @InjectMocks
    private LancamentoFinanceiroService lancamentoService;

    private Categoria criarCategoriaExistente(Long id, TipoLancamento tipo) {
        var categoria = new Categoria("Serviços", null, tipo);
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    private LancamentoFinanceiro criarLancamentoExistente(Long id) {
        var categoria = criarCategoriaExistente(1L, TipoLancamento.RECEITA);
        var lancamento = new LancamentoFinanceiro(
                categoria,
                "Prestação de serviços",
                "Cliente A",
                new BigDecimal("1000.00"),
                TipoLancamento.RECEITA,
                LocalDate.of(2026, 11, 30)
        );
        ReflectionTestUtils.setField(lancamento, "id", id);
        return lancamento;
    }

    private LancamentoFinanceiroRequest criarRequest() {
        return new LancamentoFinanceiroRequest(
                1L,
                "Prestação de serviços",
                "Cliente A",
                new BigDecimal("1000.00"),
                TipoLancamento.RECEITA,
                LocalDate.of(2026, 11, 30)
        );
    }

    @Test
    public void dadoRequestValido_AoCadastrar_DeveSalvarERetornarLancamentoPendente() {
        // Arrange
        var dados = criarRequest();
        var categoria = criarCategoriaExistente(1L, TipoLancamento.RECEITA);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(lancamentoFinanceiroRepository.save(any(LancamentoFinanceiro.class)))
                .thenAnswer(invocacao -> {
                    LancamentoFinanceiro lancamento = invocacao.getArgument(0);
                    ReflectionTestUtils.setField(lancamento, "id", 10L);
                    return lancamento;
                });

        // Act
        var resposta = lancamentoService.cadastrarLancamentoFinanceiro(dados);

        // Assert
        assertAll(
                () -> assertEquals(10L, resposta.id()),
                () -> assertEquals(1L, resposta.categoriaId()),
                () -> assertEquals(dados.valor(), resposta.valor()),
                () -> assertEquals(SituacaoLancamento.PENDENTE, resposta.situacaoLancamento()),
                () -> assertNull(resposta.valorLiquidado())
        );
        verify(lancamentoFinanceiroRepository).save(any(LancamentoFinanceiro.class));
    }

    @Test
    public void dadaCategoriaInexistente_AoCadastrar_DeveLancarExcecaoENaoSalvar() {
        // Arrange
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> lancamentoService.cadastrarLancamentoFinanceiro(criarRequest()));
        verify(lancamentoFinanceiroRepository, never()).save(any(LancamentoFinanceiro.class));
    }

    @Test
    public void dadoTipoIncompativelComCategoria_AoCadastrar_DeveRejeitar() {
        // Arrange
        var categoria = criarCategoriaExistente(1L, TipoLancamento.DESPESA);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> lancamentoService.cadastrarLancamentoFinanceiro(criarRequest()));
        verify(lancamentoFinanceiroRepository, never()).save(any(LancamentoFinanceiro.class));
    }

    @Test
    public void dadoIdExistente_AoBuscar_DeveRetornarLancamento() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));

        // Act
        var resposta = lancamentoService.buscarPorId(10L);

        // Assert
        assertAll(
                () -> assertEquals(10L, resposta.id()),
                () -> assertEquals(1L, resposta.categoriaId()),
                () -> assertEquals("Prestação de serviços", resposta.descricao()),
                () -> assertEquals(SituacaoLancamento.PENDENTE, resposta.situacaoLancamento())
        );
    }

    @Test
    public void dadoIdInexistente_AoBuscar_DeveLancarExcecao() {
        // Arrange
        when(lancamentoFinanceiroRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> lancamentoService.buscarPorId(99L));
    }

    @Test
    public void dadoFiltroPorTipo_AoListar_DeveChamarConsultaFiltrada() {
        // Arrange
        var paginacao = PageRequest.of(0, 10);
        var lancamento = criarLancamentoExistente(10L);
        when(lancamentoFinanceiroRepository.findByTipoLancamento(TipoLancamento.RECEITA, paginacao))
                .thenReturn(new PageImpl<>(List.of(lancamento), paginacao, 1));

        // Act
        var pagina = lancamentoService.listar(paginacao, TipoLancamento.RECEITA);

        // Assert
        assertAll(
                () -> assertEquals(1, pagina.getTotalElements()),
                () -> assertEquals(10L, pagina.getContent().get(0).id()),
                () -> assertEquals(TipoLancamento.RECEITA,
                        pagina.getContent().get(0).tipoLancamento())
        );
        verify(lancamentoFinanceiroRepository).findByTipoLancamento(TipoLancamento.RECEITA, paginacao);
        verify(lancamentoFinanceiroRepository, never()).findAll(paginacao);
    }

    @Test
    public void dadoFiltroNulo_AoListar_DeveConsultarTodosOsLancamentos() {
        // Arrange
        var paginacao = PageRequest.of(0, 10);
        var lancamento = criarLancamentoExistente(10L);
        when(lancamentoFinanceiroRepository.findAll(paginacao))
                .thenReturn(new PageImpl<>(List.of(lancamento), paginacao, 1));

        // Act
        var pagina = lancamentoService.listar(paginacao, null);

        // Assert
        assertEquals(1, pagina.getTotalElements());
        verify(lancamentoFinanceiroRepository).findAll(paginacao);
        verify(lancamentoFinanceiroRepository, never())
                .findByTipoLancamento(any(TipoLancamento.class), any());
    }

    @Test
    public void dadoLancamentoPendente_AoAtualizar_DeveModificarOsDados() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        var categoria = criarCategoriaExistente(1L, TipoLancamento.RECEITA);
        var dados = new LancamentoFinanceiroRequest(
                1L, "Contrato atualizado", "Cliente B",
                new BigDecimal("1200.00"), TipoLancamento.RECEITA,
                LocalDate.of(2026, 12, 15)
        );
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act
        var resposta = lancamentoService.atualizarLancamentoFinanceiro(10L, dados);

        // Assert
        assertAll(
                () -> assertEquals("Contrato atualizado", lancamento.getDescricao()),
                () -> assertEquals("Cliente B", lancamento.getNomeContraparte()),
                () -> assertEquals(new BigDecimal("1200.00"), resposta.valor()),
                () -> assertEquals(LocalDate.of(2026, 12, 15), resposta.dataVencimento()),
                () -> assertEquals(SituacaoLancamento.PENDENTE, resposta.situacaoLancamento())
        );
        // O Service atual não chama save() depois de atualizar uma entidade gerenciada.
        verify(lancamentoFinanceiroRepository, never()).save(any(LancamentoFinanceiro.class));
    }

    @Test
    public void dadoLancamentoInexistente_AoAtualizar_DeveLancarExcecao() {
        // Arrange
        when(lancamentoFinanceiroRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> lancamentoService.atualizarLancamentoFinanceiro(99L, criarRequest()));
        verifyNoInteractions(categoriaRepository);
    }

    @Test
    public void dadaCategoriaInexistente_AoAtualizar_DeveLancarExcecao() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> lancamentoService.atualizarLancamentoFinanceiro(10L, criarRequest()));
        assertEquals("Prestação de serviços", lancamento.getDescricao());
    }

    @Test
    public void dadaCategoriaDeTipoIncompativel_AoAtualizar_DeveRejeitar() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        var categoriaIncompativel = criarCategoriaExistente(1L, TipoLancamento.DESPESA);
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaIncompativel));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> lancamentoService.atualizarLancamentoFinanceiro(10L, criarRequest()));
        assertEquals("Prestação de serviços", lancamento.getDescricao());
        assertEquals(TipoLancamento.RECEITA, lancamento.getCategoria().getTipo());
    }

    @Test
    public void dadoLancamentoLiquidado_AoAtualizar_DeveLancarConflito() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        lancamento.liquidar(LocalDate.now(), new BigDecimal("1050.00"));
        var categoria = criarCategoriaExistente(1L, TipoLancamento.RECEITA);
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act & Assert
        assertThrows(ConflitoNegocioException.class,
                () -> lancamentoService.atualizarLancamentoFinanceiro(10L, criarRequest()));
        assertAll(
                () -> assertEquals(SituacaoLancamento.LIQUIDADO, lancamento.getSituacaoLancamento()),
                () -> assertEquals(new BigDecimal("1050.00"), lancamento.getValorLiquidado())
        );
        verify(lancamentoFinanceiroRepository, never()).save(any(LancamentoFinanceiro.class));
    }

    @Test
    public void dadoLancamentoPendente_AoExcluir_DeveRemoverRegistro() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));

        // Act
        lancamentoService.excluirLancamentoFinanceiro(10L);

        // Assert
        verify(lancamentoFinanceiroRepository).delete(lancamento);
    }

    @Test
    public void dadoLancamentoLiquidado_AoExcluir_DeveLancarConflito() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        lancamento.liquidar(LocalDate.now(), new BigDecimal("980.00"));
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));

        // Act & Assert
        assertThrows(ConflitoNegocioException.class,
                () -> lancamentoService.excluirLancamentoFinanceiro(10L));
        verify(lancamentoFinanceiroRepository, never()).delete(any(LancamentoFinanceiro.class));
    }

    @Test
    public void dadoLancamentoInexistente_AoExcluir_DeveLancarExcecao() {
        // Arrange
        when(lancamentoFinanceiroRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> lancamentoService.excluirLancamentoFinanceiro(99L));
        verify(lancamentoFinanceiroRepository, never()).delete(any(LancamentoFinanceiro.class));
    }

    @Test
    public void dadoLancamentoPendente_AoLiquidar_DeveRegistrarValorEfetivo() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        var dataLiquidacao = LocalDate.now();
        var valorLiquidado = new BigDecimal("1050.00");
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));

        // Act
        var resposta = lancamentoService.liquidarLancamento(10L, dataLiquidacao, valorLiquidado);

        // Assert
        assertAll(
                () -> assertEquals(SituacaoLancamento.LIQUIDADO, resposta.situacaoLancamento()),
                () -> assertEquals(dataLiquidacao, resposta.dataLiquidacao()),
                () -> assertEquals(new BigDecimal("1000.00"), resposta.valor()),
                () -> assertEquals(valorLiquidado, resposta.valorLiquidado())
        );
        verify(lancamentoFinanceiroRepository, never()).save(any(LancamentoFinanceiro.class));
    }

    @Test
    public void dadoLancamentoInexistente_AoLiquidar_DeveLancarExcecao() {
        // Arrange
        when(lancamentoFinanceiroRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> lancamentoService.liquidarLancamento(
                        99L, LocalDate.now(), new BigDecimal("1000.00")));
    }

    @Test
    public void dadoLancamentoJaLiquidado_AoLiquidarNovamente_DeveLancarConflito() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        lancamento.liquidar(LocalDate.now(), new BigDecimal("1050.00"));
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));

        // Act & Assert
        assertThrows(ConflitoNegocioException.class,
                () -> lancamentoService.liquidarLancamento(
                        10L, LocalDate.now(), new BigDecimal("1100.00")));
        assertEquals(new BigDecimal("1050.00"), lancamento.getValorLiquidado());
    }

    @Test
    public void dadoValorLiquidadoInvalido_AoLiquidar_DeveRejeitarSemAlterarLancamento() {
        // Arrange
        var lancamento = criarLancamentoExistente(10L);
        when(lancamentoFinanceiroRepository.findById(10L)).thenReturn(Optional.of(lancamento));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> lancamentoService.liquidarLancamento(
                        10L, LocalDate.now(), BigDecimal.ZERO));
        assertAll(
                () -> assertEquals(SituacaoLancamento.PENDENTE, lancamento.getSituacaoLancamento()),
                () -> assertNull(lancamento.getDataLiquidacao()),
                () -> assertNull(lancamento.getValorLiquidado())
        );
    }
}
