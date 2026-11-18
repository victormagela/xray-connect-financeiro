package com.xrayconnect.financeiro.servico;

import com.xrayconnect.financeiro.dto.CategoriaRequest;
import com.xrayconnect.financeiro.dto.CategoriaResponse;
import com.xrayconnect.financeiro.entidade.Categoria;
import com.xrayconnect.financeiro.helper.MapeadorCategoria;
import com.xrayconnect.financeiro.repositorio.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repositorio;

    @Transactional
    public CategoriaResponse cadastrar(CategoriaRequest dados) {
        if (repositorio.existsByNomeAndTipo(dados.nome(), dados.tipo())) {
            throw new IllegalArgumentException("Já existe uma categoria com esse nome e tipo.");
        }

        var categoria = repositorio.save(new Categoria(
                dados.nome(),
                dados.descricao(),
                dados.tipo()
        ));

        return MapeadorCategoria.paraDtoResponse(categoria);
    }

    public CategoriaResponse buscarPorId(Long id) {
        var categoria = repositorio.findById(id).orElseThrow();

        return MapeadorCategoria.paraDtoResponse(categoria);
    }

    public List<CategoriaResponse> listar() {
        return repositorio.findAll()
                .stream()
                .map(MapeadorCategoria::paraDtoResponse)
                .toList();
    }
}
