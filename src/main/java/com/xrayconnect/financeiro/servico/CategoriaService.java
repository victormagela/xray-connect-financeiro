package com.xrayconnect.financeiro.servico;

import com.xrayconnect.financeiro.dto.CategoriaRequest;
import com.xrayconnect.financeiro.dto.CategoriaResponse;
import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.helper.MapeadorCategoria;
import com.xrayconnect.financeiro.repositorio.CategoriaRepository;
import com.xrayconnect.financeiro.repositorio.LancamentoFinanceiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

    @Transactional
    public CategoriaResponse cadastrar(CategoriaRequest dados) {
        if (categoriaRepository.existsByNomeAndTipo(dados.nome(), dados.tipo())) {
            throw new IllegalArgumentException("Já existe uma categoria com esse nome e tipo.");
        }

        var categoria = categoriaRepository.save(new Categoria(
                dados.nome(),
                dados.descricao(),
                dados.tipo()
        ));

        return MapeadorCategoria.paraDtoResponse(categoria);
    }

    public CategoriaResponse buscarPorId(Long id) {
        var categoria = categoriaRepository.findById(id).orElseThrow();

        return MapeadorCategoria.paraDtoResponse(categoria);
    }

    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll()
                .stream()
                .map(MapeadorCategoria::paraDtoResponse)
                .toList();
    }

    @Transactional
    public CategoriaResponse atualizarCategoria(Long id, CategoriaRequest dados) {
        var categoria = categoriaRepository.findById(id).orElseThrow();

        if (categoriaRepository.existsByNomeAndTipoAndIdNot(dados.nome(), dados.tipo(), id)) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome e tipo.");
        }

        if (categoria.getTipo() != dados.tipo() && lancamentoFinanceiroRepository.existsByCategoria_Id(id)) {
            throw new IllegalArgumentException("Não é possível mudar o tipo de uma categoria que já possui lançamentos.");
        }

        categoria.atualizar(dados.nome(), dados.descricao(), dados.tipo());

        return MapeadorCategoria.paraDtoResponse(categoria);
    }

    @Transactional
    public void excluirCategoria(Long id) {
        var categoria = categoriaRepository.findById(id).orElseThrow();

        if (lancamentoFinanceiroRepository.existsByCategoria_Id(id)) {
            throw new IllegalArgumentException("Não é possível excluir uma categoria que já possui lançamentos");
        }

        categoriaRepository.delete(categoria);
    }
}
