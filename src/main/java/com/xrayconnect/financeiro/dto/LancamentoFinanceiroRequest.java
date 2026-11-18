package com.xrayconnect.financeiro.dto;

import com.xrayconnect.financeiro.enumeracao.TipoLancamento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoFinanceiroRequest(
        @NotNull Long categoriaId,
        @NotBlank @Size(max = 255) String descricao,
        @Size(max = 120) String nomeContraparte,
        @NotNull @DecimalMin(value = "0.0", inclusive = false, message = "O valor deve ser maior que 0.") @Digits(integer = 13, fraction = 2)
        BigDecimal valor,
        @NotNull TipoLancamento tipoLancamento,
        @NotNull LocalDate dataVencimento
        ) {
}
