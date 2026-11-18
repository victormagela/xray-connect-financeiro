package com.xrayconnect.financeiro.dto;

import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 255) String descricao,
        @NotNull TipoLancamento tipo
) {
}
