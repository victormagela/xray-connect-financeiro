import com.xrayconnect.financeiro.dto.CategoriaRequest;
import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import com.xrayconnect.financeiro.exception.ConflitoNegocioException;
import com.xrayconnect.financeiro.exception.RecursoNaoEncontradoException;
import com.xrayconnect.financeiro.repositorio.CategoriaRepository;
import com.xrayconnect.financeiro.repositorio.LancamentoFinanceiroRepository;
import com.xrayconnect.financeiro.servico.CategoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private CategoriaRequest criarRequest() {
        return new CategoriaRequest(
                "Manutenção",
                "Manutenção de equipamentos",
                TipoLancamento.DESPESA
        );
    }

    private Categoria criarCategoriaExistente(Long id) {
        var categoria = new Categoria(
                "Manutenção",
                "Manutenção de equipamentos",
                TipoLancamento.DESPESA
        );
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }

    @Test
    public void dadoRequestValido_AoCadastrar_DeveSalvarERetornarCategoria() {
        // Arrange
        var dados = criarRequest();
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocacao -> {
            Categoria categoria = invocacao.getArgument(0);
            ReflectionTestUtils.setField(categoria, "id", 1L);
            return categoria;
        });

        // Act
        var resposta = categoriaService.cadastrar(dados);

        // Assert
        assertAll(
                () -> assertEquals(1L, resposta.id()),
                () -> assertEquals(dados.nome(), resposta.nome()),
                () -> assertEquals(dados.descricao(), resposta.descricao()),
                () -> assertEquals(dados.tipo(), resposta.tipo())
        );
        verify(categoriaRepository).existsByNomeAndTipo(dados.nome(), dados.tipo());
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    public void dadoNomeETipoDuplicados_AoCadastrar_DeveLancarConflitoENaoSalvar() {
        // Arrange
        var dados = criarRequest();
        when(categoriaRepository.existsByNomeAndTipo(dados.nome(), dados.tipo()))
                .thenReturn(true);

        // Act & Assert
        assertThrows(ConflitoNegocioException.class, () -> categoriaService.cadastrar(dados));
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    public void dadoIdExistente_AoBuscar_DeveRetornarCategoria() {
        // Arrange
        var categoria = criarCategoriaExistente(5L);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));

        // Act
        var resposta = categoriaService.buscarPorId(5L);

        // Assert
        assertAll(
                () -> assertEquals(5L, resposta.id()),
                () -> assertEquals("Manutenção", resposta.nome()),
                () -> assertEquals(TipoLancamento.DESPESA, resposta.tipo())
        );
    }

    @Test
    public void dadoIdInexistente_AoBuscar_DeveLancarExcecao() {
        // Arrange
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> categoriaService.buscarPorId(99L));
    }

    @Test
    public void dadoPaginaComCategorias_AoListar_DeveMapearOsResultados() {
        // Arrange
        var paginacao = PageRequest.of(0, 10);
        var categoria = criarCategoriaExistente(1L);
        when(categoriaRepository.findAll(paginacao))
                .thenReturn(new PageImpl<>(List.of(categoria), paginacao, 1));

        // Act
        var pagina = categoriaService.listar(paginacao);

        // Assert
        assertAll(
                () -> assertEquals(1, pagina.getTotalElements()),
                () -> assertEquals(1L, pagina.getContent().get(0).id()),
                () -> assertEquals("Manutenção", pagina.getContent().get(0).nome())
        );
        verify(categoriaRepository).findAll(paginacao);
    }

    @Test
    public void dadoCategoriaExistente_AoAtualizar_DeveAlterarSeusDados() {
        // Arrange
        var categoria = criarCategoriaExistente(1L);
        var dados = new CategoriaRequest("Transporte", "Custos logísticos", TipoLancamento.DESPESA);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act
        var resposta = categoriaService.atualizarCategoria(1L, dados);

        // Assert
        assertAll(
                () -> assertEquals("Transporte", categoria.getNome()),
                () -> assertEquals("Custos logísticos", categoria.getDescricao()),
                () -> assertEquals("Transporte", resposta.nome())
        );
        verify(categoriaRepository).existsByNomeAndTipoAndIdNot("Transporte", TipoLancamento.DESPESA, 1L);
        verifyNoInteractions(lancamentoFinanceiroRepository);
        // Não é necessário chamar save(): o JPA fará dirty checking na transação real.
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    public void dadoNomeETipoJaUsadosPorOutraCategoria_AoAtualizar_DeveLancarConflito() {
        // Arrange
        var categoria = criarCategoriaExistente(1L);
        var dados = new CategoriaRequest("Existente", null, TipoLancamento.DESPESA);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByNomeAndTipoAndIdNot("Existente", TipoLancamento.DESPESA, 1L))
                .thenReturn(true);

        // Act & Assert
        assertThrows(ConflitoNegocioException.class,
                () -> categoriaService.atualizarCategoria(1L, dados));
        assertEquals("Manutenção", categoria.getNome());
        assertEquals("Manutenção de equipamentos", categoria.getDescricao());
        verifyNoInteractions(lancamentoFinanceiroRepository);
    }

    @Test
    public void dadaCategoriaComLancamentos_AoMudarTipo_DeveLancarConflito() {
        // Arrange
        var categoria = criarCategoriaExistente(1L);
        var dados = new CategoriaRequest("Manutenção", null, TipoLancamento.RECEITA);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(lancamentoFinanceiroRepository.existsByCategoria_Id(1L)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflitoNegocioException.class,
                () -> categoriaService.atualizarCategoria(1L, dados));
        assertEquals(TipoLancamento.DESPESA, categoria.getTipo());
        verify(lancamentoFinanceiroRepository).existsByCategoria_Id(1L);
    }

    @Test
    public void dadaCategoriaSemLancamentos_AoMudarTipo_DevePermitirAtualizacao() {
        // Arrange
        var categoria = criarCategoriaExistente(1L);
        var dados = new CategoriaRequest("Serviços prestados", null, TipoLancamento.RECEITA);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act
        var resposta = categoriaService.atualizarCategoria(1L, dados);

        // Assert
        assertAll(
                () -> assertEquals(TipoLancamento.RECEITA, categoria.getTipo()),
                () -> assertEquals("Serviços prestados", resposta.nome()),
                () -> assertNull(categoria.getDescricao())
        );
        verify(lancamentoFinanceiroRepository).existsByCategoria_Id(1L);
    }

    @Test
    public void dadaCategoriaComLancamentos_AoAtualizarSemMudarTipo_DevePermitir() {
        // Arrange
        var categoria = criarCategoriaExistente(1L);
        var dados = new CategoriaRequest("Manutenção industrial", null, TipoLancamento.DESPESA);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act
        categoriaService.atualizarCategoria(1L, dados);

        // Assert
        assertEquals("Manutenção industrial", categoria.getNome());
        verifyNoInteractions(lancamentoFinanceiroRepository);
    }

    @Test
    public void dadoIdInexistente_AoAtualizar_DeveLancarExcecao() {
        // Arrange
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> categoriaService.atualizarCategoria(99L, criarRequest()));
        verify(categoriaRepository, never()).save(any(Categoria.class));
    }

    @Test
    public void dadaCategoriaSemLancamentos_AoExcluir_DeveExcluir() {
        // Arrange
        var categoria = criarCategoriaExistente(1L);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act
        categoriaService.excluirCategoria(1L);

        // Assert
        verify(lancamentoFinanceiroRepository).existsByCategoria_Id(1L);
        verify(categoriaRepository).delete(categoria);
    }

    @Test
    public void dadaCategoriaComLancamentos_AoExcluir_DeveLancarConflito() {
        // Arrange
        var categoria = criarCategoriaExistente(1L);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(lancamentoFinanceiroRepository.existsByCategoria_Id(1L)).thenReturn(true);

        // Act & Assert
        assertThrows(ConflitoNegocioException.class,
                () -> categoriaService.excluirCategoria(1L));
        verify(categoriaRepository, never()).delete(any(Categoria.class));
    }

    @Test
    public void dadoIdInexistente_AoExcluir_DeveLancarExcecao() {
        // Arrange
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecursoNaoEncontradoException.class,
                () -> categoriaService.excluirCategoria(99L));
        verify(categoriaRepository, never()).delete(any(Categoria.class));
    }
}
