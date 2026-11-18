package com.xrayconnect.financeiro.dto;

import com.xrayconnect.financeiro.enumeracao.TipoLancamento;

public record CategoriaResponse(
        Long id,
        String nome,
        String descricao,
        TipoLancamento tipo
) {
}
