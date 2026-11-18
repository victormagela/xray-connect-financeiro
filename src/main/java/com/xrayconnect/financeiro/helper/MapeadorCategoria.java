package com.xrayconnect.financeiro.helper;

import com.xrayconnect.financeiro.dto.CategoriaResponse;
import com.xrayconnect.financeiro.entidade.Categoria;

public class MapeadorCategoria {
    public static CategoriaResponse paraDtoResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.getTipo()
        );
    }
}
