package com.xrayconnect.financeiro.controller;

import com.xrayconnect.financeiro.dto.CategoriaRequest;
import com.xrayconnect.financeiro.dto.CategoriaResponse;
import com.xrayconnect.financeiro.servico.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService servicoCategoria;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse postCategoria(@RequestBody @Valid CategoriaRequest dados) {
        return servicoCategoria.cadastrar(dados);
    }

    @GetMapping
    public Page<CategoriaResponse> getAllCategoria(Pageable pageable) {
        return servicoCategoria.listar(pageable);
    }

    @GetMapping("/{id}")
    public CategoriaResponse getCategoriaById(@PathVariable Long id) {
        return servicoCategoria.buscarPorId(id);
    }

    @PutMapping("{id}")
    public CategoriaResponse putCategoria(@PathVariable Long id, @RequestBody @Valid CategoriaRequest dados) {
        return servicoCategoria.atualizarCategoria(id, dados);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategoria(@PathVariable Long id) {
        servicoCategoria.excluirCategoria(id);
    }
}
