package com.xrayconnect.financeiro.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LiquidacaoRequest(
        @NotNull
        @PastOrPresent(message = "Data deve ser passada ou presente.")
        LocalDate dataLiquidacao,
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "O valor deve ser maior que 0")
        @Digits(integer = 13, fraction = 2)
        BigDecimal valorLiquidado
) {
}
